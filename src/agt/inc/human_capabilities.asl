// Shared code for simulated human agents that are part of a Hybrid Intelligent System.
// Benjamin Schlup, Student ID 200050007
//
// =============================================================================
// HANDING TASKS BACK TO COMPUTATIONAL AGENTS
// =============================================================================
// Attempt to hand back all tasks to a CA who requested an HA to assist:
+!handback_if_possible(CA_name)
    <- .my_name(Agent_name);
       .findall(Task_name, task_in_focus(Agent_with_focus, Task_name) & not Agent_with_focus == Agent_name, Tasks_in_focus);
       .findall(Task_name, ha_help_required(CA_name, Task_name) & not .member(Task_name, Tasks_in_focus), Task_list);
       !handback_if_possible(CA_name, Task_list).

// Process a list of tasks to be handed back to a CA:
+!handback_if_possible(_,[]).
+!handback_if_possible(CA_name, [Task_name | Other_tasks])
    <- !handback_if_possible(CA_name, Task_name, "OPEN");
       !handback_if_possible(CA_name, Other_tasks).

// Do not attempt to handback tasks that have non-OPEN status:
+!handback_if_possible(CA_name, Task_name, Task_status)
    :  not Task_status == "OPEN".

// Ask the CA to reevaluate whether he has now knowledge to handle a task
+!handback_if_possible(CA_name, Task_name, "OPEN")
    <- ?simulation_speed(Simulation_speed);
       .max([1000 / Simulation_speed, 100], Wait_time);
       .send(CA_name,askOne,reevaluation(Situations, Task_name), Reevaluation, Wait_time);
       !handback_if_possible(CA_name, Task_name, "OPEN", Reevaluation).

// The CA still has no appropriate knowledge how to handle the task, thus continue
+!handback_if_possible(CA_name, Task_name, "OPEN", Reevaluation)
    :   Reevaluation == reevaluation([], Task_name)[source(csa)]
    |   Reevaluation == timeout
    <-  log("MANAGEMENT", Task_name, "CA not able to take task back").

// The CA has now recognised one or multiple situations he's able to handle, thus hand back task:
+!handback_if_possible(CA_name, Task_name, "OPEN", Reevaluation)
    <- !remove_note_from_blackboard(Task_name, "ha_help_required", [CA_name, Task_name]);
       log("MANAGEMENT", Task_name, "Handed over task back to CA").


// =============================================================================
// STANDARD PLANS FOR TEACHING
// =============================================================================
// Teach a specific CA about a task we are ourselves not knowledgeable about:
+!teach(CA_name, Situation, Operation)
    :  not domain_teaching_knowledge(Situation, Operation, Teaching_time)
    <- ?agent_type(Agent_type);
       getDomainTaskKnowledge(Agent_type, Situation, Operation,
       	                  Must_triggers, Must_not_triggers, Eval_config, Exec_config);
       !learn_domain_knowledge(Situation, Operation, Must_triggers,
                         Must_not_triggers, Eval_config, Exec_config);
       !teach(CA_name, Situation, Operation).

// Teach a specific CA about a task we have knowledge about, and we're able to teach:
+!teach(CA_name, Situation, Operation)
    :  domain_teaching_knowledge(Situation, Operation, Teaching_time)
    &  Teaching_time >= 0
    <- ?simulation_time(Start_time);
       .concat("Initiated teaching of '",Situation,"'/'",Operation,"'",Init_log_message);
       log("MANAGEMENT", Init_log_message);
       await(Teaching_time);
       getDomainTaskKnowledge("CA", Situation,Operation,
	                  Must_triggers, Must_not_triggers, Eval_config, Exec_config);
       .send(CA_name, achieve,
             update_domain_knowledge(Situation, Operation, Must_triggers, Must_not_triggers, Eval_config, Exec_config));
       .concat("Teaching of '",Situation,"'/'",Operation,"'",Completion_log_message);
       log("MANAGEMENT", "", Completion_log_message, "SUCCESS", Start_time, -1).

// Log the cases when we are unable to teach a CA:
+!teach(CA_name, Situation, Operation)
    <- .concat("Unable to teach ", CA_name, " new situation (", Situation, ")", Log_entry);
       log(Log_entry).

@dedicated_teaching [atomic]
+!dedicated_teaching(CA_name, Situation, Operation)
    <- ?refocusing_time(Seconds);
       ?simulation_speed(Simulation_speed);
       .wait(Seconds * 1000 / Simulation_speed);
       !teach(CA_name, Situation, Operation);
       !remove_note_from_blackboard(csa_teaching_required,[CA_name, Situation, Operation]);
       !handback_if_possible(CA_name).

{ include ("inc/common_capabilities.asl") }
