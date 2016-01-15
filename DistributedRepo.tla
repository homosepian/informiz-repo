-------------------------- MODULE DistributedRepo --------------------------
EXTENDS Naturals, FiniteSets, Sequences, TLC

\* The set of repository IDs
CONSTANT Repository

\* Possible entity ids
CONSTANT ENTITY_ID

\* Possible entity values
CONSTANT ENTITY_VAL

\* Plumtree and Network constants
CONSTANTS CreateGossipMsg(_), ProcessGossip(_,_), Gossip, IHave, Graft, Prune, PeerSamplingService, Msg, MAX_MSG

\* Plumtree and Network variables
VARIABLES msgCounter, commChannels
VARIABLES eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, pc, recipient, message 

\* Entities to exchange in updates between repositories
\* tOrder is a test field used for simulating conflict resolution between concurrent updates.
Entity == [id: ENTITY_ID, val: ENTITY_VAL, vclock: UNION {[S -> Nat] : S \in (SUBSET Repository)}, tOrder: Nat]

\* The local replica at each repository
\* This is a mapping between a repository and another mapping, of entity id to entity
\* E.g {r1 -> { id1 -> e1, id2 -> e2, ...}, r2 -> {...}, ...}
VARIABLE replica

\* All variables, for stuttering
vars == <<eagerPushPeers, lazyPushPeers, receivedUpdates, missingUpdateSrcs, timers, 
             commChannels, msgCounter, pc, recipient, message, replica>>


\*************************** Utility Operators **************************
-------------------------------------------------------------------------

MaxVal(m, n) == IF m < n THEN n ELSE m

\* For conflict resolution. In an actual implementation the application resolves conflicts.
\* But for the purpose of this spec, the latest update wins
resolveConflict(ver1, ver2) == IF ver1.tOrder > ver2.tOrder 
                               THEN ver1 ELSE ver2 

\* Is vv1 bigger than vv2
dominates(vv1, vv2) == IF /\ \A r \in (DOMAIN vv1 \cap DOMAIN vv2) : vv1[r] >= vv2[r]
                          /\ \neg  (\E r \in DOMAIN vv2: 
                                       r \notin DOMAIN vv1)
                          /\ ( \/ \E r \in (DOMAIN vv1 \cap DOMAIN vv2) : vv1[r] > vv2[r] 
                               \/  (\E r \in DOMAIN vv1: 
                                       r \notin DOMAIN vv2) )
                       THEN TRUE ELSE FALSE

\* Update vv1 with vv2
updateVV(vv1, vv2) == [r \in (DOMAIN vv1 \cup DOMAIN vv2) |-> 
                         CASE r \in (DOMAIN vv2 \cap DOMAIN vv1) -> MaxVal(vv1[r], vv2[r]) 
                         [] r \in DOMAIN vv1 -> vv1[r] 
                         [] OTHER -> vv2[r]]            

\* Merge two versions of an entity
mergeEntity(ent1, ent2) == CASE dominates(ent1.vclock, ent2.vclock) -> ent1
                             [] dominates(ent2.vclock, ent1.vclock) -> ent2
                             [] OTHER -> LET ent == resolveConflict(ent1, ent2) IN 
                                         [id     |-> ent.id,
                                          val    |-> ent.val,
                                          tOrder |-> ent.tOrder,
                                          vclock |-> updateVV(ent1.vclock, ent2.vclock)]
                                        
\* Update a local replica with a new or updated entity
UpdateReplica(curReplica, entity) ==
    IF entity.id \in DOMAIN curReplica THEN
        [curReplica EXCEPT ![entity.id] = mergeEntity(@, entity)]
    ELSE
        curReplica @@ (entity.id :> entity) 

\* The ProcessGossip implementation
ReceiveGossipMsg(peer, msg) == 
    LET curReplica == UpdateReplica(replica[peer], msg.mbody) IN 
       /\ replica' = [ replica EXCEPT ![peer] = curReplica ]

\* Create an entity with the given id and value, and update its version clock
CreateEntity(peer, eid, v) == [id     |-> eid,
                               val    |-> v,
                               tOrder |-> msgCounter,
                               vclock |-> IF eid \in DOMAIN replica[peer] 
                                          THEN updateVV(replica[peer][eid].vclock, (peer :> msgCounter)) 
                                          ELSE (peer :> msgCounter)]

\* The CreateGossipMsg implementation
CreateUpdate(peer) == \E eid \in ENTITY_ID, v \in ENTITY_VAL:
                        message' = [message EXCEPT ![peer] = 
                            [mtype |-> Gossip, 
                             mid   |-> msgCounter, 
                             mbody |-> CreateEntity(peer, eid, v),
                             msrc  |-> peer]]

\* The refinement mapping of the Plumtree protocol
PT == INSTANCE Plumtree WITH Peer <- Repository, MBody <- Entity

\* A conjunct describing a consistent state of the distributed repository - all replicas contain identical entities
ConsistentRepo == /\ PT!ConsistentData
                  /\ \A r1, r2 \in Repository: DOMAIN replica[r1] = DOMAIN replica[r2] 
                  /\ \A r1, r2 \in Repository: \A id \in DOMAIN replica[r1]: 
                                                     replica[r1][id] = replica[r2][id]

\* The communication behavior - handling gossip changes the local replica
s(p) == \/ /\ \/ PT!PTCycle(p) 
              \/ PT!ProcessMessage(p) 
              \/ PT!SendGossip(p) 
              \/ PT!SendIHave(p) 
              \/ PT!NotifyGossip(p)
           /\ UNCHANGED replica
        \/ PT!HandleGossip(p) 

\* The initial predicate: init Plumtree variables and the local replica at each repository
Init == /\ PT!InitPT
        /\ replica = [i \in Repository |-> [j \in {} |-> PT!Nil]] 

\* The next-state relation: communicate, or stutter when there are no more updates and 
\* consistency was reached
Next == \/ \E p \in Repository: s(p)
        \/ (* Disjunct to prevent deadlock on termination *)
           /\ msgCounter > MAX_MSG
           /\ ConsistentRepo
           /\ UNCHANGED vars
           
\* The test-spec - take communication steps as long as they are enabled              
Spec == Init /\ [][Next]_vars /\ \A p \in Repository: WF_vars(s(p)) 


\* Invariant for verifying variable assignment during execution
TypeSafe == /\ PT!TypeOk
            /\ \A r \in DOMAIN replica: /\ DOMAIN replica[r] \in (SUBSET ENTITY_ID)
                                        /\ \A id \in DOMAIN replica[r]: replica[r][id] \in Entity

\* Temporal formula asserting that once new updates stop appearing - the repository eventually reaches consistency
ReachRepoConsistency == (msgCounter > MAX_MSG) ~> ConsistentRepo 

===============================================================================
