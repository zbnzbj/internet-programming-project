package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLog {
    private static final String LOG_FILE = "ServerLog.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter writer;

    static {
        try {
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("Failed to initialize server logger: " + e.getMessage());
        }
    }

    private static synchronized void log(String level, String message) {
        String time = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", time, level, message);
        
        System.out.println(logEntry); // Print to console
        if (writer != null) {
            writer.println(logEntry); // Write to file
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warning(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }
}
