# 在 Windows 上安装 Redis（与 HourlyStore / Spring Boot 3.2 配套说明）

## 结论：和 Spring Boot 要“匹配”的是什么？

Spring Data Redis（Lettuce）只要求 **RESP 协议** 兼容的 Redis 服务端，并不绑定某一固定小版本号。  
对你当前项目（只用 `SET` / `GET` / `EXPIRE` 存登录 token）来说，**Redis 3.x～7.x 的服务端都可以**。

更推荐 **较新、仍维护** 的实现，而不是很老的 Windows 移植版。

---

## 方案 A（推荐）：Memurai Developer

- **说明**：Windows 原生、**兼容 Redis 7 API**（官方写明支持 Redis API 7.0），与 Spring Boot 3.2 生态更合拍。
- **安装**（建议 **以管理员身份** 打开 PowerShell 或 CMD，只跑其中一条，不要同时跑两个 winget 安装）：

```powershell
winget install Memurai.MemuraiDeveloper --accept-package-agreements --accept-source-agreements
```

- 安装过程中若弹出 UAC，请点“是”。
- 默认一般监听 **`127.0.0.1:6379`**，与 `application.yml` 一致。

---

## 方案 B：winget 里的 “Redis on Windows”（旧版移植）

- **包名**：`Redis.Redis`，版本约 **3.0.x**（微软存档的 Windows 构建）。
- **说明**：很老，但对你现在的 **基础功能** 够用；若方案 A 安装失败可试这个。

```powershell
winget install Redis.Redis --accept-package-agreements --accept-source-agreements
```

安装后通常可在 `C:\Program Files\Redis\` 找到 `redis-server.exe` / `redis-cli.exe`，需要时可手动启动服务或把目录加入 PATH。

---

## 若 winget 一直卡住

1. 关掉所有“正在安装”的窗口，在任务管理器里结束卡死的 **msiexec.exe**（若没有正常安装请勿随意结束）。
2. **只保留一个**安装程序重试（不要 Memurai 和 Redis.Redis 同时装）。
3. 或自行从 Memurai / Redis 官网下载 MSI，双击安装（同样需要管理员权限）。

---

## 验证是否可用

若已把 `redis-cli` 加入 PATH：

```text
redis-cli ping
```

应返回 `PONG`。

若没有 `redis-cli`，只要 **本机 6379 端口** 有服务监听，且 Spring Boot 启动不再报 Redis 连接错误即可。

---

## 与 `application.yml` 的关系

默认配置为：

- `host: localhost`
- `port: 6379`
- 无密码

若你设置了密码，在 `spring.data.redis` 下增加：

```yaml
spring:
  data:
    redis:
      password: 你的密码
```
