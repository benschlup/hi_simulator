package control;

import cartago.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * The logger artefact allows other artefacts and agents to add event information to the central CSV-file log.
 * Optionally, a caller may request displaying the log entries during simulation runs in separate windows.
 */
public class LoggerArtifact extends Artifact {

    static private final String[] csvFields = {"Entry_type", "Case_identifier", "Activity", "Timestamp", "Resource", "Start_time", "Duration", "Result", "Quality"};
    static private final String[] fieldFormats = {"%-11s", "%-11s", "%-40s", "%-19s", "%-16s", "%-19s", "%8s", "%-16s", "%6s"};
    static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("hi_simulator");
    static private final HashMap<String, Display> display = new HashMap<>();
    static private Display baseDisplay = null;
    static private PrintWriter logWriter = null;
    static private ArtifactId loggerArtifactId = null;

    /**
     * Logging operation available internally for use by other CArtAgO artefacts (as a linkable operation or
     * direct method call), and to agents.
     * @param entryType usually "MANAGEMENT", "DOMAIN" (for domain tasks), or "MONITORING"
     * @param caseIdentifier in case log entries reference a specific composite task, ID of that task
     * @param activity activity performed
     * @param resource resource that performed the activity,
     * @param startTime -1 if not known, otherwise milliseconds since epoch
     * @param result empty string if not relevant
     * @param quality percentage of quality for executed operations, otherwise a negative value
     */
    @LINK
    @INTERNAL_OPERATION
    @OPERATION
    static protected void log(String entryType, String caseIdentifier, String activity, String resource, long startTime, String result, double quality) {

        // Calculate time-related fields
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        long logTime = TimerArtifact.getSimulationTime();
        String durationString = startTime < 0 ? "" : String.format("%.3f", (logTime - startTime) / 1000.0);
        String endTimeISO = timeFormatter.format(logTime);
        String startTimeISO = startTime < 0 ? "" : timeFormatter.format(startTime);

        // Determine quality as string
        String qualityString = quality < 0 ? "" : String.format("%.03f", quality);

        String[] logEntry = {entryType, caseIdentifier, activity, endTimeISO, resource, startTimeISO, durationString, result, qualityString};

        // Output to display and file:
        if (baseDisplay != null && logWriter != null) {
            if (display.containsKey(resource)) {
                display.get(resource).addRow(logEntry);
            } else {
                baseDisplay.addRow(logEntry);
            }
            logEntry = logEntry.clone();
            for (int i = 0; i < logEntry.length; i++) {
                logEntry[i] = logEntry[i].replace("\"", "\"\"");
                if (logEntry[i].matches(".*[\",].*")) {
                    logEntry[i] = "\"" + logEntry[i] + "\"";
                }
            }
            logWriter.println(String.join(",", logEntry));

        } else {
            logger.warning("CALL TO STATIC LOGGING ARTIFACT THAT IS NOT YET READY.");
        }
    }

    /** Limited environmental log entry with just resource and activity.
     * @param resource should be class name of calling method
     * @param activity activity to be written to the log file
     */
    static public void env_log(String resource, String activity) {
        log("ENVIRONMENT", "", activity, resource, -1, "", -1);
    }

    /** Limited environmental log entry.
     * @param caseIdentifier Task name (string) a log entry should reference
     * @param resource should be class name of calling method
     * @param activity activity to be written to the log file
     */
    static public void env_log(String caseIdentifier, String resource, String activity) {
        log("ENVIRONMENT", caseIdentifier, activity, resource, -1, "", -1);
    }

    /** Limited environmental log entry.
     * @param caseIdentifier Task name (string) a log entry should reference
     * @param resource should be class name of calling method
     * @param activity activity to be written to the log file
     * @param startTime start time for log entries that reference a duration
     * @param result result
     */
    static public void env_log(String caseIdentifier, String resource, String activity, long startTime, String result) {
        log("ENVIRONMENT", caseIdentifier, activity, resource, startTime, result, -1);
    }

    /** Full environmental log entry
     * @param caseIdentifier Task name (string) a log entry should reference
     * @param resource should be class name of calling method
     * @param activity activity to be written to the log file
     * @param startTime start time for log entries that reference a duration
     * @param result result
     * @param quality percentage of quality for executed operations, otherwise a negative value
     */
    static public void env_log(String caseIdentifier, String resource, String activity, long startTime, String result, Double quality) {
        log("ENVIRONMENT", caseIdentifier, activity, resource, startTime, result, quality);
    }

    /**
     * Method for actively flushing buffered log entries to log file.
     */
    static public void flushBuffers() {
        logWriter.flush();
    }

    /**
     * Initialise a new logger artefact: Intentionally, this is implemented as a singleton to allow only one logger.
     *
     * @param name     Display name to be shown at the top of the window.
     * @param fileName File name to write CSV log to.
     */
    void init(String name, String fileName) throws IOException, ArtifactAlreadyPresentException {

        if (loggerArtifactId != null) {
            logger.warning("Multiple instances of logger not allowed (logger must be singleton).");
            throw new ArtifactAlreadyPresentException("LoggerArtifact", "unknown workspace");
        }

        baseDisplay = new Display(name);
        logWriter = new PrintWriter(new FileWriter(fileName, StandardCharsets.UTF_8));
        logWriter.println(String.join(",", csvFields));
        execInternalOp("flushTimer");
        loggerArtifactId = this.getId();
    }

    /**
     * Method that flushes buffered log entries to the file system at an interval of < 1 seconds to protect against loss of logging data in case of failure.
     */
    @INTERNAL_OPERATION
    void flushTimer() {
        while (true) {
            logWriter.flush();
            await_time(800);
        }
    }

    /**
     * Simple log entry by agent just logging an activity.
     * @param activity string describing activity performed
     */
    @OPERATION
    protected void log(String activity) {

        String agentName = this.getCurrentOpAgentId().getAgentName();

        log("MANAGEMENT", "", activity, agentName, -1, "", -1);
    }

    /** Log entry by agent logging a non-MANAGEMENT activity with no more details.
     * @param entryType reference to type of activity (usually: DOMAIN or MANAGEMENT)
     * @param activity string describing activity performed
     */
    @OPERATION
    protected void log(String entryType, String activity) {

        String agentName = this.getCurrentOpAgentId().getAgentName();

        log(entryType, "", activity, agentName, -1, "", -1);
    }

    /** Log entry by agent logging an activity with reference to a specific task.
     * @param entryType reference to type of activity (usually: DOMAIN or MANAGEMENT)
     * @param caseId reference to task
     * @param activity string describing activity performed
     */
    @OPERATION
    protected void log(String entryType, String caseId, String activity) {

        String agentName = this.getCurrentOpAgentId().getAgentName();

        log(entryType, caseId, activity, agentName, -1, "", -1);
    }

    /** Log entry by agent logging an activity with reference to a specific task and a result.
     * @param entryType reference to type of activity (usually: DOMAIN or MANAGEMENT)
     * @param caseId reference to task
     * @param activity string describing activity performed
     * @param result string describing result of activity
     */
    @OPERATION
    protected void log(String entryType, String caseId, String activity, String result) {

        String agentName = this.getCurrentOpAgentId().getAgentName();

        log(entryType, caseId, activity, agentName, -1, result, -1);
    }

    /** Full log entry by agent.
     * @param entryType reference to type of activity (usually: DOMAIN or MANAGEMENT)
     * @param caseId reference to task
     * @param activity string describing activity performed
     * @param result string describing result of activity
     * @param startTime start time for long-running operations, if not known: -1
     * @param quality percentage of quality for executed operations, otherwise a negative value
     */
    @OPERATION
    protected void log(String entryType, String caseId, String activity, String result, long startTime, double quality) {

        String agentName = this.getCurrentOpAgentId().getAgentName();

        log(entryType, caseId, activity, agentName, startTime, result, quality);
    }

    /** Operation for agents that want to register an independent display.
     * @param resource Should be the agent's name (could be taken from getCurrentOpAgentId().getAgentName() in future)
     * @param displayName Text string added to the agent's name in the top border of the GUI window.
     */
    @OPERATION
    protected void log_display(String resource, String displayName) {

        if (!display.containsKey(resource)) {
            display.put(resource, new Display(resource + ": " + displayName));
        }

    }

    /**
     * Class for displaying logging data while simulations run.
     * Note: For other environments, the sizing and positioning of GUI windows should be parameterised.
     */
    static class Display extends JFrame {
        private static int n = 0;
        private final DefaultTableModel table_model = new DefaultTableModel(0, csvFields.length);
        private final JTable table = new JTable(table_model);

        /** Constructor for new GUI output window.
         * @param name Name to be put as title of the GUI window
         */
        public Display(String name) {
            setTitle(name);

            table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            table_model.setColumnIdentifiers(csvFields);
            for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(String.format(fieldFormats[i], "").length() * 10);
            }

            JScrollPane scroll = new JScrollPane(table);
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scroll.setPreferredSize(new Dimension(1380, 250));
            add(scroll);
            pack();
            setLocation(50, n * 300 + 40);
            setVisible(true);
            toFront();

            n++;
        }

        /**
         * Method to add log entry to a GUI window.
         * @param logEntry Array of strings representing row to be added to GUI window.
         */
        public void addRow(String[] logEntry) {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < logEntry.length; i++) {
                    logEntry[i] = String.format(fieldFormats[i], logEntry[i]);
                }
                ((DefaultTableModel) table.getModel()).addRow(logEntry);
                table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
            });
        }
    }
}