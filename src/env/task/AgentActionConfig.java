/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */

package task;

/**
 * Class for maintaining an agent's specific capabilities with respect to evaluating or executing an atomic task.
 */
public class AgentActionConfig
{                                          // by default:
    private Integer initialTime = -1;      // agent is unable to perform this atomic task
    private Integer minimumTime = -1;      // dito
    private Double initialQuality = 1.0;   // does deliver perfect quality
    private Double optimalQuality = 1.0;   // cannot improve quality
    private Integer learningCycles = 0;    // does not improve over time
    private Integer learningTime = -1;     // is unable to learn this atomic task by itself
    private Integer teachingTime = -1;     // agent cannot teach another agent-type this task

    public Integer getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(Integer initialTime) {
        this.initialTime = initialTime;
    }

    public Integer getMinimumTime() { return minimumTime < 0 ? initialTime : minimumTime; }

    public void setMinimumTime(Integer minimumTime) {
        this.minimumTime = minimumTime;
    }

    public Double getInitialQuality() {
        return initialQuality;
    }

    public void setInitialQuality(Double initialQuality) {
        this.initialQuality = initialQuality;
    }

    public Double getOptimalQuality() { return optimalQuality < 0 ? initialQuality : optimalQuality; }

    public void setOptimalQuality(Double optimalQuality) {
        this.optimalQuality = optimalQuality;
    }

    public Integer getLearningCycles() {
        return learningCycles;
    }

    public void setLearningCycles(Integer learningCycles) {
        this.learningCycles = learningCycles;
    }

    public Integer getTeachingTime() {
        return teachingTime;
    }

    public void setTeachingTime(Integer teachingTime) {
        this.teachingTime = teachingTime;
    }

    public Object[] getConfig() {
        return new Object[]{initialTime, minimumTime, initialQuality, optimalQuality, learningCycles, learningTime, teachingTime};
    }

    public void setLearningTime(Integer learningTime) { this.learningTime = learningTime; }

    public Integer getLearningTime(Integer learningTime) { return learningTime; }
}
