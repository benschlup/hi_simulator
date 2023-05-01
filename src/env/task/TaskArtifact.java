/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 * <p>
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

import cartago.*;
import control.LoggerArtifact;
import control.TimerArtifact;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class representing a core artefact in the task environment: an instance of a composite task.
 */
public class TaskArtifact extends Artifact {
    static private final Logger logger = Logger.getLogger(TaskArtifact.class.getName());
    private final long startTime = TimerArtifact.getSimulationTime();
    private final ArrayList<AtomicTaskInstance> activeAtomicTasks = new ArrayList<>();
    private TaskStatus status = TaskStatus.OPEN;
    private List<AtomicTaskDefinition> atomicTasks;
    private String compositeTask;
    private Double totalQuality = 1.0;

    /**
     * Initialise the newly created task artefact:
     *
     * @param compositeTask           Human-readable name of task taken from simulation script
     * @param compositeTaskDefinition Composite task properties
     * @param atomicTasks             List of atomic tasks that are part of this composite task
     */
    void init(String compositeTask, CompositeTaskDefinition compositeTaskDefinition, List<AtomicTaskDefinition> atomicTasks) {
        this.compositeTask = compositeTask;
        this.atomicTasks = atomicTasks;

        LoggerArtifact.env_log(getId().getName(), this.getClass().getName(), "New task: " + compositeTask);

        // Make sure we activate initial tasks which do not require any preconditions to be met:
        execInternalOp("reviewTasks");

        // Start the loop for monitoring the timeout
        execInternalOp("timerLoop", compositeTaskDefinition.getTimeout());
    }

    /**
     * Log disposal of a task artefact
     */

    @Override
    protected void dispose() {

        if (status == TaskStatus.OPEN) {
            status = TaskStatus.DISPOSED;
        }

        LoggerArtifact.env_log(getId().getName(),
                               this.getClass().getName(),
                               "Disposed: " + compositeTask,
                               startTime,
                               String.valueOf(status),
                               totalQuality);
    }

    /**
     * Dispose the task artefact and ignore any exception, as these may be caused by concurrent threads attempting
     * to dispose the same task at the same time - which is not an issue, as the disposal is anyway final.
     */
    protected void disposeTask() {
        try {
            dispose(getId());
        } catch (OperationException e) {
            // Can safely be ignored, as this happens only when
            // the task artefact was disposed before, e.g. through
            // concurrent activities
        }
    }


    /**
     * Safely remove a task property by checking first whether it exists.
     *
     * @param taskProperty Name of the task property to be removed
     */
    private void safeRemoveObsProperty(String taskProperty) {
        if (getObsPropertyByTemplate("task_property", taskProperty.trim()) != null) {
            removeObsPropertyByTemplate("task_property", taskProperty.trim());
        }
    }

    /**
     * Safely add a task property by checking first whether it exists.
     *
     * @param taskProperty Name of the task property to be added
     */
    private void safeDefineTaskProperty(String taskProperty) {
        if (getObsPropertyByTemplate("task_property", taskProperty.trim()) == null) {
            defineObsProperty("task_property", taskProperty.trim());
        }
    }

    /**
     * Review total list of pending atomic tasks to activate the ones with matching preconditions
     */
    @INTERNAL_OPERATION
    @OPERATION
    synchronized void reviewTasks() {

        // Otherwise start the review of all atomic tasks
        List<AtomicTaskDefinition> activatedTasks = new ArrayList<>();
        boolean propertiesUpdated;

        do {
            propertiesUpdated = false;

            atomicTaskReview:
            for (AtomicTaskDefinition atomicTask : atomicTasks) {
                // Review potentially required preconditions before activating an atomic task
                if (atomicTask.getPreConditions() != null) {
                    for (String condition : atomicTask.getPreConditions()) {
                        if (condition.charAt(0) == '!') {
                            String property = condition.substring(1).trim();
                            if (getObsPropertyByTemplate("task_property", property) != null)
                                continue atomicTaskReview;
                        } else {
                            String property = condition.trim();
                            if (getObsPropertyByTemplate("task_property", property) == null)
                                continue atomicTaskReview;
                        }
                    }
                }

                // Now process the atomic task that did not disqualify during pre-conditions check
                LoggerArtifact.env_log(getId().getName(),
                                       this.getClass().getName(),
                                       "New situation: " + atomicTask.getSituation());

                // If it is a regular atomic task requiring an operation as a response, activate it
                if (atomicTask.getOperation() != null) {
                    activeAtomicTasks.add(new AtomicTaskInstance(atomicTask));
                }
                // If it is a pure situation-transition (i.e. pre-conditions leading to post-conditions),
                // just set the post-conditions and make sure another iteration through the task list is
                // performed as the task properties might now be different and trigger other atomic tasks:
                else {
                    setPostConditions(atomicTask);
                    propertiesUpdated = true;
                }
                // Let's take a note of the activated task for later removal from the list. This cannot
                // be done within the loop.
                activatedTasks.add(atomicTask);
            }
            atomicTasks.removeAll(activatedTasks);

        } while (propertiesUpdated);
    }

    /** Set all task properties according to the definitions in an atomic task.
     * @param atomicTask basis for setting post-condition task properties.
     */
    private void setPostConditions(AtomicTaskDefinition atomicTask) {
        if (atomicTask.getPostConditions() != null) {
            for (String postCondition : atomicTask.getPostConditions()) {
                switch (postCondition.charAt(0)) {
                    case '-' -> safeRemoveObsProperty(postCondition.substring(1));
                    case '+' -> safeDefineTaskProperty(postCondition.substring(1));
                    default -> logger.warning("postCondition lacking +/-, operation "
                                                      + atomicTask.getSituation());
                }
            }
        }
    }

    /** Watch timeouts of atomic tasks and the overall composite task.
     * @param overallTimeout overall composite task maximum running time in simulation-seconds
     */
    @INTERNAL_OPERATION
    void timerLoop(Integer overallTimeout) {

        while (status == TaskStatus.OPEN && atomicTasks.size() > 0) {

            // Check if any active atomic task has reached its defined timeout value
            for (AtomicTaskInstance atomicTaskInstance : activeAtomicTasks) {
                if (atomicTaskInstance.atomicTaskDefinition.getTimeout() > 0
                        && (TimerArtifact.getSimulationTime() - atomicTaskInstance.startTime) / 1000
                        >= atomicTaskInstance.atomicTaskDefinition.getTimeout()) {
                    LoggerArtifact.env_log(getId().getName(),
                                           this.getClass().getName(),
                                           "Situation timeout: " + atomicTaskInstance.atomicTaskDefinition.getSituation());
                    status = TaskStatus.TIMEOUT;
                    disposeTask();
                }
            }

            // Check if the composite task has reached its defined timeout value
            if (overallTimeout > 0 && (TimerArtifact.getSimulationTime() - startTime) / 1000 >= overallTimeout) {
                status = TaskStatus.TIMEOUT;
                disposeTask();
            } else {
                await_time(800 / TimerArtifact.getSimulationSpeed());
            }
        }

    }

    /** Provide an agent-accessible operation for simulation the execution of an action to move an atomic task ahead.
     * @param operation operation (descriptive string)
     * @param agentType "CA" for computational agents, "HA" for human agents
     * @param cycles experience of an agent measured in successful execution cycles
     * @param execQuality returns the calculated quality of an execution back to the agent
     * @param taskStatus returns the new status of the composite task back to the agent
     * @throws ArtifactNotAvailableException in case there are no pending atomic tasks
     */
    @OPERATION
    void executeArtifactOperation(String operation, String agentType, int cycles, OpFeedbackParam<Double> execQuality, OpFeedbackParam<String> taskStatus) throws ArtifactNotAvailableException {

        // There was an operation executed while the task is being disposed
        if (atomicTasks.size() == 0 && activeAtomicTasks.size() == 0) {
            throw new ArtifactNotAvailableException();
        }

        // The assumption is that open tasks remain open...
        taskStatus.set(String.valueOf(this.status));

        for (AtomicTaskInstance atomicTaskInstance : activeAtomicTasks) {
            AtomicTaskDefinition atomicTask = atomicTaskInstance.atomicTaskDefinition;

            if (atomicTask.getOperation().equals(operation)) {

                AgentActionConfig agentActionConfig;
                if (agentType.equals("CA")) {
                    agentActionConfig = atomicTask.getCaConfig().getExecution();
                } else {
                    agentActionConfig = atomicTask.getHaConfig().getExecution();
                }

                if (agentActionConfig.getInitialTime() < 0) {
                    LoggerArtifact.env_log(getId().getName(), this.getClass().getName(), operation,
                                           TimerArtifact.getSimulationTime(),
                                           "Agent type " + agentType + " unable to perform this atomic task");
                    failed("Agent type unable to perform this atomic task");
                }


                double qualityRange = (1 - agentActionConfig.getOptimalQuality())
                        + (agentActionConfig.getOptimalQuality() - agentActionConfig.getInitialQuality())
                        * Math.min(cycles, agentActionConfig.getLearningCycles())
                        / Math.max(1, agentActionConfig.getLearningCycles());

                double quality = 1 - Math.random() * qualityRange;

                execQuality.set(quality);
                totalQuality *= quality;

                long endTime = 1000 * (agentActionConfig.getInitialTime()
                        - (long) (agentActionConfig.getInitialTime() - agentActionConfig.getMinimumTime())
                        * Math.min(cycles, agentActionConfig.getLearningCycles())
                        / Math.max(1, agentActionConfig.getLearningCycles())) + TimerArtifact.getSimulationTime();

                while (endTime > TimerArtifact.getSimulationTime()) {
                    await_time(1000L / TimerArtifact.getSimulationSpeed());
                    if (status != TaskStatus.OPEN) {
                        taskStatus.set(String.valueOf(this.status));
                        failed(status.toString());
                    }
                }

                if (quality < atomicTask.getMinimumQuality()) {
                    if (atomicTaskInstance.retries < atomicTask.getMaximumRetries()) {
                        atomicTaskInstance.retries++;
                        failed(String.format("QUALITY ISSUE:%f,%f", quality, atomicTask.getMinimumQuality()));
                    } else {
                        this.status = TaskStatus.FAILED;
                        taskStatus.set(String.valueOf(this.status));
                        disposeTask();
                        failed(String.format("QUALITY ISSUE:%f,%f", quality, atomicTask.getMinimumQuality()));
                    }
                } else {
                    setPostConditions(atomicTask);
                    activeAtomicTasks.remove(atomicTaskInstance);
                    if (atomicTasks.size() == 0 && activeAtomicTasks.size() == 0) {
                        status = TaskStatus.COMPLETED;
                        taskStatus.set(String.valueOf(status));
                        disposeTask();
                    } else {
                        reviewTasks();
                    }
                }
                return;
            }
        }

        // If no operation has matched, fail
        execQuality.set(0.0);
        failed("Unexpected operation");
    }

    /** Allow intelligent agents to ask the composite task for any currently active atomic tasks: this is required
     * to simulate learning by an agent.
     * @param situationOperations list of Situation-Operation tuples referencing the currently active atomic tasks
     */
    @OPERATION
    void currentAtomicTasks(OpFeedbackParam<Object[]> situationOperations) {
        ArrayList<String[]> currentSituationOperations = new ArrayList<>();

        for (AtomicTaskInstance atomicTaskInstance : activeAtomicTasks) {
            currentSituationOperations.add(atomicTaskInstance.atomicTaskDefinition.getSituationOperation());
        }
        situationOperations.set(currentSituationOperations.toArray());
        if (currentSituationOperations.size() == 0) {
            failed("No atomic tasks pending");
        }
    }

    /** An agent may take notes with respect to a task; in extreme cases, the agent may modify task properties
     * that are maintained by the TaskArtifact otherwise (do that with care; with power comes responsibility!)
     * @param property artefact/task property to be removed
     * @param value value of the observable property
     */
    @OPERATION
    void updateTaskProperty(String property, Object[] value) {
        if (getObsPropertyByTemplate(property, value) == null) {
            defineObsProperty(property, value);
        }
    }

    /** An agent may remove task properties which would usually represent "notes". Modifying the "task_property"
     * properties themselves should be done with extreme care, as this would not automatically trigger a review
     * of the task. Agents may call reviewTask actively, but this may lead to considerably complex scenarios.
     * @param property artefact/task property to be removed
     * @param value value of the observable property
     */
    @OPERATION
    void removeTaskProperty(String property, Object[] value) {
        if (getObsPropertyByTemplate(property, value) != null) {
            removeObsPropertyByTemplate(property, value);
        }
    }

    /**
     * A helper class to track start time and retries of atomic tasks once activated.
     */
    private static class AtomicTaskInstance {
        final long startTime = TimerArtifact.getSimulationTime();
        int retries = 0;

        AtomicTaskDefinition atomicTaskDefinition;

        public AtomicTaskInstance(AtomicTaskDefinition atomicTaskDefinition) {
            this.atomicTaskDefinition = atomicTaskDefinition;
        }
    }

}
