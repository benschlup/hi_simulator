// Human (customer) support agent (HSA) in project hi_simulator
// Benjamin Schlup, Student ID 200050007
// (ben.schlup@schlup.com)
//
// This agent simulates a human assisting a computational support agent
// in handling customer service requests.
//
// =============================================================================
// START GOAL
// =============================================================================
// After agent startup, check if there is any tasks already waiting for support:
+!start
    <- !assist_csa.

// =============================================================================
// AGENT-SPECIFIC RULES
// =============================================================================
// Check if a human teacher is available in the HIS, identified by its name HTA
hta_available :- .all_names(Agents) & .member(hta, Agents).

// =============================================================================
// SIGNAL HANDLING
// =============================================================================
// None: A human should not get interrupted in his work through requests from
// the outside.

// =============================================================================
// AGENT-SPECIFIC PLANS
// =============================================================================
// -----------------------------------------------------------------------------
// Main loop of the human customer support agent
// -----------------------------------------------------------------------------
// Assistance consists of (a) looking up all requests for assistance on the
// blackboard, (b) sorting the requests based on their number to start with
// the oldest, (c) actually supporting the CSAs, (d) reviewing any backlog
// of needs to teach computational agents. After such a cycle, the agent
// considers taking a break - unless there is still something to do.
// Note that a person always pursues only one task, thus this is defined
// as atomic plan.
@assist_csa [atomic]
+!assist_csa
    <-  .findall([CA_name, Task_name], ha_help_required(CA_name, Task_name), HA_help_required);
        .sort(HA_help_required, Sorted_help_requests);
        !assist_csa(Sorted_help_requests);
        !review_teaching_backlog;
        !consider_a_break;
        !assist_csa.

// Start a break if there is no more work pending:
+!consider_a_break
    :  not break_start_time(_)
    &  ( hta_available | (.count(csa_teaching_required(_,_,_), CSA_teaching_requests) & CSA_teaching_requests == 0)
        & .count(ha_help_required(_,_), HA_assistance_requests) & HA_assistance_requests == 0 )
    <- ?simulation_time(Simulation_time);
       +break_start_time(Simulation_time);
       log("Starting break");
       !consider_a_break.

// If there is still work around, delay any break:
+!consider_a_break
    :  not break_start_time(_)
    &  ( (not hta_available & .count(csa_teaching_required(_,_,_), CSA_teaching_requests) & CSA_teaching_requests > 0)
       | .count(ha_help_required(_,_), HA_assistance_requests) & HA_assistance_requests > 0 ).

// If during a break, new work arrives, start working again:
+!consider_a_break
    :  break_start_time(Break_start_time)
    &  ( (not hta_available & .count(csa_teaching_required(_,_,_), CSA_teaching_requests) & CSA_teaching_requests > 0)
       | .count(ha_help_required(_,_), HA_assistance_requests) & HA_assistance_requests > 0 )
    <- log("MANAGEMENT", "", "Waiting time", "", Break_start_time, -1);
       -break_start_time(Break_start_time).

// Continue with the break if there is no further work is requested:
+!consider_a_break
    :  break_start_time(_)
    &  (hta_available | (.count(csa_teaching_required(_,_,_), CSA_teaching_requests) & CSA_teaching_requests == 0))
    &  (.count(ha_help_required(_,_), HA_assistance_requests) & HA_assistance_requests == 0)
    <- await(1);
       !consider_a_break.

// -----------------------------------------------------------------------------
// Plans for handling a list of requests for human assistance
// -----------------------------------------------------------------------------
// Stop recursion if the list is empty:
+!assist_csa([]).

// Pick up the next request and start working, continue with other requests thereafter:
+!assist_csa([[CA_name, Task_name] | Other_tasks])
    <- !assist_csa(CA_name, Task_name, "OPEN");
       !assist_csa(Other_tasks).

// The sequence for assisting on a single task consists of (a) focusing on the task
// (b) reviewing the knowledge base required to take the task forward, (c) evaluate
// the situation, (d) escalate in case there is no appropriate knowledge in the
// belief base, (e) execute the next operation based on known situations,
// (d) try to hand back the task to the CA before continuing.
+!assist_csa(CA_name, Task_name, "OPEN")
    :  ha_help_required(CA_name, Task_name)
    <- !refocus_agent(Task_name);
       !review_knowledge(CA_name, Task_name);
       !evaluation(Situations, Task_name);
       !escalate_unknown_situations(Situations, Task_name);
       !execute_next_operation(Situations, Task_name, New_task_status);
       !handback_if_possible(CA_name, Task_name, New_task_status);
       !assist_csa(CA_name, Task_name, New_task_status).


// The task is no longer OPEN or the help request is not active anymore:
+!assist_csa(CA_name, Task_name, Task_status)
    :  not ha_help_required(CA_name, Task_name)
    |  not Task_status == "OPEN"
    <- !stop_focus(Task_name);
       !remove_note_from_blackboard(Task_name, "ha_help_required", [CA_name, Task_name]).

// Gracefully handle the case when the task has disappeared:
-!assist_csa(CA_name, Task_name, Task_status) [focus_fail("cartago.ArtifactNotAvailableException")]
   <-  log("MANAGEMENT",Task_name,"Assisting CSA","FAILED (Task artefact disappeared)");
       !remove_note_from_blackboard(Task_name, "ha_help_required", [CA_name, Task_name]);
       !stop_focus(Task_name).

// If we have the same situational knowledge and the operation was unexpected:
+!review_knowledge(CA_name, Task_name)
    :  task_note(CA_name, Situation, Operation, "Unexpected operation")
       & domain_knowledge(Situation, Operation)
    <- // Forget our own knowledge about this situation as well:
       !forget_domain_knowledge(Situation);
       // Then retry
       !review_knowledge(CA_name, Task_name).

// -----------------------------------------------------------------------------
// Plans for knowledge maintenance
// -----------------------------------------------------------------------------
// If we have different situational knowledge and the operation was unexpected:
+!review_knowledge(CA_name, Task_name)
    :  task_note(CA_name, Situation, Operation, "Unexpected operation")
       & not domain_knowledge(Situation, Operation)
    <- .send(CA_name,achieve,forget_domain_knowledge(Situation));
       !remove_note_on_task(Task_name, [CA_name, Situation, Operation, "Unexpected operation"]);
       !review_knowledge(CA_name, Task_name).

// In case we do not know how to handle the situation, learn one next atomic task ad hoc:
+!review_knowledge(CA_name, Task_name)
    :  not task_note(CA_name, Situation, Operation, "Unexpected operation")
    &  situations([], Task_name)
    <- lookupArtifact(Task_name, Task_id);
       currentAtomicTasks(Situation_operations) [artifact_id(Task_id)];
       .nth(0, Situation_operations, Situation_operation);
       !learn([Situation_operation]).

// If the last performed operation was not unexpected and we have appropriate knowledge,
// just continue:
+!review_knowledge(CA_name, Task_name)
    :  not task_note(CA_name, Situation, Operation, "Unexpected operation")
    &  not situations([], Task_name).

// In the exceptional case there is nothing left to do:
-!review_knowledge(Agent_name, Task_name) [error_msg("No atomic tasks pending")].

// In case the task disappeared meanwhile
-!review_knowledge(Agent_name, Task_name) [error_msg("cartago.ArtifactNotAvailableException")]
    <- log("MANAGEMENT",Task_name,"Reviewing knowledge", "Task artefact not available any longer");
       !remove_note_from_blackboard(Task_name, "ha_help_required", [Agent_name, Task_name]).

// -----------------------------------------------------------------------------
// Plans for escalations
// -----------------------------------------------------------------------------
// In case we know what to do, no need for escalation:
+!escalate_unknown_situations(Situations,_)
    :  not Situations == [].

// In case we have no knowledge, escalate and dispose task artefact:
+!escalate_unknown_situations([], Task_name)
    <- !stop_focus(Task_name);
       log("DOMAIN",Task_name,"Escalation due to lack of knowledge: disposing task");
       lookupArtifact(Task_name, Task_id);
       disposeArtifact(Task_id);
       !remove_note_from_blackboard(Task_name, "ha_help_required", [Agent_name, Task_name]);
       .drop_intention(assist_csa(_, Task_name, _)).

// In case the task disappeared meanwhile
-!escalate_unknown_situations(_,Task_name) [error_msg("cartago.ArtifactNotAvailableException")]
    <- log("MANAGEMENT",Task_name,"Reviewing knowledge", "Task artefact not available any longer");
       !remove_note_from_blackboard(Task_name, "ha_help_required", [Agent_name, Task_name]);
       .drop_intention(assist_csa(CA_name, Task_name)).

// -----------------------------------------------------------------------------
// Plans for escalations
// -----------------------------------------------------------------------------
// Execute just a single pending operation, or none if the list is empty. As
// the computational agent was obviously lacking knowledge about the situation,
// take a note of the need for teaching:
+!execute_next_operation([],_,_).
+!execute_next_operation([Situation|_], Task_name, New_task_status)
    <- ?domain_knowledge(Situation, Operation);
       !execute_operation(Situation, Operation, Task_name, New_task_status);
       ?ha_help_required(CA_name, Task_name);
       !teaching_note(CA_name, Situation, Operation).

// It seems our last operation wasn't successful, let's never retry that by forgetting about it:
-!execute_next_operation([Situation|_],_,"FAILED") [exec_fail("Unexpected operation")]
    <- !forget_domain_knowledge(Situation).

// It seems our last operation wasn't successful for quality reasons, let's retry that:
-!execute_next_operation([Situation|_], Task_name, "OPEN") [quality_fail(_,_)].

// It seems our last operation wasn't successful for other reasons, let's record that:
-!execute_next_operation([Situation|_], Task_name, "FAILED") [exec_fail(Error_message)]
    <- .concat("Failure in situation ", Situation, Activity);
       .concat("FAILED (", Error_message, ")", Error_text);
       log("DOMAIN",Task_name, Activity, Error_text).

// Only take note of teaching if someone is around that handles teaching
+!teaching_note(CA_name, Situation, Operation)
    :  hta_available
    |  not non_teaching_agent
    <- !add_note_to_blackboard(csa_teaching_required,[CA_name, Situation, Operation]).

+!teaching_note(_,_,_).

// -----------------------------------------------------------------------------
// Plans for handling teaching work
// -----------------------------------------------------------------------------
// There is no need to review the teaching backlog
// - If there is a dedidcated teacher in the HIS, or
// - the human customer support agent is not supposed to assume teaching responsibilities
+!review_teaching_backlog
    :  hta_available
    |  non_teaching_agent.

// In case the teaching backlog is empty, there is nothing to do:
+!review_teaching_backlog
    :   (.count(csa_teaching_required(_,_,_), CSA_teaching_requests) &  CSA_teaching_requests == 0).

// Only pick up a next teaching note if no requests for operational human assistance are pending:
+!review_teaching_backlog
    :  (.count(csa_teaching_required(_,_,_), CSA_teaching_requests) &  CSA_teaching_requests > 0)
    &  (.count(ha_help_required(_,_), HA_assistance_requests) &  HA_assistance_requests == 0)
    <- ?csa_teaching_required(CA_name, Situation, Operation);
       !dedicated_teaching(CA_name, Situation, Operation).

// otherwise suspend teaching in favour of providing assistance on open tasks:
+!review_teaching_backlog
    <- log("MANAGEMENT", "", "Suspending teaching due to requests for HA assistance").


{ include ("inc/human_capabilities.asl") }
