/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

import java.util.List;

/**
 * Class holding a composite task definition.
 */
public class CompositeTaskDefinition {
    private Integer timeout = 0;

    private List<SituationOperation> atomicTasks;

    public List<SituationOperation> getAtomicTasks()  {
        return atomicTasks;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setAtomicTasks(List<SituationOperation> atomicTasks) {
        this.atomicTasks = atomicTasks;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}