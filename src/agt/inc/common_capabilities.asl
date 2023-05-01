// Shared code for agents that are part of a simulated Hybrid Intelligent System.
// Benjamin Schlup, Student ID 200050007
// (ben.schlup@schlup.com)
//
// =============================================================================
// INITIAL GOALS
// =============================================================================
!init.
+!init
    <- !init_log;
       log("Agent started");
       !loadInitialKnowledge;
       !!start.

// Initialise an agent-specific display for the log if a display name is
// provided as belief during agent startup (i.e. in the JaCaMo project file)
+!init_log
    :  agent_displayname(Agent_displayname)
    <- .my_name(Agent_name);
        log_display(Agent_name, Agent_displayname).

// Otherwise: no agent-specific display initialisation for logging
+!init_log.

// Plan for loading the initial knowledge that an agent possesses, based on
// the agent type:
+!loadInitialKnowledge
  :  agent_type(_)
  <- log("MANAGEMENT","Loading initial knowledge");
     ?agent_type(Agent_type);
     initialKnowledge(Agent_type, Situation_operations);
     !learn(Situation_operations).

// If no agent_type belief, there is no initial knowledge to be loaded:
+!loadInitialKnowledge
  <- log("MANAGEMENT","Unspecified agent type, no learning of initial knowledge").

// =============================================================================
// RULES AND PLANS FOR HANDLING KNOWLEDGE AND ASSESSING SITUATIONS
// =============================================================================
// Check if all conditions of a situation are met, or not met respectively:
all_set([]) :- true.
all_set([Condition|Conditions]) :- task_property(Condition) & all_set(Conditions).
not_set([]) :- true.
not_set([Condition|Conditions]) :- not task_property(Condition) & not_set(Conditions).

// Domain knowledge related rules - does the agent know about a situation?
domain_knowledge(Situation) :- domain_knowledge(Situation,_,_,_,_,_).

// Which operation is appropriate in the current situation?
domain_knowledge(Situation,Operation) :- domain_knowledge(Situation,Operation,_,_,_,_).

// Retrieve time required for teaching:
domain_teaching_knowledge(Situation,Operation,Teaching_time) :-
    domain_knowledge(Situation,Operation,_,_,_,[_,_,_,_,_,_,Teaching_time]).

// Retrieve time required for (self-)learning:
domain_learning_time([_,_,_,_,_,Configured_learning_time,_],Learning_time)
    :- Learning_time = Configured_learning_time.

// ---------------------------------------------------------------------------------------------------------------------
// PLANS HANDLING AGENT LEARNING
// ---------------------------------------------------------------------------------------------------------------------
// An empty list - nothing to be done:
+!learn([]).
+!learn([[]]).

// Learn new knowledge based on a list of situations/operations. The
// knowledge is provided by the task set artefact and is dependent on
// agent type:
+!learn([[Situation,Operation]|Situation_operations])
  <- ?agent_type(Agent_type);
     getDomainTaskKnowledge(Agent_type, Situation, Operation,
	                  Must_triggers, Must_not_triggers, Eval_config, Exec_config);
     !learn_domain_knowledge(Situation, Operation, Must_triggers,
	                  Must_not_triggers, Eval_config, Exec_config);
     !learn(Situation_operations).

// Incremental learning while agents are in "run-mode" simulates learning effort:
+!learn_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config)
    :  (not .intend(init))
       & (domain_learning_time(Eval_config, Eval_learning_time) & Eval_learning_time >= 0)
       & (domain_learning_time(Exec_config, Exec_learning_time) & Exec_learning_time >= 0)
    <- .concat("Simulate learning (",Situation,": ",Operation,")", Start_log);
       log("MANAGEMENT", "", Start_log);
       ?simulation_time(Start_time);
       -learning_start_time(Situation, Operation,_);
       +learning_start_time(Situation, Operation, Start_time);
       await(Eval_learning_time+Exec_learning_time);
       !update_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config);
       -learning_start_time(Situation, Operation, Start_time).

// Incremental learning by agents is sometimes not possible:
+!learn_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config)
    :  not .intend(init)
       & ( (domain_learning_time(Eval_config,Eval_learning_time) & Eval_learning_time < 0)
           | (domain_learning_time(Exec_config,Exec_learning_time) & Exec_learning_time < 0) )
    <- .concat("Cannot self-learn (",Situation,": ",Operation,")",Activity);
       log(Activity).

// Incremental learning by agents during initialisation does not require "waiting":
+!learn_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config)
    :  .intend(init)
    <- !update_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config).

// Plan for updating the agent's knowledge on how to handle a particular situation:
// (a) first drop potentially conflicting situational knowledge from belief base, (b) add new knowledge belief,
// (c) record the time that was used for learning.
+!update_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config)
    <- !forget_domain_knowledge(Situation);
       +domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config);
       ?learning_start_time(Situation, Operation, Start_time);
       -learning_start_time(Situation, Operation,_);
	   .concat("Domain knowledge learned (", Situation, ")", End_log);
       log("MANAGEMENT", "", End_log, "SUCCESS", Start_time, -1).

// In case it was not "self-learning", the starting time is not recorded. This happens when a CA gets taught
// by an HA. By using a negative start time, the logger will record no time span:
+?learning_start_time(Situation, Operation, -1).

// Sometimes it is required to actively forget knowledge about a particular situation - in case we actually have it.
// We also need to forget about experience we had with handling that situation (i.e. recorded successful cycles).
// In case the operation was marked as "suspicious", also forget that.
+!forget_domain_knowledge(Situation) : domain_knowledge(Situation)
    <- -domain_knowledge(Situation,_,_,_,_,_);
       -situationSuccessfulOperationCycles(Situation,_,_);
       -suspiciousOperation(Situation,_);

	   // Record that we have dropped domain knowledge
       .concat("Domain knowledge about situation '", Situation, "' dropped", Activity);
       log("MANAGEMENT", "", Activity).

// If we do not have any knowledge how to handle the situation, we do not need to forget:
+!forget_domain_knowledge(Situation) : not domain_knowledge(Situation).


// ---------------------------------------------------------------------------------------------------------------------
// PLANS FOR EVALUATING A TASK
// ---------------------------------------------------------------------------------------------------------------------
// Obtain list of all situations a task is currently in:
situations(Situations, Task_name)
    :- .findall(Situation,
	         domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers,_,_)
             & all_set(Must_triggers)
             & not_set(Must_not_triggers),
             Situations).

// In case there are situations recognised, simulate the time required for the evaluation:
+!evaluation([], Task_name) : situations([], Task_name).

+!evaluation(Situations, Task_name)
    :  situations(Situations, Task_name) & not Situations == []
    <- ?simulation_time(Start_time);
       log("DOMAIN", Task_name, "Initiating task evaluation");
       !evaluation_wait(Situations, Task_name);
       .term2string(Situations, Situations_string);
       log("DOMAIN", Task_name, "Evaluated situations", Situations_string, Start_time, -1).

// Simulate the actual evaluation. Note that Eval_quality is not yet used / for future use:
+!evaluation_wait([], _).
+!evaluation_wait([Situation|Situations], Task_name)
    <- ?domain_knowledge(Situation, Operation);
       ?situationSuccessfulOperationCycles(Situation, Operation, Cycles);
       ?agent_type(Agent_type);
       simulateEvaluation(Situation, Operation, Agent_type, Cycles, Eval_quality);
       !evaluation_wait(Situations, Task_name).

// ---------------------------------------------------------------------------------------------------------------------
// MAINTAINING TASK PROPERTIES AND NOTES
// ---------------------------------------------------------------------------------------------------------------------
// Create or update a task property:
+!update_task_property(Task_name, Property, Value)
    <- lookupArtifact(Task_name, Task_id);
       updateTaskProperty(Property, Value) [artifact_id(Task_id)].

// Remove a task property:
+!remove_task_property(Task_name, Property, Value)
    <- lookupArtifact(Task_name, Task_id);
       removeTaskProperty(Property, Value) [artifact_id(Task_id)].

// Handle the case that the task artefact has disappeared meanwhile:
-!update_task_property(Task_name, _, _) [error_msg(Error_message)]
    :  Error_message = "cartago.ArtifactNotAvailableException"
    |  Error_message = "Artifact Not Available."
    <- log("MANAGEMENT",Task_name,"Updating task property","Task artefact not available any longer").

-!remove_task_property(Task_name, _, _) [error_msg(Error_message)]
    :  Error_message = "cartago.ArtifactNotAvailableException"
    |  Error_message = "Artifact Not Available."
    <- log("MANAGEMENT",Task_name,"Updating task property","Task artefact not available any longer").

// Gracefully handle any other potential failure:
-!update_task_property(Task_name, _, _) [error_msg(Error_message)]
    <- log("MANAGEMENT",Task_name,"Updating task property",Error_message).

-!update_task_property(Task_name, _, _) [error_msg(Error_message)]
    <- log("MANAGEMENT",Task_name,"Updating task property",Error_message).

// ---------------------------------------------------------------------------------------------------------------------
// MAINTAINING EXPERIENCE
// ---------------------------------------------------------------------------------------------------------------------
// If test goal situationSuccessfulOperationCycles fails, assume that the situation-operation has never occurred so far:
+?situationSuccessfulOperationCycles(Situation, Operation, 0).

// Safely increment the number of successful situation-operation cycles:
@incrementSuccessfulSituationOperationCycles [atomic]
+!incrementSuccessfulSituationOperationCycles(Situation, Operation)
     <- ?situationSuccessfulOperationCycles(Situation, Operation, Cycles);
        New_cycles = Cycles + 1;
        -situationSuccessfulOperationCycles(Situation, Operation, Cycles);
        +situationSuccessfulOperationCycles(Situation, Operation, New_cycles).

// ---------------------------------------------------------------------------------------------------------------------
// STANDARD PLANS FOR EXECUTING OPERATIONS ON TASK ARTEFACT
// ---------------------------------------------------------------------------------------------------------------------
// Execute specific operation: Note that a task does not need to be in focus - e.g. CAs may be multi-tasking.
// After any execution, await disposal of the task artefact if the returned task status is "COMPLETED".
// Furthermore, record any successful execution of an operation as added experience.
+!execute_operation(Situation, Operation, Task_name, Task_status)
    <- .concat("Initiating operation: ",Operation,Activity);
       log("MANAGEMENT",Task_name,Activity);
       ?agent_type(Agent_type);
       ?simulation_time(Start_time);
       ?situationSuccessfulOperationCycles(Situation, Operation, Cycles);
       -+last_execution_start(Situation_name, Operation, Task_name, Start_time);
       lookupArtifact(Task_name, Task_id);
       executeArtifactOperation(Operation, Agent_type, Cycles, Exec_quality, Task_status) [artifact_id(Task_id)];
       !wait_for_disposal_of_non_open_task(Task_name, Task_status);
       !incrementSuccessfulSituationOperationCycles(Situation, Operation);
       log("DOMAIN", Task_name, Operation, "SUCCESS", Start_time, Exec_quality).

// Execution had insufficient quality, record this and retry:
-!execute_operation(Situation, Operation, Task_name, "OPEN") [error_msg(Error_message)]
    :  .substring("QUALITY ISSUE", Error_message, 0)
    <- ?last_execution_start(Situation, Operation, Task_name, Start_time);
       extractErrorDetails(Error_message, Error_details);
       ?unpackQualityError(Error_details, Exec_quality, Minimum_quality);
       .concat("QUALITY BELOW MINIMUM OF ", Minimum_quality, Result);
       log("DOMAIN", Task_name, Operation, Result, Start_time, Exec_quality);
       .fail(quality_fail(Exec_quality, Minimum_quality)).

// Error handling rule for unpacking quality related information:
unpackQualityError([Exec_quality,Minimum_quality],Exec_quality,Minimum_quality).

// Gracefully handle any other errors:
-!execute_operation(Situation, Operation, Task_name, Task_status) [error_msg(Error_message)]
    <- ?last_execution_start(Situation, Operation, Task_name, Start_time);
       .concat("FAILED (", Error_message, ")", Error_log);
       log("DOMAIN", Task_name, Operation, Error_log, Start_time, -1);
       .fail(exec_fail(Error_message)).

// Completed tasks will be disposed in CArtAgO, wait for that to happen:
+!wait_for_disposal_of_non_open_task(Task_name, Task_status)
    :  Task_status == "OPEN".

+!wait_for_disposal_of_non_open_task(Task_name, Task_status)
    :  not Task_status == "OPEN"
    <- getCurrentArtifacts(Artifact_names);
       !wait_for_disposal_of_non_open_task(Task_name, Task_status, Artifact_names).

+!wait_for_disposal_of_non_open_task(Task_name, Task_status, Artifact_names)
    :  .member(Task_name, Artifact_names)
    <- await(1);
       !wait_for_disposal_of_non_open_task(Task_name, Task_status).

+!wait_for_disposal_of_non_open_task(Task_name, Task_status, Artifact_names)
    :  not .member(Task_name, Artifact_names).

// If agent did not track start time - for safety reasons:
+?last_execution_start(Situation, Operation,_,-1).


// ---------------------------------------------------------------------------------------------------------------------
// BLACKBOARD MANAGEMENT PLANS
// ---------------------------------------------------------------------------------------------------------------------
// Add a note to the blackboard without specific reference to a task, no logging:
+!add_note_to_blackboard(Request, Details)
    <- !add_note_to_blackboard("", Request, Details).

// Add a note to the blackboard with reference to a specific task, and log the action:
+!add_note_to_blackboard(Task_name, Request, Details)
    <- .term2string(Details, Details_string);
       .concat("Adding to blackboard: ", Request, " with details ", Details_string, Log_entry);
       log("MANAGEMENT", Task_name, Log_entry);
       addToBlackboard(Request, Details).

// Remove a note from the blackboard without specific reference to a task, no logging:
+!remove_note_from_blackboard(Request, Details)
    <- !remove_note_from_blackboard("", Request, Details).

// Remove a note from the blackboard with reference to a specific task, and log the action:
+!remove_note_from_blackboard(Task_name, Request, Details)
    <- .term2string(Details, Details_string);
       .concat("Removing from blackboard: ", Request, " with details ", Details_string, Log_entry);
       log("MANAGEMENT", Task_name, Log_entry);
       removeFromBlackboard(Request, Details).

// ---------------------------------------------------------------------------------------------------------------------
// TASK NOTE MANAGEMENT PLANS
// ---------------------------------------------------------------------------------------------------------------------
// Simulate time to take a note if there is no time required:
+!note_taking_time : not note_taking_time(_) | note_taking_time(0).

// Simulate time to take a note if there is time required:
+!note_taking_time : note_taking_time(Note_taking_time) & Note_taking_time > 0
   <-  await(Note_taking_time).

// Simulate the actual updating of a task note:
+!update_note_on_task(Task_name, Note_details)
    <- ?simulation_time(Start_time);
       !note_taking_time;
       .term2string(Note_details, Details_string);
       .concat("Updating note on task: ", Details_string, Log_entry);
       !update_task_property(Task_name, "task_note", Note_details);
       log("MANAGEMENT", Task_name, Log_entry, "SUCCESS", Start_time, -1).

// Simulate the removal of a task note:
+!remove_note_on_task(Task_name, Note_details)
    <- ?simulation_time(Start_time);
       !note_taking_time;
       .term2string(Note_details, Details_string);
       .concat("Removing note on task: ", Details_string, Log_entry);
       !remove_task_property(Task_name, "task_note", Note_details);
       log("MANAGEMENT", Task_name, Log_entry, "SUCCESS", Start_time, -1).


// ---------------------------------------------------------------------------------------------------------------------
// TASK FOCUSING
// ---------------------------------------------------------------------------------------------------------------------
// If task already in focus - do nothing:
+!refocus_agent(Task_name)
    :  .my_name(Agent_name)
    &  .term2string(Agent_name, Agent_name_string)
    &  task_in_focus(Agent_name_string, Task_name).

// If another task currently in focus: stop focusing now
@refocus_agent1 [atomic]
+!refocus_agent(Task_name)
    :  .my_name(Agent_name)
    &  .term2string(Agent_name, Agent_name_string)
    &  task_in_focus(Agent_name_string, Task_in_focus)
    &  not Task_in_focus == Task_name
    <- !stop_focus(Task_in_focus);
       !refocus_agent(Task_name).

// If no task in focus, select new task to focus on:
@refocus_agent2 [atomic]
+!refocus_agent(Task_name)
    <- ?refocusing_time(Seconds);
       await(Seconds);
       lookupArtifact(Task_name, Task_id);
       focus(Task_id);
       .my_name(Agent_name);
       !add_note_to_blackboard(Task_name, "task_in_focus", [Agent_name, Task_name]).

// In case there is an attempt to focus on a task that lacks an artefact - controlled failure:
-!refocus_agent(Task_name) [error_msg("cartago.ArtifactNotAvailableException")]
    <- .my_name(Agent_name);
       !remove_note_from_blackboard(Task_name, "task_in_focus", [Agent_name, Task_name]);
       .fail(focus_fail("cartago.ArtifactNotAvailableException")).

// If no refocusing_time is set
+?refocusing_time(0).

// Using stopFocus directly in a plan must be avoided if it is not guaranteed that the task still exists:
+!stop_focus(Task_name)
    <- .my_name(Agent_name);
       !remove_note_from_blackboard(Task_name, "task_in_focus", [Agent_name, Task_name]);
       lookupArtifact(Task_name, Task_id);
       stopFocus(Task_id).

// Stop focusing on a task that has disappeared meanwhile is not required and creates a failure event:
-!stop_focus(Task_name) [error_msg(Error_message)]
    :  Error_message = "cartago.ArtifactNotAvailableException" | Error_message = "Artifact Not Available."
    <- .my_name(Agent_name);
       !remove_note_from_blackboard(Task_name, "task_in_focus", [Agent_name, Task_name]).


{ include("$jacamoJar/templates/common-cartago.asl") }