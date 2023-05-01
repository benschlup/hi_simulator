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
 * Class holding the definition of an atomic task.
 */
public class AtomicTaskDefinition {
    private String situation;
    private String operation;
    private Integer timeout = 0;
    private Double minimumQuality = 0.0;
    private Integer maximumRetries = 0;

    private List<String> preConditions = null;
    private List<String> postConditions = null;

    private AgentConfig caConfig = new AgentConfig();
    private AgentConfig haConfig = new AgentConfig();

    public String getOperation() {
        return operation;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public List<String> getPreConditions() {
        return preConditions;
    }

    public void setPreConditions(List<String> preConditions) {
        this.preConditions = preConditions;
    }

    public List<String> getPostConditions() {
        return postConditions;
    }

    public void setPostConditions(List<String> postConditions) {
        this.postConditions = postConditions;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public AgentConfig getCaConfig() {
        return caConfig;
    }

    public void setCaConfig(AgentConfig caConfig) {
        this.caConfig = caConfig;
    }

    public String getSituation() {
        return situation;
    }

    public void setSituation(String situation) {
        this.situation = situation;
    }

    public AgentConfig getHaConfig() {
        return haConfig;
    }

    public void setHaConfig(AgentConfig haConfig) {
        this.haConfig = haConfig;
    }

    public Double getMinimumQuality() {
        return minimumQuality;
    }

    public void setMinimumQuality(Double minimumQuality) {
        this.minimumQuality = minimumQuality;
    }

    public Integer getMaximumRetries() {
        return maximumRetries;
    }

    public void setMaximumRetries(Integer maximumRetries) {
        this.maximumRetries = maximumRetries;
    }

    public String[] getSituationOperation() { return new String[]{situation, operation}; }

}
