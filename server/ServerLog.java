package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 服务端活动日志记录器 (Server Logger)
 * 功能：
 * 将服务端产生的所有关键事件（如：组件启动、用户连接/断开、管理员封禁操作）
 * 以带有标准时间戳的格式写入本地的 ServerLog.txt 文件中，便于审计与调试。
 */
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
