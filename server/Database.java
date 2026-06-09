package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

/**
 * [EN] Local User Database Manager
 * Core technology: Properties key-value storage and SHA-256 encryption.
 * Features:
 * 1. Provides password hash encryption during registration (SHA-256 + random Salt).
 * 2. Verifies passwords during user login.
 * 3. Persists all user credentials locally into the 'users_db.properties' file.
 *
 * [ZH] 本地用户数据库管理类
 * 核心技术：Properties 键值对存储与 SHA-256 加密算法。
 * 功能：
 * 1. 提供用户注册时的密码哈希加密（SHA-256 + 随机 Salt）。
 * 2. 提供用户登录时的密码比对校验。
 * 3. 所有的用户凭证会被持久化保存到本地文件 users_db.properties 中。
 */
public class Database {
    private static final String DB_FILE = "users_db.properties";
    private Properties users;

    public Database() {
        users = new Properties();
        loadDatabase();
    }

    // [EN] Load the database from the local file system
    // [ZH] 从本地文件系统加载数据库配置
    private void loadDatabase() {
        try {
            File file = new File(DB_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                users.load(fis);
            }
        } catch (IOException e) {
            ServerLog.error("Failed to load database: " + e.getMessage());
        }
    }

    // [EN] Save the current user data to the local file
    // [ZH] 将当前的用户数据保存持久化到本地文件
    private void saveDatabase() {
        try (FileOutputStream fos = new FileOutputStream(DB_FILE)) {
            users.store(fos, "User Credentials (Salt:HashedPassword)");
        } catch (IOException e) {
            ServerLog.error("Failed to save database: " + e.getMessage());
        }
    }

    // [EN] Hash the password using SHA-256
    // [ZH] 使用 SHA-256 算法对密码进行哈希加密
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    // [EN] Register a new user with a randomly generated salt
    // [ZH] 注册新用户，自动生成随机 Salt 并加密密码
    public synchronized boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // [EN] User already exists / [ZH] 用户已存在
        }
        String salt = Long.toHexString(Double.doubleToLongBits(Math.random()));
        String hashed = hashPassword(password, salt);
        users.setProperty(username, salt + ":" + hashed);
        saveDatabase();
        ServerLog.info("New user registered: " + username);
        return true;
    }

    // [EN] Verify user credentials during login
    // [ZH] 在用户登录时验证凭证是否匹配
    public synchronized boolean verifyUser(String username, String password) {
        String storedData = users.getProperty(username);
        if (storedData == null) {
            return false; // [EN] User not found / [ZH] 用户不存在
        }
        String[] parts = storedData.split(":");
        if (parts.length != 2) return false;
        String salt = parts[0];
        String storedHash = parts[1];
        String loginHash = hashPassword(password, salt);
        return storedHash.equals(loginHash);
    }
}
