/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */

package task;

/**
 * Class holding the specification of an agent's capabilities with respect to an atomic task.
 */
public class AgentConfig {
    private Boolean initialKnowledge = Boolean.FALSE;
    private AgentActionConfig evaluation = new AgentActionConfig();
    private AgentActionConfig execution = new AgentActionConfig();


    public AgentActionConfig getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(AgentActionConfig evaluation) {
        this.evaluation = evaluation;
    }

    public AgentActionConfig getExecution() {
        return execution;
    }

    public void setExecution(AgentActionConfig execution) {
        this.execution = execution;
    }

    public Boolean getInitialKnowledge() {
        return initialKnowledge;
    }

    public void setInitialKnowledge(Boolean initialKnowledge) {
        this.initialKnowledge = initialKnowledge;
    }
}
