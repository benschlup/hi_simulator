// Human teacher agent in project hi_simulator
// Benjamin Schlup, Student ID 200050007
//
// This is an agent simulating a dedicated teacher. This frees the human
// customer support agent up from providing the computational agents
// with new knowledge.
//
// =============================================================================
// START GOAL
// =============================================================================
// After agent startup, check if there is any teaching task already there:
// Process them.
+!start
    <- .findall([CA_name, Situation, Operation],
                csa_teaching_required(CA_name, Situation, Operation),
                CSA_teaching_required);
       !start(CSA_teaching_required).

+!start([]).
+!start([[CA_name, Situation, Operation] | Other_teaching_tasks])
    <- !!dedicated_teaching(CA_name, Situation, Operation);
       !start(Other_teaching_tasks).

// =============================================================================
// SIGNAL HANDLING
// =============================================================================
// When the teacher learns about a new teaching request, pick it up:
@csa_teaching_required [atomic]
+csa_teaching_required(CA_name, Situation, Operation)
    <- !dedicated_teaching(CA_name, Situation, Operation).


{ include ("inc/human_capabilities.asl") }

