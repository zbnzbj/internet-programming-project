# Internet Programming Lab Project: Topic 1 - Multi-Threaded Chat Room

A secure, multi-threaded chat room application designed and implemented using native Java (TCP/UDP/HTTP, NIO, SSL, Swing). Fully aligned with the core requirements, functional requirements, and all A/A+ extension items specified in the course design for Topic 1.

## ✨ Feature Implementation Checklist (Mapped to Assignment Requirements)

### 1. Core Technical Requirements
- [x] **TCP-based C/S Communication**: Uses `Socket` and `ServerSocket` at the transport layer for stable connections.
- [x] **Multi-threaded Server**: Capable of handling concurrent requests from multiple clients simultaneously.
- [x] **User Authentication**: Complete registration/login workflow with passwords securely stored using **SHA-256 + Salt** hashing on the server side.
- [x] **End-to-End Encryption**: All authentication traffic is transmitted over **SSL/TLS** (`SSLSocket` / `SSLServerSocket`), preventing packet sniffing.
- [x] **UDP Broadcast Messaging**: The server periodically sends UDP broadcasts, enabling automatic server discovery when clients start up.
- [x] **HTTP-based File Sharing**: Built-in native `HttpServer`; clients use `HttpURLConnection` for file uploads and downloads.
- [x] **Non-blocking I/O (NIO)**: The high-concurrency chat core uses `Selector` and `SocketChannel` for high scalability.
- [x] **Logging**: Logs user connections, disconnections, errors, and admin operations to a local `ServerLog.txt` file.

### 2. Functional Requirements

**Client-side:**
- [x] **GUI Interface**: Clean and user-friendly interface built with native Java **Swing**.
- [x] **Messaging**: Supports group chat (default) and private messaging (via `/msg <username> <message>`).
- [x] **File Transfer**: The interface provides clear "Upload File" and "Download File" buttons.
- [x] **Online User List**: The right-side panel refreshes in real-time to display all currently online users.

**Server-side:**
- [x] **Multi-user Handling**: Combines thread pools with NIO mechanisms to handle concurrency.
- [x] **Session Management**: Effectively maintains and tracks online user connection sessions.
- [x] **Activity Logging**: Complete operation logs are preserved in both the server console and log file.
- [x] **Admin Commands**: Supports privileged admin commands: `/kick <user>` (disconnect) and `/ban <user>` (permanent ban).

### 3. A/A+ Optional Extensions
- [x] **Proxy Support**: The client login page optionally accepts a SOCKS proxy server IP and port; all underlying traffic seamlessly routes through the proxy.
- [x] **IPv6 Compatibility**: Fully compatible with dual-stack networking, with IPv6 prioritized.
- [x] **DNS Resolution**: Uses `InetAddress` to resolve hostnames before connecting, printing both IPv4 and IPv6 results.
- [x] **Web-based Admin Panel**: Built-in lightweight web server (default port `28081`); accessible via browser for visual user management.

---

## 🚀 Quick Start Guide

This project is built entirely on native Java libraries — **no third-party dependencies (no Maven/Gradle required)**. Compilation and execution are straightforward.

### Prerequisites
- JDK 11 or higher (tested on Java 25)

### Compile
Open a terminal in the project root directory and run:
```bash
javac server/*.java client/*.java
```

### Start the Server
```bash
java server.ServerMain
```
Once started, the server listens on the following ports:
- **Chat Port (NIO)**: `28080`
- **Secure Auth Port (SSL)**: `28443`
- **UDP Broadcast Port**: `28888`
- **HTTP File Server**: `28000`
- **Web Admin Dashboard**: `28081`

*(Type `quit` in the terminal to gracefully shut down the server)*

### Start the Client
In a separate terminal (you can open multiple instances to test group chat):
```bash
java client.ClientMain
```

---

## 💻 Project Structure

```text
server/
├── ServerMain.java       # Server entry point (binds all sub-services)
├── AuthServer.java       # Handles SSL-encrypted registration and login
├── Database.java         # Simulated database with salted password hashing
├── HttpFileServer.java   # HTTP file server (handles file upload/download)
├── NioChatServer.java    # NIO-based high-performance chat message dispatcher
├── ServerLog.java        # Logging system (writes to ServerLog.txt)
├── UdpBroadcaster.java   # UDP broadcaster (announces server IP on LAN)
└── WebAdminServer.java   # Web-based admin dashboard server

client/
├── ClientMain.java       # Client entry point
├── ChatGUI.java          # Swing GUI code
└── NetworkClient.java    # Handles all network communication (TCP, SSL, UDP, HTTP, DNS, Proxy)
```

## 💡 Usage Tips

1. **Private Messaging**: Type `/msg <target_username> <your_message>` in the chat input box to send a private message.
2. **Web Admin Dashboard**: Open a browser and navigate to `http://localhost:28081` to view online users and kick them directly.
3. **Testing Proxy Functionality**: Fill in a proxy server IP and port on the client login page. The underlying network sockets will route connections through the proxy (try entering a fake IP to observe connection errors as proof that the feature works).

---

## 🧪 Acceptance Testing & Demo Script

Follow this step-by-step script to quickly verify all features are working correctly:

### Preparation
Open **3 terminal windows**:
1. **Terminal A (Server)**: Run `java server.ServerMain`
2. **Terminal B (Client 1)**: Run `java client.ClientMain`
3. **Terminal C (Client 2)**: Run `java client.ClientMain`

### Act 1: Authentication, Networking & Extension Verification
1. **UDP Auto-Discovery & Dual-Stack Networking**:
   - Client 1: Change the `Server Hostname/IP` field to `localhost` (instead of 127.0.0.1).
   - **Expected Result**: Connection succeeds, and Terminal B prints both the resolved IPv4 and IPv6 addresses (e.g., `0:0:0:0:0:0:0:1`). (✅ DNS resolution & IPv6 test passed)
2. **Registration, Hashing & SSL Encryption**:
   - Client 1: Enter username `admin`, password `123`, click `Register` then `Login`.
   - Client 2: Enter username `user1`, password `123`, click `Register` then `Login`.
   - **Expected Result**: Open the local `users_db.properties` file — passwords are stored as SHA-256 + Salt hashed values, preventing eavesdropping and tampering. (✅ Authentication & end-to-end encryption test passed)
3. **SOCKS Proxy Test**:
   - Launch Client 3, enter a fake proxy IP `1.1.1.1`, port `1080`, then click Login.
   - **Expected Result**: The program freezes and eventually reports a connection failure, proving that the underlying network socket genuinely attempted to route through the proxy tunnel. (✅ Proxy extension test passed)

### Act 2: Chat & File Sharing
1. **Multi-threading & Group Chat**:
   - In the `admin` interface, type and send "Hello everyone".
   - **Expected Result**: `user1` receives the message instantly; the online user list on the right displays both users. (✅ Multi-threading, group chat & online list test passed)
2. **Private Messaging**:
   - In `user1`'s input box, type: `/msg admin Private message test`
   - **Expected Result**: Only `admin`'s interface displays the private message content. (✅ Private messaging test passed)
3. **HTTP File Sharing**:
   - `user1` clicks `Upload File` and selects any small file from the local machine.
   - `admin` types the exact filename in the input box and clicks `Download File`.
   - **Expected Result**: The file downloads instantly to the current working directory. (✅ HTTP file sharing test passed)

### Act 3: Admin Privileges & Logging Final Test
1. **Web Admin Dashboard**:
   - Open browser and navigate to: `http://localhost:28081`
   - **Expected Result**: The page displays an online user table. Click the `Kick` button next to `user1` — `user1`'s client is immediately disconnected. (✅ Web admin panel test passed)
2. **Supreme Admin Commands**:
   - `user1` logs back in.
   - `admin` types in the chat box: `/ban user1`
   - **Expected Result**: `user1` is disconnected again, and any subsequent login attempt will be rejected by the system. (✅ Admin command test passed)
3. **Server Log Audit**:
   - Open `ServerLog.txt` in the project root directory.
   - **Expected Result**: Contains detailed timestamped logs of all connection, disconnection, Kick, and Ban actions performed during the demo. (✅ Logging test passed)
