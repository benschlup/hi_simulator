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
