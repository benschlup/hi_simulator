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
import jacamo.infra.JaCaMoLauncher;
import jacamo.platform.Cartago;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Class for managing a task set as per specification in the simulation script.
 */
public class TaskSetArtifact extends Artifact {
    static Logger logger = Logger.getLogger(Cartago.class.getName());
    private Storybook storybook = null;
//    private Workspace taskWorkspace;

    /**
     * Calculate the evaluation quality for a simulated evaluation based on an agent's profile
     * @param agentActionConfig configured agent capability
     * @param cycles experience cycles as maintained by an agent
     * @return estimated quality of the evaluation
     */
    static private double calculateQuality(AgentActionConfig agentActionConfig, int cycles) {

        return 1 - Math.random() * (1 - agentActionConfig.getInitialQuality()
                + (agentActionConfig.getOptimalQuality() - agentActionConfig.getInitialQuality())
                * Math.max(agentActionConfig.getLearningCycles() - cycles, 0)
                / Math.max(1, agentActionConfig.getLearningCycles()));

    }

    /**
     * Calculate the time required for a simulated evaluation of a situation
     * @param agentActionConfig  configured agent capability
     * @param cycles experience cycles as maintained by the agent
     * @return (simulation) time required to perform the evaluation
     */
    static private long calculateTime(AgentActionConfig agentActionConfig, int cycles) {

        return 1000 * (agentActionConfig.getInitialTime()
                - (long) (agentActionConfig.getInitialTime() - agentActionConfig.getMinimumTime())
                * Math.min(cycles, agentActionConfig.getLearningCycles())
                / Math.max(1, agentActionConfig.getLearningCycles()));
    }

    /**
     * Initialisation of a task set. Usually, a single instance is instantiated in the JaCaMo project file like this:
     * <p>{@code
     * artifact taskSet: task.TaskSetArtifact("cfg\\simulation_script.yaml", "task_")
     * }
     * @param fileName filename, optionally including path, to the simulation script with storybook and tasks.
     * @param artefactPrefix prefix to be used when creating artefact names
     */
    void init(String fileName, String artefactPrefix) {
        defineObsProperty("fileName", fileName);
        defineObsProperty("artefactPrefix", artefactPrefix.equals("") ? "task_" : artefactPrefix);
        loadTasks();
        execInternalOp("taskSetUpdate");
    }

    /**
     * Alternative initialisation with a default task artefact prefix.
     * @param fileName filename, optionally including path, to the simulation script with storybook and tasks
     */
    void init(String fileName) {
        init(fileName, "task_");
    }

    /**
     * Periodical operation that instantiates new task artefacts in CArtAgO when the start time has been reached.
     */
    @INTERNAL_OPERATION
    void taskSetUpdate() {
        int Task_id = 0;

        while (storybook.getStorybookTasks().size() > 0) {

            ArrayList<StorybookTask> openedTasks = new ArrayList<>();

            // Note that the following could be much more efficient by not walking through full list of tasks;
            // that could be implemented by initial sort by start time, removing completed tasks, and a while loop

            for (StorybookTask storybookTask : storybook.getStorybookTasks()) {

                // as the storybook is ordered by start time, we can stop looping once reaching
                // tasks beyond current simulation time
                if (storybookTask.getStartTime() > TimerArtifact.getSimulationTime()) {
                    break;
                }
                Task_id++;
                try {
                    String taskName = String.format("%s%04d", getObsProperty("artefactPrefix").stringValue(), Task_id);
                    makeArtifact(taskName, "task.TaskArtifact",
                                 new ArtifactConfig(storybookTask.getCompositeTask(),
                                                    storybook.getCompositeTaskDefinition(storybookTask.getCompositeTask()),
                                                    storybook.getAtomicTaskDefinitions(storybookTask.getCompositeTask())));

                    // Let listeners know that a task got created
                    signal("task_created", taskName);
                    signal("tick");

                } catch (OperationException e) {
                    throw new RuntimeException(e);
                }
                // Take a note of created tasks so they can be removed from the storybook
                openedTasks.add(storybookTask);
            }

            // Remove created tasks from storybook outside the loop, as this otherwise is a concurrent modification
            storybook.removeStorybookTask(openedTasks);

            await_time(800 / TimerArtifact.getSimulationSpeed());
        }

        logger.info("No further tasks in the storybook: Waiting for tasks to be completed.");
        execInternalOp("waitForTasksToComplete");
    }

    /**
     * Load the simulation script consisting of a storybook (timeline of tasks), composite and atomic task definitions.
     */
    @OPERATION
    void loadTasks() {
        Constructor constructor = new Constructor(Storybook.class);
        TypeDescription storybookDesc = new TypeDescription(Storybook.class);
        storybookDesc.addPropertyParameters("storybookTasks", StorybookTask.class);
        storybookDesc.addPropertyParameters("compositeTasks", CompositeTaskDefinition.class);
        storybookDesc.addPropertyParameters("atomicTasks", AtomicTaskDefinition.class);
        constructor.addTypeDescription(storybookDesc);
        Yaml yaml = new Yaml(constructor);
        FileInputStream inputStream;

        try {
            inputStream = new FileInputStream(getObsProperty("fileName").stringValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        storybook = yaml.load(inputStream);
        storybook.sortStorybookTasks();

        LoggerArtifact.env_log(this.getClass().getName(), storybook.getStorybookTasks().size() + " tasks loaded");
    }

    /**
     * Wait for disappearance of all task.TaskArtifacts: this indicated that the simulation has ended.
     */
    @INTERNAL_OPERATION
    void waitForTasksToComplete()  {
        String workspacePath = this.getId().getWorkspaceId().getFullName();
        Workspace taskWorkspace;

        try {
            taskWorkspace =  CartagoEnvironment.getInstance().resolveWSP(workspacePath).getWorkspace();
        } catch (WorkspaceNotFoundException e) {
            return; // if the workspace disappears (for a strange reason), there are no tasks surviving anyway
        }

        outerloop:
        while (true) {
            await_time(1000);

            for (ArtifactId artifactId : taskWorkspace.getArtifactIdList()) {
                try {
                    if (artifactId.getArtifactType().equals("task.TaskArtifact")) {
                        continue outerloop;
                    }
                } catch (Exception e) {
                    // no need for logging the exception: this may happen if a task artefact disappears during evaluation
                }
            }
            break;
        }

        LoggerArtifact.flushBuffers();

        // Keep JaCaMo running for another 2 minutes to allow taking screenshots of GUI
        JaCaMoLauncher.getJaCaMoRunner().finish(120000, true, 0);
    }

    /** Provide agents with a list of situation-operation tuples that should be known from the beginning, to
     * support building up initial belief base.
     * @param agentType "HA" or "CA"
     * @param situationOperations return value for array of objects with situation-operation tuples
     */
    @OPERATION
    void initialKnowledge(String agentType, OpFeedbackParam<Object[]> situationOperations) {

        ArrayList<String[]> initialKnowledge = new ArrayList<>();

        for (AtomicTaskDefinition atomicTask : storybook.getAtomicTaskDefinitions()) {
            if ((agentType.equals("HA") && atomicTask.getHaConfig().getInitialKnowledge()) ||
                    (agentType.equals("CA") && atomicTask.getCaConfig().getInitialKnowledge())) {
                initialKnowledge.add(atomicTask.getSituationOperation());
            }
        }

        situationOperations.set(initialKnowledge.toArray());
    }

    /** Also the evaluation of situations requires skill and time. This operation allows agents to simulate the
     * time required and quality resulting from evaluating a specific situation.
     * @param situation situation that is being considered
     * @param operation operation (completing the situation-operation tuple), needed to lookup atomic task -> agent capabilities
     * @param agentType "HA" or "CA"
     * @param cycles number of successful past evaluations, as an indicator for experience in handling this situation
     * @param evalQuality feedback parameter with an estimated quality of the evaluation performed
     */
    @OPERATION
    void simulateEvaluation(String situation, String operation, String agentType, int cycles, OpFeedbackParam<Double> evalQuality) {

        AtomicTaskDefinition atomicTask = storybook.getAtomicTaskDefinition(new SituationOperation(situation, operation));

        if (atomicTask == null) {
            LoggerArtifact.env_log(this.getClass().getName(), this.getCurrentOpAgentId().getAgentName() + " requested simulated evaluation of unknown situation '" + situation + "' operation '" + operation + "'");
            failed("Unknown atomic task for situation '" + situation + "' operation '" + operation + "'");
        } else {

            AgentActionConfig agentActionConfig;

            if (agentType.equals("CA")) {
                agentActionConfig = atomicTask.getCaConfig().getEvaluation();
            } else {
                agentActionConfig = atomicTask.getHaConfig().getEvaluation();
            }

            if (agentActionConfig.getInitialTime() < 0) {
                LoggerArtifact.env_log(this.getClass().getName(),
                                       "Agent type " + agentType + " unable to evaluate this situation/operation");
                failed("Agent type unable to evaluate this situation/operation");
            } else {
                evalQuality.set(calculateQuality(agentActionConfig, cycles));
                await_time(calculateTime(agentActionConfig, cycles) / TimerArtifact.getSimulationSpeed());
            }
        }
    }

    /** Helper operation that allows an agent to unpack error details embedded in a failure error message
     * @param errMessage error message as gotten from error_msg(A)
     * @param errDetails return of result with array of objects containing the error detail contents
     */
    @OPERATION
    void extractErrorDetails(String errMessage, OpFeedbackParam<Object[]> errDetails) {
        ArrayList<Object> errorAttributes = new ArrayList<>();

        for (String errDetail : errMessage.substring(errMessage.indexOf(':') + 1).split(",")) {
            try {
                double d = Double.parseDouble(errDetail);
                errorAttributes.add(d);
            } catch (NumberFormatException nfe) {
                errorAttributes.add(errDetail);
            }
        }

        errDetails.set(errorAttributes.toArray());
    }

    /** Obtain knowledge on how to handle a task; to be used for simulating learning.
     * @param agentType "HA" or "CA"
     * @param situation situation that needs to be addressed
     * @param operation operation that should be executed
     * @param mustTriggers preconditions that must be met to qualify such a situation
     * @param mustNotTriggers task attributes that should not be set to qualify such a situation
     * @param evaluation agent capabilities related to evaluating such situations
     * @param execution agent capabilities related to executing such situations
     */
    @OPERATION
    void getDomainTaskKnowledge(String agentType,
                                String situation,
                                String operation,
                                OpFeedbackParam<Object[]> mustTriggers,
                                OpFeedbackParam<Object[]> mustNotTriggers,
                                OpFeedbackParam<Object[]> evaluation,
                                OpFeedbackParam<Object[]> execution) {


        AtomicTaskDefinition atomicTask = storybook.getAtomicTaskDefinition(new SituationOperation(situation, operation));

        ArrayList<String> mTriggers = new ArrayList<>();
        ArrayList<String> mnTriggers = new ArrayList<>();

        for (String condition : atomicTask.getPreConditions()) {
            if (condition.charAt(0) == '!') {
                mnTriggers.add(condition.substring(1).trim());
            } else {
                mTriggers.add(condition.trim());
            }
        }

        mustTriggers.set(mTriggers.toArray());
        mustNotTriggers.set(mnTriggers.toArray());

        if (agentType.equals("CA")) {
            evaluation.set(atomicTask.getCaConfig().getEvaluation().getConfig());
            execution.set(atomicTask.getCaConfig().getExecution().getConfig());
        } else if (agentType.equals("HA")) {
            evaluation.set(atomicTask.getHaConfig().getEvaluation().getConfig());
            execution.set(atomicTask.getHaConfig().getExecution().getConfig());
        } else {
            logger.warning("Specified agent type is neither HA nor CA?");
            failed("Specified agent type is neither HA nor CA?");
        }
    }

}