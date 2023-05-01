/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

import java.util.Date;

/**
 * Class holding a storybook task from the simulation script: This is a task scheduled to start at a specific time,
 * referencing a well-defined composite task.
 */
public class StorybookTask {
    private String taskName;
    private Date startTimeISO8601;
    private String compositeTask;


    public long getStartTime() {
        return startTimeISO8601.toInstant().toEpochMilli();
    }

    public String getCompositeTask() {
        return compositeTask;
    }

    public Date getStartTimeISO8601() {
        return startTimeISO8601;
    }

    public void setStartTimeISO8601(Date startTimeISO8601) {
        this.startTimeISO8601 = startTimeISO8601;
    }

    public void setCompositeTask(String compositeTask) {
        this.compositeTask = compositeTask;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}