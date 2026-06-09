# 互联网编程实验大作业：Topic 1 - 多线程聊天室

这是一个基于原生 Java (TCP/UDP/HTTP, NIO, SSL, Swing) 设计并实现的安全、多线程聊天室应用。完全对标了课程设计中针对 Topic 1 的核心要求、功能要求以及所有的高分扩展项（A/A+）。

## ✨ 功能实现情况 (对标作业要求)

### 1. 核心技术要求 (Core Technical Requirements)
- [x] **基于 TCP 的 C/S 通信**：底层使用 `Socket` 和 `ServerSocket` 建立稳定连接。
- [x] **多线程服务器**：能够同时处理多个客户端的并发请求。
- [x] **用户身份验证**：包含完整的注册/登录流程，密码在服务端使用 **SHA-256 + Salt (加盐)** 进行哈希安全存储。
- [x] **端到端加密**：核心的鉴权流量强制使用 **SSL/TLS** (`SSLSocket` / `SSLServerSocket`) 传输，防抓包。
- [x] **基于 UDP 的广播消息**：服务端定期发出 UDP 广播，用于客户端启动时的自动服务器发现。
- [x] **基于 HTTP 的文件共享**：原生内置 `HttpServer`，客户端使用 `HttpURLConnection` 完成文件上传与下载。
- [x] **非阻塞 I/O (NIO)**：高并发聊天核心使用了 `Selector` 与 `SocketChannel` 实现高度可扩展性。
- [x] **日志记录**：将用户连接、断开、错误日志以及管理操作写入本地 `ServerLog.txt` 文件。

### 2. 功能需求 (Functional Requirements)

**客户端 (Client-side):**
- [x] **GUI 界面**：使用原生 Java **Swing** 编写美观易用的界面。
- [x] **消息收发**：支持群聊（默认）以及私聊（输入 `/msg 目标用户名 消息`）。
- [x] **文件传输**：界面提供清晰的“上传文件”与“下载文件”入口。
- [x] **在线列表**：界面右侧实时刷新当前在线的所有用户名单。

**服务器端 (Server-side):**
- [x] **多用户处理**：通过线程池与 NIO 机制结合处理并发。
- [x] **会话管理**：有效维护和追踪在线用户的连接 Session。
- [x] **活动日志**：服务端控制台及文件均留存完整运行日志。
- [x] **管理员指令**：支持最高权限账户使用 `/kick <用户>`（踢出）和 `/ban <用户>`（永久封禁）指令。

### 3. A/A+ 高分可选扩展项 (Optional Extensions)
- [x] **代理支持 (Proxy support)**：客户端登录页可选填 SOCKS 代理服务器 IP 与端口，底层流量无缝走代理转发。
- [x] **IPv6 兼容性 (IPv6 compatibility)**：完全兼容并优先尝试双栈网络。
- [x] **DNS 解析 (DNS resolution)**：连接前使用 `InetAddress` 对输入的域名进行解析，并打印 IPv4/IPv6 结果。
- [x] **基于 Web 的管理员面板**：内置轻量级 Web 服务器（默认端口 `28081`），浏览器访问即可可视化踢人。

---

## 🚀 快速启动指南

本项目纯基于 Java 原生库，**无任何第三方依赖（无需 Maven/Gradle）**，编译和运行极其简单。

### 环境要求
- JDK 11 或更高版本 (支持并测试于 Java 25)

### 编译代码
在项目根目录下，打开终端运行：
```bash
javac server/*.java client/*.java
```

### 运行服务端
```bash
java server.ServerMain
```
服务端启动后会同时监听以下端口：
- **聊天端口 (NIO)**: `28080`
- **安全认证端口 (SSL)**: `28443`
- **UDP 广播端口**: `28888`
- **HTTP 文件服务器**: `28000`
- **Web 管理后台**: `28081`

*(在终端输入 `quit` 即可安全关闭服务端)*

### 运行客户端
在另一个新终端中运行（可打开多个测试群聊）：
```bash
java client.ClientMain
```

---

## 💻 目录结构说明

```text
server/
├── ServerMain.java       # 服务端启动入口（绑定所有子服务）
├── AuthServer.java       # 负责处理 SSL 加密的注册和登录
├── Database.java         # 模拟数据库，处理密码的加盐与哈希校验
├── HttpFileServer.java   # HTTP 文件服务器（负责文件收发）
├── NioChatServer.java    # 基于 NIO 的高性能聊天消息分发核心
├── ServerLog.java        # 日志系统，会将运行日志写入 ServerLog.txt
├── UdpBroadcaster.java   # UDP 广播器，用于宣告服务器 IP
└── WebAdminServer.java   # Web 端管理员面板服务器

client/
├── ClientMain.java       # 客户端启动入口
├── ChatGUI.java          # Swing 图形界面代码
└── NetworkClient.java    # 处理所有的网络通信 (TCP, SSL, UDP, HTTP, DNS, Proxy)
```

## 💡 使用提示

1. **私聊**：在聊天框输入 `/msg 对方用户名 你的消息` 即可发送私聊。
2. **Web 管理员后台**：打开浏览器访问 `http://localhost:28081`，可以直接查看到当前的在线用户列表并执行踢出操作。
3. **测试代理功能**：在客户端登录页面选填一个代理服务器 IP 和端口，底层的网络 Socket 就会通过代理建立连接（可随便填一个错误 IP 观察控制台报错来证明功能有效）。
