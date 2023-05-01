/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

/**
 * Enumeration with statuses a composite task can be in.
 */
enum TaskStatus {
    WAITING,
    OPEN,
    COMPLETED,
    FAILED,
    TIMEOUT,
    DISPOSED
}