/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 *
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

import cartago.Artifact;
import cartago.OPERATION;

/**
 * Absolutely simple blackboard implementation on top of CArtAgO, which makes blackboard entries
 * visible as observable properties. May be instantiated  as follows in the JaCaMo project file:
 *
 * <pre>{@code workspace tasks {
 *         artifact taskBoard: task.BlackboardArtifact
 * }}
 */
public class BlackboardArtifact extends Artifact {

    /** Add a new observable property to the blackboard. Ignore the request in case this already exists, as
     * CArtAgO would otherwise duplicate the entries.
     * @param requestType this becomes the observable property's name
     * @param details this becomes the observable property's value(s)
     */
    @OPERATION void addToBlackboard(String requestType, Object[] details) {
        if (getObsPropertyByTemplate(requestType, details) == null) {
            defineObsProperty(requestType, details);
        }
    }
    /** Remove an observable property from the blackboard. Ignore the request in case the property does not exist
     * @param requestType this is the observable property's name
     * @param details this is/these are the observable property's value(s)
     */
    @OPERATION void removeFromBlackboard(String requestType, Object[] details) {
        if (getObsPropertyByTemplate(requestType, details) != null) {
            removeObsPropertyByTemplate(requestType, details);
        }
    }

}