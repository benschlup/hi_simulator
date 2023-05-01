package control;

import cartago.*;
import jacamo.platform.Cartago;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;

/**
 * The simulation timer artefacts provides other artefacts and agents with a simulated time that
 * supports arbitrary starting points in a real world timeline and configurable timelapse.
 * <p>Instantiation including configuration is done in a JaCaMo project file like this:
 * <p>{@code
 * artifact timer: simulationtimer.TimerArtifact("2023-01-03T07:00:00.00Z",2)
 * }
 * <p>Whereas the first parameter is the real world start date/time in ISO8601 format and the second parameter is
 * the simulation speed factor (integer).
 * <p>Note that there may only be a single instance of that artefact in any given simulation project (singleton).
 */
public class TimerArtifact extends Artifact {
    private static final Logger logger = Logger.getLogger(Cartago.class.getName());
    private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static long realWorldStart;
    private static long simulationWorldStart;
    private static int simulationSpeed;
    private static ArtifactId timerArtifactId = null;

    /**
     * Provide direct access to current simulation time for other artefacts. This enables higher efficiency
     * than calling an operation through the CArtAgO infrastructure. Due to the static and deterministic
     * nature of this method, this is non-problematic.
     *
     * @return simulation time in milliseconds
     */
    static public long getSimulationTime() {
        awaitConfiguration();
        return (currentTimeMillis() - realWorldStart) * simulationSpeed + simulationWorldStart;
    }

    /**
     * Provide direct access to current simulation speed for other artefacts. This enables higher efficiency
     * than calling an operation through the CArtAgO infrastructure. Due to the static and deterministic
     * nature of this method, this is non-problematic.
     *
     * @return simulation speed as a factor of regular time flow
     */
    static public int getSimulationSpeed() {
        awaitConfiguration();
        return TimerArtifact.simulationSpeed;
    }

    /**
     * Make sure that there is a properly configured timer artefact before other methods are made available.
     */
    static private void awaitConfiguration() {
        while (timerArtifactId == null) {
            logger.info("Waiting for instantiation of timer artifact.");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Configure the timer artefact singleton when the JaCaMo project instantiates it.
     *
     * @param simulationWorldStartISO8601 ISO8601 formatted starting point in real world time
     * @param simulationSpeed             as a factor of real world time flow
     * @throws ArtifactAlreadyPresentException if multiple timers configured for a JaCaMo project
     */
    void init(String simulationWorldStartISO8601, int simulationSpeed) throws ArtifactAlreadyPresentException {

        // Make sure we have only one timer artefact running
        if (timerArtifactId != null) {
            logger.warning("Multiple instances of simulation timer not allowed (timer must be singleton).");
            throw new ArtifactAlreadyPresentException("TimerArtifact", "unknown workspace");
        }

        // Initialise timer artefact
        logger.info("Initializing simulation timer artifact.");
        realWorldStart = currentTimeMillis();
        simulationWorldStart = Instant.parse(simulationWorldStartISO8601).toEpochMilli();
        TimerArtifact.simulationSpeed = simulationSpeed;

        // Make observable properties available
        defineObsProperty("simulationworld_start_ISO8601", iso8601.format(simulationWorldStart));
        defineObsProperty("realworld_start_ISO8601", iso8601.format(realWorldStart));
        defineObsProperty("simulation_time_ISO8601", iso8601.format(simulationWorldStart));
        defineObsProperty("simulation_time", simulationWorldStart);
        defineObsProperty("simulation_speed", simulationSpeed);

        // Initiate regular updating of observable current date/time property
        execInternalOp("publishTime");

        // Take note of the newly created timer artefact: As there are never multiple threads initialising a
        // CArtAgO artefact, this is adequate for guaranteeing that this remains a singleton.
        timerArtifactId = this.getId();
    }

    /**
     * CArtAgO-internal operation that never ends and regularly updates the observable simulation time:
     * To be called only once when the timer artefact is initialised.
     */
    @INTERNAL_OPERATION
    void publishTime() {
        while (true) {
            // Update the observable simulation time in ISO format
            getObsProperty("simulation_time_ISO8601").updateValue(iso8601.format(getSimulationTime()));
            getObsProperty("simulation_time").updateValue(getSimulationTime());

            // Make sure the updates happen quicker and at least once per simulated second:
            await_time(800 / getSimulationSpeed());
        }
    }

    /** Simulate the waiting of an agent for a specified amount of time.
     * @param simulatedSeconds (Simulated) seconds to keep an agent waiting.
     */
    @OPERATION
    void await(int simulatedSeconds) {
        await_time(simulatedSeconds * 1000L / getSimulationSpeed());
    }
}

