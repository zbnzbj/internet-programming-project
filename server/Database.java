package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

/**
 * 本地用户数据库管理类 (Database Manager)
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
        load();
    }
    
    private synchronized void load() {
        File f = new File(DB_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                users.load(in);
            } catch (IOException e) {
                ServerLog.error("Failed to load user database: " + e.getMessage());
            }
        }
    }
    
    private synchronized void save() {
        try (FileOutputStream out = new FileOutputStream(DB_FILE)) {
            users.store(out, "User Database - username=salt:hash");
        } catch (IOException e) {
            ServerLog.error("Failed to save user database: " + e.getMessage());
        }
    }
    
    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        
        // Generate a random salt (simple version using timestamp and Math.random)
        String salt = Base64.getEncoder().encodeToString(String.valueOf(System.nanoTime() + Math.random()).getBytes());
        String hash = hashPassword(password, salt);
        
        users.setProperty(username, salt + ":" + hash);
        save();
        ServerLog.info("Registered new user: " + username);
        return true;
    }
    
    public synchronized boolean authenticate(String username, String password) {
        String data = users.getProperty(username);
        if (data == null) {
            return false;
        }
        
        String[] parts = data.split(":");
        if (parts.length != 2) return false;
        String salt = parts[0];
        String expectedHash = parts[1];
        
        String actualHash = hashPassword(password, salt);
        return expectedHash.equals(actualHash);
    }
    
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
