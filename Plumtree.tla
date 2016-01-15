------------------------------ MODULE Plumtree ------------------------------
(****************************************************************************************************************
A specification for the Plumtree protocol, a self-healing broadcast tree embedded in a gossip-based overlay.
From the paper (http://homepages.gsd.inesc-id.pt/~jleitao/pdf/srds07-leitao.pdf): 
"The broadcast tree in created and maintained using a low cost algorithm, described in the paper. 
Broadcast is achieved mainly by using push gossip on the tree branches. However, the remaining links of the 
gossip-based overlay are also used to propagate the message using a lazy-push approach.
The lazy-push steps ensure high reliability (by guaranteeing that, in face of failures, the protocol falls back 
to a pure gossip approach) and, additionally, also provide a way to quickly heal the broadcast tree".
****************************************************************************************************************)
EXTENDS Naturals, FiniteSets, Sequences, TLC, Network

\* Message types for the Plumtree protocol
CONSTANTS Gossip, IHave, Graft, Prune

\* Message Body - the gossip being broadcasted
CONSTANT MBody

\* Unique ids for the gossip messages
MID == 1..MAX_MSG 
\* Set of possible messages. This serves as the Network's Msg parameter
ProtoMsg == [msrc: Peer, mtype: {IHave, Graft}, mid: MID]
            \cup
            [msrc: Peer, mtype: {Gossip}, mid: MID, mbody: MBody]
            \cup
            [msrc: Peer, mtype: {Prune}]

\* An abstraction of the peer sampling service, provides the initial set of eager peers 
\* for the protocol.          
CONSTANT PeerSamplingService          

\* An abstraction of gossip generation, which creates some gossip message with origin <peer> and 
\* assignes it to message[peer].
\* For spec testing this is set to the TestCreateGossip(peer) operator 
CONSTANT CreateGossipMsg(_)
                      
\* An abstraction of gossip processing at the application level.
\* For spec testing this is set to TRUE 
CONSTANT ProcessGossip(_,_)
ASSUME \A p \in Peer, m \in ProtoMsg: ProcessGossip(p, m) \in BOOLEAN 
                      
\* The set of peers for eager message pushing
\* This is a mapping between a peer and a set of other peers
VARIABLE eagerPushPeers 

\* The set of peers for lazy message pushing
\* This is a mapping between a peer and a set of other peers
VARIABLE lazyPushPeers 

\* A log of the recieved gossip at each peer
\* This is a mapping between a peer and another mapping, of message id to body
\* E.g {r1 -> { id1 -> m1, id2 -> m2, ...}, r2 -> {...}, ...}
VARIABLE receivedUpdates

\* A log of the announced gossip that hasn't been received yet
\* This is a mapping between a peer and another mapping, of message id to  
\* a FIFO queue of peers that have the message
\* E.g {r1 -> { id1 -> {r3, r5}, id2 -> {r2}, ...}, r2 -> {...}, ...}
VARIABLE missingUpdateSrcs

\* Expiration timers for missing-message requests
\* This is a mapping between a peer and a FIFO queue of message ids representing 
\* timers waiting for those messages
\* E.g {r1 -> { id1, id2, ...}, r2 -> {...}, ...}
VARIABLE timers

\* Auxiliary variables - for managing the control flow.
\* The program counter, the currently handled messsage, and the current recipients from each peer 
VARIABLE pc, message, recipient

\* No value, for reprisenting 'no available peer'
Nil == CHOOSE x \in Nat: x \notin Peer

\* All variables (for stuttering)
allvars == <<eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, 
             commChannels, msgCounter, pc, recipient, message>>
 
\* For spec testing - generate a gossip message
TestCreateGossip(peer) == \E b \in MBody:
                        message' = [message EXCEPT ![peer] = 
                            [mtype |-> Gossip, 
                             mid   |-> msgCounter, 
                             mbody |-> b,
                             msrc  |-> peer]]

\*************************** Utility Operators **************************
-------------------------------------------------------------------------

\* Send a message to one of multiple receipients - move to the next state when there are no more receipients
SendMsg(peer, nextState) == 
    IF recipient[peer] # {}
    THEN /\ LET p == CHOOSE i \in recipient[peer]: TRUE IN 
            /\ Send(message[peer], peer, p)
            /\ recipient' = [recipient EXCEPT ![peer] = (@ \ {p})]
         /\ UNCHANGED <<msgCounter, eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, message, pc>>
    ELSE /\ pc' = [pc EXCEPT ![peer] = nextState]
         /\ UNCHANGED <<commChannels, msgCounter, message, recipient, eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers>>

\* Remove a timer of a specific message
StopTimer(self, id) == 
    LET Keep(m) == IF m # id THEN TRUE ELSE FALSE IN
        timers' = [timers EXCEPT ![self] = SelectSeq(timers[self], Keep)]

\* Remove the mapping between a message-id and its IHave senders from the missingUpdateSrcs log
RemoveMissingMsg(self, m) == 
    LET myMissing == missingUpdateSrcs[self] IN
        missingUpdateSrcs' = [missingUpdateSrcs EXCEPT ![self] = [id \in (DOMAIN myMissing \ {m.mid}) |-> myMissing[id]]]

\* Receive gossip for the first time - broadcast it and make sure the sender is in our eager peers. 
ReceiveGossip(peer) == 
    /\ LET m == message[peer] IN  
       /\ receivedUpdates' = [receivedUpdates EXCEPT ![peer] = (receivedUpdates[peer] @@ m.mid :> m.mbody)]
       /\ StopTimer(peer, m.mid)
       /\ RemoveMissingMsg(peer, m)
       /\ eagerPushPeers' = [eagerPushPeers EXCEPT ![peer] = @ \cup ({m.msrc} \ {peer})]
       /\ lazyPushPeers'  = [lazyPushPeers EXCEPT ![peer] = @ \ {m.msrc}]
    /\ pc' = [pc EXCEPT ![peer] = "HandleGossip"] 
    /\ UNCHANGED <<commChannels, msgCounter, message, recipient>>

\* Receive an already-known gossip - move the sender to our lazy peers and tell it to do the same.
RejectGossip(peer) == 
    /\ LET m == message[peer] IN  
       /\ eagerPushPeers' = [eagerPushPeers EXCEPT ![peer] = @ \ {m.msrc}]
       /\ lazyPushPeers'  = [lazyPushPeers EXCEPT ![peer] = @ \cup ({m.msrc} \ {peer})]
       /\ Send([msrc  |-> peer, 
                mtype |-> Prune], peer, m.msrc)
    /\ pc' = [pc EXCEPT ![peer] = "Ready"] 
    /\ UNCHANGED <<msgCounter, message, recipient, receivedUpdates, missingUpdateSrcs, timers>>

\* Upon receiving IHave message, if this is an unknown message add it and the sender to the missingUpdateSrcs map.
\* If this is the first IHave message for a missing message, start a timer for requesting it.
ReceiveIHave(peer) == 
    /\ LET m == message[peer] IN
       IF m.mid \notin DOMAIN missingUpdateSrcs[peer]
       THEN /\ missingUpdateSrcs' = [missingUpdateSrcs EXCEPT ![peer] = (missingUpdateSrcs[peer] @@ m.mid :> <<m.msrc>>)]
            /\ timers' = [timers EXCEPT ![peer] = Append(timers[peer], m.mid)]
            /\ UNCHANGED <<commChannels, msgCounter, eagerPushPeers, lazyPushPeers, receivedUpdates, recipient, message>>
       ELSE /\ missingUpdateSrcs' = [missingUpdateSrcs EXCEPT ![peer][m.mid] = Append(missingUpdateSrcs[peer][m.mid], m.msrc)]
            /\ UNCHANGED <<commChannels, msgCounter, eagerPushPeers, lazyPushPeers, receivedUpdates, timers, recipient, message>>
    /\ pc' = [pc EXCEPT ![peer] = "Ready"] 

\* A graft request results in adding the sender to the eager-push peers and sending the missing message
ReceiveGraft(peer) ==
    /\ LET m == message[peer] IN 
       /\ Send([mtype |-> Gossip,
                mid   |-> m.mid,
                msrc  |-> peer, 
                mbody |-> receivedUpdates[peer][m.mid]], 
                peer, m.msrc)
       /\ eagerPushPeers' = [eagerPushPeers EXCEPT ![peer] = @ \cup {m.msrc}]
       /\ lazyPushPeers'  = [lazyPushPeers EXCEPT ![peer] = @ \ {m.msrc}]
    /\ pc' = [pc EXCEPT ![peer] = "Ready"] 
    /\ UNCHANGED <<msgCounter, receivedUpdates, missingUpdateSrcs, timers, recipient, message>>

\* A prune request results in moving the sender from the eager-push to the lazy-push peers
ReceivePrune(peer) == 
    /\ LET m == message[peer] IN 
       /\ eagerPushPeers' = [eagerPushPeers EXCEPT ![peer] = @ \ {m.msrc}]
       /\ lazyPushPeers'  = [lazyPushPeers EXCEPT ![peer] = @ \cup {m.msrc}]
    /\ pc' = [pc EXCEPT ![peer] = "Ready"] 
    /\ UNCHANGED <<commChannels, msgCounter, receivedUpdates, missingUpdateSrcs, timers, recipient, message>>


\***************************** System States ****************************
-------------------------------------------------------------------------

\* Process the gossip and send the update to the eager peers.
HandleGossip(peer) ==
    /\ pc[peer] = "HandleGossip"
    /\ ProcessGossip(peer, message[peer])
    /\ recipient' = [recipient EXCEPT ![peer] = (eagerPushPeers[peer] \ { message[peer].msrc })]
    /\ message' = [message EXCEPT ![peer] = [msrc  |-> peer, 
                                             mtype |-> Gossip, 
                                             mid   |-> message[peer].mid, 
                                             mbody |-> message[peer].mbody]]
    /\ pc' = [pc EXCEPT ![peer] = "SendGossip"]
    /\ UNCHANGED <<eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, commChannels, msgCounter>>

\* Notify received gossip: send an ihave notification to the lazy peers.
NotifyGossip(peer) == 
    /\ pc[peer] = "NotifyGossip"
    /\ recipient' = [recipient EXCEPT ![peer] = (lazyPushPeers[peer] \ { message[peer].msrc })]
    /\ message' = [message EXCEPT ![peer] = [msrc  |-> peer, 
                                             mtype |-> IHave, 
                                             mid   |-> message[peer].mid]]
    /\ pc' = [pc EXCEPT ![peer] = "SendIHave"]
    /\ UNCHANGED <<commChannels, msgCounter, eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers>>

\* Send the gossip to the eager peers in a loop, then move on to notify the lazy peers
SendGossip(peer) == 
    /\ pc[peer] = "SendGossip"
    /\ SendMsg(peer, "NotifyGossip") 

\* Send a gossip notification to the lazy peers in a loop, then continue to communicate
SendIHave(peer) == 
    /\ pc[peer] = "SendIHave"
    /\ SendMsg(peer, "Ready") 

\* Process a message
ProcessMessage(peer) == 
    /\ pc[peer] = "ProcessMessage"
    /\ LET m == message[peer] IN 
        CASE m.mtype = Gossip -> IF m.mid \notin DOMAIN receivedUpdates[peer] 
                                 THEN ReceiveGossip(peer)
                                 ELSE RejectGossip(peer)
        []   m.mtype = IHave ->  IF m.mid \notin DOMAIN receivedUpdates[peer] 
                                 THEN ReceiveIHave(peer)
                                 ELSE /\ pc' = [pc EXCEPT ![peer] = "Ready"]
                                      /\ UNCHANGED <<eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, 
                                                     timers, commChannels, msgCounter, recipient, message>>
        []   m.mtype = Graft ->  ReceiveGraft(peer)
        []   m.mtype = Prune ->  ReceivePrune(peer)


\* The following actions are enabled when a peer is ready to communicate
-----
\* Time-out on a message and try requesting it again from the next source.
Timeout(peer) == 
    /\ Len(timers[peer]) > 0
    /\ LET id == Head(timers[peer]) IN \* timer of the longest-waiting message expires
        LET neighbour == IF Len(missingUpdateSrcs[peer][id]) > 0 
                          THEN Head(missingUpdateSrcs[peer][id]) ELSE Nil 
        IN 
        \/ (/\ neighbour = Nil \* No available source, keep waiting for another IHave
            /\ timers' = [timers EXCEPT ![peer] = Append(Tail( @ ), id)]
            /\ UNCHANGED <<pc, commChannels, msgCounter, eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, recipient, message>>)
        \/ (/\ neighbour # Nil
            /\ Send([mtype |-> Graft,
                     msrc  |-> peer,
                     mid   |-> id], 
                    peer, neighbour)
            /\ eagerPushPeers' = [eagerPushPeers EXCEPT ![peer] = @ \cup {neighbour}]
            /\ lazyPushPeers'  = [lazyPushPeers EXCEPT ![peer] = @ \ {neighbour}]
            /\ missingUpdateSrcs' = [missingUpdateSrcs EXCEPT ![peer][id] = Tail( @ )]
            /\ timers' = [timers EXCEPT ![peer] = Append(Tail( @ ), id)]
            /\ UNCHANGED <<pc, msgCounter, receivedUpdates, recipient, message>>)

\* Receive a message and process it
ReceiveMessage(peer) == 
    \E p \in Peer: /\ Len(commChannels[p, peer].in) > 0 
                   /\ message' = [message EXCEPT ![peer] = Head(commChannels[p, peer].in)]
                   /\ Receive(peer, p)
                   /\ pc' = [pc EXCEPT ![peer] = "ProcessMessage"] 
                   /\ UNCHANGED <<eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, recipient, msgCounter>>

\* Generate gossip and start spreading it
\* For simplicity, the gossip message is created and assigned to message[peer], and then sent over the network to localhost 
GenerateGossip(peer) == 
    /\ msgCounter <= MAX_MSG
    /\ CreateGossipMsg(peer)  \* sets message[peer] to the new gossip message
    /\ Send( message'[peer], peer, peer )  \* gossip is sent to localhost
    /\ msgCounter' = msgCounter + 1
    /\ UNCHANGED <<pc, receivedUpdates, eagerPushPeers, lazyPushPeers, missingUpdateSrcs, timers, recipient>>

\* On each communication cycle either process a message, send new gossip or timeout on a missing message
PTCycle(peer) == /\ pc[peer] = "Ready"
                 /\ \/ ReceiveMessage(peer) 
                    \/ GenerateGossip(peer)
                    \/ Timeout(peer)


\******************************* Spec Body ******************************
-------------------------------------------------------------------------

\* A conjunct describing a consistent state of the system: all updates received at all peers
ConsistentData == /\ \A p \in Peer: DOMAIN missingUpdateSrcs[p] = {} 
                  /\ \A p1, p2 \in Peer: DOMAIN receivedUpdates[p1] = DOMAIN receivedUpdates[p2] 
                  /\ \A p1, p2 \in Peer: \A id \in DOMAIN receivedUpdates[p1]: 
                                                     receivedUpdates[p1][id] = receivedUpdates[p2][id]
                  /\ \A p \in Peer: DOMAIN timers[p] = {}

\* All possible communication states of a peer
s(p) == \/ PTCycle(p) 
        \/ ProcessMessage(p) 
        \/ SendGossip(p) 
        \/ SendIHave(p) 
        \/ HandleGossip(p) 
        \/ NotifyGossip(p) 

\* Init all protocol-related variables
InitPeers == /\ eagerPushPeers = [i \in Peer |-> PeerSamplingService[i]]
             /\ lazyPushPeers = [i \in Peer |-> {}]
             /\ receivedUpdates = [i \in Peer |-> [j \in {} |-> {}]]
             /\ missingUpdateSrcs = [i \in Peer |-> [j \in {} |-> <<>>]]
             /\ timers = [i \in Peer |-> <<>>]

\* Init auxiliary variables - dummy messages, no recipients and ready to communicate
InitAuxVars == /\ message = [i \in Peer |-> [msrc  |-> i, 
                                             mtype |-> Prune]]
               /\ recipient = [i \in Peer |-> {}]
               /\ pc = [i \in Peer |-> "Ready"]

\* The initial predicate: init the network, the peers and the auxiliary variables
InitPT == /\ NWInit 
          /\ InitPeers
          /\ InitAuxVars
          
\* The next-state relation: communicate, or stutter when there are no more updates and 
\* consistency was reached
NextPT == \/ \E p \in Peer: s(p)
          \/ (* Disjunct to prevent deadlock on termination *)
             /\ msgCounter > MAX_MSG
             /\ ConsistentData
             /\ UNCHANGED allvars
         
\* The test-spec - take communication steps as long as they are enabled              
SpecPT == InitPT /\ [][NextPT]_allvars /\ \A p \in Peer: WF_allvars(s(p))

\* A helper operator for enumerating finite sequences, because TLC can't enumerate Seq(S)
FiniteSubseq(S) == LET T == 1..Cardinality(S) IN
                   UNION {[R -> S] : R \in (SUBSET T)}


\* Invariant for verifying variable assignment during execution
TypeOk == LET midSubsets == (SUBSET MID)     \* for efficiency - only calculate this set once 
              seqPeer == FiniteSubseq(Peer)  \* finite Seq(Peer)
              seqMid == FiniteSubseq(MID)    \* finite Seq(MID)
              p2p == [Peer -> (SUBSET Peer)] \* for efficiency - only calculate this set once
          IN
          /\ NWTypeOK
          /\ eagerPushPeers \in p2p
          /\ lazyPushPeers \in p2p
          /\ \A p \in Peer: /\ DOMAIN receivedUpdates[p] \in midSubsets
                            /\ \A id \in DOMAIN receivedUpdates[p]: receivedUpdates[p][id] \in MBody
          /\ missingUpdateSrcs \in [Peer -> UNION {[T -> seqPeer] : T \in midSubsets}]
          /\ timers \in [Peer -> seqMid]
          /\ message \in [Peer -> Msg]
          /\ recipient \in p2p
          /\ pc \in [Peer -> {"Ready", "ProcessMessage", "SendIHave", "SendGossip", "NotifyGossip", "HandleGossip"}]

\* Temporal formula asserting that once new updates stop appearing - the system eventually reaches consistency
ReachConsistency == (msgCounter > MAX_MSG) ~> ConsistentData 

===============================================================================
