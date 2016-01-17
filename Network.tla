------------------------------ MODULE Network ------------------------------
EXTENDS Integers, Sequences, TLAPS, NaturalsInduction, WellFoundedInduction

CONSTANT Peer \* The set of communicating peers
CONSTANT Msg  \* Allowed messages to be sent over the network

CONSTANT MAX_MSG \* For testing - maximum amount of messages to send

ASSUME MAX_MSG \in Nat

\* The channels for sending messages. A channel is comprised of an "in" and "out" FIFO queue
VARIABLES commChannels 

VARIABLE msgCounter \* For testing - message counter

\* All variables of this module - for stuttering
netvars == << commChannels, msgCounter >>


\*************************** Utility Operators **************************
-------------------------------------------------------------------------

\* A message is received by moving it from the "in" queue of the channel to the "out" queue
Receive(to, from) == /\ commChannels' = [commChannels EXCEPT ![from, to] =
                                            [commChannels[from, to] EXCEPT
                                                          !.out = Append(@, Head(commChannels[from, to].in)),
                                                          !.in  = Tail(@)]]
                     /\ UNCHANGED << msgCounter >>

\* A message is sent by appending it to the "in" queue of the relevant channel
Send(m, from, to) == commChannels' = [commChannels EXCEPT ![from, to] =
                                           [commChannels[from, to] EXCEPT !.in = Append(commChannels[from, to].in, m)]]

\* Does peer i have a message waiting for it from peer j 
HasMsg(i, j) == Len(commChannels[j, i].in) > 0 


\****************************** Spec Body *******************************
-------------------------------------------------------------------------

\* Type-safety invariant
NWTypeOK == /\ msgCounter \in Nat
            /\ \A p1 \in Peer, p2 \in Peer: /\ commChannels[p1, p2].in \in Seq(Msg) 
                                            /\ commChannels[p1, p2].out \in Seq(Msg)

\* Initial state predicate: channels are empty and current message number is 1
NWInit == /\ commChannels = [peer1 \in Peer, peer2 \in Peer |->
                                             [in |-> <<>>, out |-> <<>>] ]
          /\ msgCounter = 1


\**************************** Test-Spec Body ****************************
\* This behavior spec sends dummy messages and checks they arrive
-------------------------------------------------------------------------

\* A communication step consists of either sending to receiving a message
LOCAL Comm(self) == \/ /\ msgCounter <= MAX_MSG
                       /\ \E m \in Msg, p \in Peer: Send(m, self, p)
                       /\ msgCounter' = msgCounter + 1
                    \/ \E p \in Peer: /\ HasMsg(self, p)
                                      /\ Receive(self, p)

\* The next-state relation: send or receive a message, or stutter if done communicating
LOCAL NWNext == \/ (\E self \in Peer: Comm(self))
                \/ (* Disjunct to prevent deadlock on termination *)
                   /\ msgCounter > MAX_MSG
                   /\ \A p1 \in Peer, p2 \in Peer: Len(commChannels[p1, p2].in) = 0
                   /\ UNCHANGED netvars

\* The test-spec - send and receive messages until MAX_MSG messages have been exchanged              
LOCAL NWSpec == /\ NWInit /\ [][NWNext]_netvars
                /\ \A self \in Peer : WF_netvars(Comm(self))


\* A recursive function for counting all messages received from a given peer
Count[i \in Peer, P \in SUBSET Peer] == IF P = {} THEN 0 
                                        ELSE LET j == CHOOSE p \in P: TRUE 
                                             IN Len(commChannels[i, j].out) + 
                                                Count[i, P \ {j}]

\* A recursive function for counting all messages received in the network
CountMsgs[P \in SUBSET Peer] == IF P = {} THEN 0 
                                          ELSE LET i == CHOOSE p \in P: TRUE 
                                               IN Count[i, Peer] + 
                                                  CountMsgs[P \ {i}]

\* Temporal formula asserting that once no more messages are sent - all sent messages are 
\* eventually received at their target peers
LOCAL Liveness == msgCounter > MAX_MSG ~> /\ \A p1 \in Peer, p2 \in Peer: Len(commChannels[p1, p2].in) = 0
                                          /\ CountMsgs[Peer] = MAX_MSG  

============================================================
