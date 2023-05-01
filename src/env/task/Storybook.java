/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class holding the "story" being told, i.e. the full contents of the simulation script with all its
 * composite tasks, required atomic tasks, and timeline.
 */
public class Storybook {
    private ArrayList<StorybookTask> storybookTasks;
    private Map<String, CompositeTaskDefinition> compositeTaskDefinitions;
    private List<AtomicTaskDefinition> atomicTaskDefinitions;
    static private final Logger logger = Logger.getLogger(TaskArtifact.class.getName());

    public CompositeTaskDefinition getCompositeTaskDefinition(String compositeTaskName) {
        return compositeTaskDefinitions.get(compositeTaskName);
    }

    /** Retrieve the full definition of an atomic task based on its Situation-Operation designation.
     * @param situationOperation Situation-Operation reference
     * @return definition of an atomic task - if found; null otherwise.
     */
    public AtomicTaskDefinition getAtomicTaskDefinition(SituationOperation situationOperation) {
        for (AtomicTaskDefinition atomicTaskDefinition : atomicTaskDefinitions) {
            if (situationOperation.equals(atomicTaskDefinition.getSituation(), atomicTaskDefinition.getOperation())) {
                return atomicTaskDefinition;
            }
        }
        // else - if no atomic task found with that Situation-Operation signature:
        logger.warning("Atomic task definition for situation '"+situationOperation.getSituation()
                               + (situationOperation.getOperation() == null ? "" :
                               "' operation '"+situationOperation.getOperation()) +"' not found!");
        return null;
    }

    /** Retrieve a list of all relevant atomic task definitions for a composite task.
     * @param compositeTaskName designation of composite task (descriptive string)
     * @return list of all atomic tasks that are part of that composite task; may be empty if the composite task
     * was not found in the simulation script (storybook)
     */
    public ArrayList<AtomicTaskDefinition> getAtomicTaskDefinitions(String compositeTaskName) {
        ArrayList<AtomicTaskDefinition> atomicTaskList = new ArrayList<>();

        if (compositeTaskDefinitions.get(compositeTaskName) == null) {
            logger.warning("Cannot find definition for composite task: "+compositeTaskName);
            return atomicTaskList;
        }

        for (SituationOperation situationOperation : compositeTaskDefinitions.get(compositeTaskName).getAtomicTasks())

            if (getAtomicTaskDefinition(situationOperation) != null) {
                atomicTaskList.add(getAtomicTaskDefinition(situationOperation));
            }

        return atomicTaskList;
    }

    public List<AtomicTaskDefinition> getAtomicTaskDefinitions() {
        return atomicTaskDefinitions;
    }

    public List<StorybookTask> getStorybookTasks() {
        return storybookTasks;
    }

    public void setCompositeTaskDefinitions(Map<String, CompositeTaskDefinition> compositeTaskDefinitions) {
        this.compositeTaskDefinitions = compositeTaskDefinitions;
    }

    public void setAtomicTaskDefinitions(List<AtomicTaskDefinition> atomicTaskDefinitions) {
        this.atomicTaskDefinitions = atomicTaskDefinitions;
    }

    public void setStorybookTasks(ArrayList<StorybookTask> storybookTasks) {
        this.storybookTasks = storybookTasks;
    }

    /** Remove a number of storybook tasks from the overall storybook, useful once instantiated.
     * @param storybookTasks list of storybook tasks to be removed from the overall storybook.
     */
    public void removeStorybookTask(ArrayList<StorybookTask> storybookTasks) {
        for (StorybookTask storybookTask : storybookTasks)
            this.storybookTasks.remove(storybookTask);
    }

    /**
     * Sort all tasks in a simulation script along their start time.
     */
    public void sortStorybookTasks() {
        Comparator<StorybookTask> comp = Comparator.comparing(StorybookTask::getStartTime);

        storybookTasks.sort(comp);
    }
}
