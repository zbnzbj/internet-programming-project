# 互联网编程实验大作业：Topic 1 - 多线程聊天室

这是一个基于原生 Java (TCP/UDP/HTTP, NIO, SSL, Swing) 设计并实现的安全、多线程聊天室应用。完全对标了课程设计中针对 Topic 1 的核心要求、功能要求以及所有的高分扩展项（A/A+）。

## ✨ 核心特性

- **多线程服务端 (NIO)**：使用 Java NIO `Selector` 和 `SocketChannel` 实现高并发的聊天分发核心。
- **安全认证与端到端加密**：注册与登录走原生的 `SSLServerSocket` 实现端到端流量加密，用户密码使用 **SHA-256 + Salt** 存储。
- **UDP 自动发现**：服务端周期性发送 UDP 广播，客户端启动时**免填IP，自动发现服务器**。
- **HTTP 文件共享**：内置原生 `HttpServer`，提供基于 HTTP 的文件上传(`/upload`)与下载(`/download`)功能。
- **Swing 图形界面**：包含登录注册界面、在线用户列表、群聊/私聊显示区以及文件收发面板。
- **管理员后台与指令**：
  - 支持内置 Web 端后台管理面板（默认 `http://localhost:28081`），在浏览器内可一键踢人。
  - 支持客户端输入 `/kick <用户名>` 踢人。
- **高分拓展项 (A/A+) 满配支持**：
  - **DNS与双栈网络支持**：客户端输入域名（如 localhost）可自动进行 DNS 解析，原生兼容 IPv6。
  - **SOCKS 代理支持**：登录界面提供代理服务器 IP/Port 输入，底层网络通信无缝走 SOCKS 代理。

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
