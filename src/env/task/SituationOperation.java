/**
 * Code part of hi_simulator, a specification-driven task environment to simulate Hybrid Intelligent Systems
 * on the basis of JaCaMo.
 * <p>
 * Benjamin Schlup, Student ID 200050007
 * (ben.schlup@schlup.com
 */
package task;

/**
 * Class holding a situation-operation tuple and providing a comparison method.
 */
public class SituationOperation {
    private String situation;
    private String operation;


    /**
     * Constructor required for snakeyaml.
     */
    public SituationOperation() {}

    /** Constructor that also initialised the two object properties
     * @param situation Situation as a descriptive string
     * @param operation Operation as a descriptive string
     */
    public SituationOperation(String situation, String operation) {
        this.situation = situation;
        this.operation = operation;
    }

    public String getSituation() {
        return situation;
    }

    public void setSituation(String situation) {
        this.situation = situation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    /** Compare an object's situation/operation tuple with another situation/operation.
     * @param situation Situation to compare with
     * @param operation Operation to compare with, whereas Operation may be null for plain situation transitions
     * @return true in case situation and operation match with the object's situation/operation.
     */
    public boolean equals(String situation, String operation) {
        if (this.operation != null) {
            return this.situation.equals(situation) && this.operation.equals(operation);
        } else {
            return this.situation.equals(situation) && operation == null;
        }
    }
}
