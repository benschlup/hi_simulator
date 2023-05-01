// Monitoring agent in project hi_simulator
// Benjamin Schlup, Student ID 200050007
// (ben.schlup@schlup.com)
//
// The monitoring agent is a purely passive part in the MAS that tracks and logs
// changes on the blackboard that is used for inter-agent communication.
//
// =============================================================================
// AGENT-SPECIFIC START GOAL
// =============================================================================
// Log the current level of requests upon start of the agent:
+!start
    <- !log_blackboard_count(ha_help_required(_,_), "HA assistance requests");
       !log_blackboard_count(csa_teaching_required(_,_,_), "CSA teaching requests").

// =============================================================================
// AGENT-SPECIFIC SIGNAL HANDLING
// =============================================================================
// Note any changes in requests for human assistance:
+ha_help_required(_,_)
    <- !log_blackboard_count(ha_help_required(_,_), "HA assistance requests").

-ha_help_required(_,_)
    <- !log_blackboard_count(ha_help_required(_,_), "HA assistance requests").

// Note any changes in requests for teaching:
+csa_teaching_required(_,_,_)
    <- !log_blackboard_count(csa_teaching_required(_,_,_), "CSA teaching requests").

-csa_teaching_required(_,_,_)
    <- !log_blackboard_count(csa_teaching_required(_,_,_), "CSA teaching requests").

// =============================================================================
// AGENT-SPECIFIC PLANS
// =============================================================================
// Count and log the number of current blackboard entries of a specific request:
+!log_blackboard_count(Request, Log_string)
    <- .concat("Number of ", Log_string, Activity);
       .count(Request, Request_count);
       .term2string(Request_count, Request_count_string);
       log("MONITORING", "", Activity, Request_count_string).


{ include ("inc/common_capabilities.asl") }
