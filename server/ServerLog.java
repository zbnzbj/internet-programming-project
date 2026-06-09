package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [EN] Server Activity Logger
 * Features: Logs all critical events on the server (component startup, user connect/disconnect, admin bans)
 * with standardized timestamps to a local 'ServerLog.txt' file for auditing and debugging.
 *
 * [ZH] 服务端活动日志记录器
 * 功能：
 * 将服务端产生的所有关键事件（如：组件启动、用户连接/断开、管理员封禁操作）
 * 以带有标准时间戳的格式写入本地的 ServerLog.txt 文件中，便于审计与调试。
 */
public class ServerLog {
    private static final String LOG_FILE = "ServerLog.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // [EN] Log an informational message
    // [ZH] 记录一条常规信息日志
    public static synchronized void info(String message) {
        log("INFO", message);
    }

    // [EN] Log a warning message
    // [ZH] 记录一条警告级别日志
    public static synchronized void warning(String message) {
        log("WARN", message);
    }

    // [EN] Log an error message
    // [ZH] 记录一条错误级别日志
    public static synchronized void error(String message) {
        log("ERROR", message);
    }

    // [EN] Core logging method to format and append the message to the file and console
    // [ZH] 核心日志记录方法，负责格式化时间戳并同时输出到控制台与本地文件
    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        System.out.println(logEntry);
        
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
