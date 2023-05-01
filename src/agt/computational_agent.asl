// Computational agent in project hi_simulator
// Benjamin Schlup, Student ID 200050007
//
// This agent simulates a computational support agent handling customer
// support requests represented by CArtAgO task artefacts.
//
// =============================================================================
// AGENT-SPECIFIC START GOAL
// =============================================================================
// Handle any open tasks already existing when the agent starts:
+!start
    <- getCurrentArtifacts(Artifact_names);
       !start(Artifact_names).

// If no open tasks upon startup of agent, fall asleep.
+!start([]).

// If open tasks upon startup of agent, process them.
// Tasks are represented by artefacts that have a name starting with "task_".
+!start([Artifact_name | Other_artifacts])
    :  .substring("task_", Artifact_name, 0)
    <- !!review_task(Artifact_name, "OPEN");
       !start(Other_artifacts).

// When recursively going through the list of artefacts, skip any artefacts with
// names not starting with "task_":
+!start([Artifact_name | Other_artifacts])
    <- !start(Other_artifacts).

// =============================================================================
// AGENT-SPECIFIC SIGNAL HANDLING
// =============================================================================
// Review new tasks when announced by the task set artefact
+task_created(Task_name)
    <- !!review_task(Task_name, "OPEN").

// When a request for human assistance is withdrawn, an event is received. Only
// act when the corresponding task artefact still exists:
-ha_help_required(Agent_name, Task_name)
    <- getCurrentArtifacts(Artifact_names);
       !take_task_back(Task_name, Artifact_names).

+!take_task_back(Task_name, Artifact_names)
    :  .member(Task_name, Artifact_names)
    <- !!review_task(Task_name, "OPEN").

+!take_task_back(Task_name, Artifact_names).

// =============================================================================
// AGENT-SPECIFIC PLANS
// =============================================================================
// -----------------------------------------------------------------------------
// Plans for handling a task
// -----------------------------------------------------------------------------
// Skip tasks during review that are awaiting help from a human agent:
+!review_task(Task_name, "OPEN")
    :  ha_help_required(_,Task_name).

// Skip tasks that are no longer open:
+!review_task(Task_name, Task_status)
    :  not Task_status == "OPEN".

// For the remaining tasks: Review task for potentially new situations that
// require action. The sequence is (a) putting focus on the task artefact,
// (b) evaluating the situation, (c) skipping situations that are marked
// suspicious, (d) requesting human assistance if there
// is no appropriate knowledge in the belief base, (e) or perform any
// operations appropriate for a recognised situation:
@review_task [atomic]
+!review_task(Task_name, Task_status)
    <- !refocus_agent(Task_name);
       !evaluation(Situations, Task_name);
       ?skip_suspicious(Situations, Clean_situations);
       !handover_unknown_situations(Clean_situations, Task_name);
       !execute_operations(Clean_situations, Task_name).

// Handle the case that a task has failed and the artefact has been disposed meanwhile:
-!review_task(Task_name,_) [focus_fail("cartago.ArtifactNotAvailableException")]
    <- log("MANAGEMENT",Task_name,"Reviewing task","FAILED (Task artefact disappeared)").

// Operations marked as suspicious are not executed again: This is done by removing them from the
// list of recognised situations:
skip_suspicious([],[]).

skip_suspicious([Situation | Old_Situations], New_situations)
     :-  domain_knowledge(Situation, Operation)
     &   not suspiciousOperation(Situation, Operation)
     &   New_situations = [Situation | New_situations2]
     &   skip_suspicious(Old_situations, New_situations2).

skip_suspicious([Situation | Situations], New_situations)
    :- skip_suspicious(Situations, New_situations).

// Note: Depending on the HIS, more failure situations could arise that require
//       specific failure-handling plans at this point.

// -----------------------------------------------------------------------------
// Plans for handling situations in which the agent has no appropriate knowledge
// -----------------------------------------------------------------------------
// In case the agent was unable to derive any situation and no active operations
// are ongoing on the task: handover the task to a human agent (HA) by putting
// it onto a blackboard - assuming there are no operations still ongoing:
+!handover_unknown_situations([_|_], _).
+!handover_unknown_situations([], Task_name)
    :  not .intend(execute_operation(_,_,Task_name) [async_and_review])
    <- .my_name(Agent_name);
       !add_note_to_blackboard(Task_name, "ha_help_required", [Agent_name,Task_name]).

// In case there are no new situations recognised, but ongoing operations: do nothing,
// as otherwise a human action may interfere with ongoing operations triggered by the
// computational support agent:
+!handover_unknown_situations([], Task_name)
    :  .intend(execute_operation(_, _, Task_name) [async_and_review])
    <- .wait(100);
       !handover_unknown_situations([], Task_name).

// -----------------------------------------------------------------------------
// Plans for executing operations based on a list of recognised situations
// -----------------------------------------------------------------------------
// In case there are no new situations to be handled: do nothing.
+!execute_operations([], Task_name).

// In case we are not yet acting on the task, but it is known how: embrace goal to execute operation.
// Do that asynchronously, as a computational agent may multi-task and scale up easily:
+!execute_operations([Situation|Situations], Task_name)
    :  domain_knowledge(Situation, Operation)
    &  not .intend(execute_operation(Situation, Operation, Task_name) [async_and_review])
    <- !!execute_operation(Situation, Operation, Task_name) [async_and_review];
       !execute_operations(Situations, Task_name).

// In case next situation on the list is already being handled by active intention, skip it and
// look into other situations that are recognised:
+!execute_operations([Situation|Situations], Task_name)
    :  domain_knowledge(Situation, Operation)
    &  .intend(execute_operation(Situation, Operation, Task_name) [async_and_review])
    <- !execute_operations(Situations, Task_name).

// After executing a particular operation asynchronously, the task should be reviewed again,
// as it could be in a new known status requiring new operations to be scheduled:
+!execute_operation(Situation, Operation, Task_name) [async_and_review]
    <- !execute_operation(Situation, Operation, Task_name, Task_status);
       !!review_task(Task_name, Task_status).

// Agent-specific plan to handle unexpected operations:
-!execute_operation(Situation, Operation, Task_name) [async_and_review, exec_fail("Unexpected operation")]
    <- .my_name(Agent_name);
       !update_note_on_task(Task_name, [Agent_name, Situation, Operation, "Unexpected operation"]);
       +suspiciousOperation(Situation, Operation);
       !add_note_to_blackboard("ha_help_required", [Agent_name, Task_name]).

// Agent-specific plan to handle disappearance of artefact:
-!execute_operation(Situation, Operation, Task_name) [async_and_review, exec_fail("cartago.ArtifactNotAvailableException")]
    <- .drop_desire(review_task(Task_name,_)).

// It seems our last operation wasn't successful for quality reasons, let's retry that:
-!execute_operation(Situation, Operation, Task_name) [quality_fail(_,_)]
    <- !!review_task(Task_name, "OPEN").

// Agent-specific plan to handle timeout:
-!execute_operation(Situation, Operation, Task_name) [async_and_review, exec_fail("TIMEOUT")]
    <- .drop_desire(review_task(Task_name,_)).

// -----------------------------------------------------------------------------
// Plans for responding to requests from human agents for reevaluating a task
// -----------------------------------------------------------------------------
// Provide the HA requesting a reevaluation with a list of recognised situations:
@reevaluation [atomic]
+?reevaluation(Clean_situations, Task_name)
    <-  !refocus_agent(Task_name);
        log("DOMAIN", Task_name, "Reevaluating task with current knowledge");
        !evaluation(Situations, Task_name);
        ?skip_suspicious(Situations, Clean_situations).

// If the request referenced a task for which no artefact is available, respond
// with an empty list, indicating that the task cannot be taken back.
-?reevaluation([], Task_name) [focus_fail("cartago.ArtifactNotAvailableException")]
    <-  log("DOMAIN", Task_name, "Task unavailable for reevaluation").


{ include ("inc/common_capabilities.asl") }
