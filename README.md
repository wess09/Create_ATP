# Create: Dynamic Blocking (机械动力：动态闭塞)

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)](https://www.minecraft.net/)
[![NeoForge Version](https://img.shields.io/badge/NeoForge-21.1.219-orange.svg)](https://neoforged.net/)
[![Create Version](https://img.shields.io/badge/Create-0.6.10--255-green.svg)](https://www.curseforge.com/minecraft/mc-mods/create)

**Create: Dynamic Blocking** 是一个为 Minecraft 模组 [机械动力 (Create)](https://www.curseforge.com/minecraft/mc-mods/create) 开发的扩展补丁，旨在为列车系统引入真实的“动态闭塞”（Moving Block）功能。

> [!IMPORTANT]
> **开发说明**：本模组的核心逻辑、拓扑扫描算法以及绝大部分代码实现均由 **AI** 完成。

---

## 📖 功能简介

在原生的机械动力中，列车通过固定的信号机区间进行闭塞管理。本模组通过引入**动态闭塞（ATP - Automatic Train Protection 自动列车保护）**逻辑，打破了固定区间的限制：

- **实时间距控制**：列车不再依赖信号灯，而是根据前车的实时位置、当前速度和制动性能自动调整间距。
- **平滑减速**：当检测到前方有列车时，后车会根据距离平滑降低目标速度，而非突兀地停止。
- **拓扑感知扫描**：基于轨道图逻辑（Track Graph）的 v4 拓扑扫描算法，能够完美处理弯道、分叉路口和贝塞尔曲线轨道，确保距离计算的极高精度。
- **自动制动保护**：包含三级保护机制：减速区、紧急制动区和最终停车区。

---

## 🛠️ 指令说明

模组提供了一个主指令 `/cdb` 用于进行管理操作：

- `/cdb reload`：热重载配置文件。无需重启服务器即可应用新的安全距离限制。
  > *需要管理员权限 (OP 2级)*

---

## ⚙️ 配置选项

配置文件位于 `.minecraft/config/create_dynamic_blocking.json`。

| 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `enabled` | `true` | 是否启用动态闭塞功能。 |
| `maxScanDistance` | `128.0` | 最大扫描距离（方块）。超过此距离的列车不会被检测。 |
| `slowdownDistance` | `60.0` | **减速开始距离**。进入此范围后列车开始平滑限速。 |
| `emergencyStopDistance` | `30.0` | **紧急制动距离**。低于此距离时将采取更激进的减速措施。 |
| `finalStopDistance` | `5.0` | **最终停车距离**。低于此距离时列车将被强制保持静止速度。 |
| `debugLogging` | `false` | 是否在日志中打印详细的拓扑扫描和限速计算调试信息。 |

---

## 🚀 技术细节

- **注入点**：通过 Mixin 注入 `Navigation.tick()`，在每刻（Tick）逻辑中实时计算前方净空距离。
- **算法模型**：
  - 动态计算制动距离：`brakingDist = (v^2) / (2 * a)`。
  - 基于距离函数 `sqrt(2 * a * d * 0.85)` 计算最大安全速度。
- **兼容性**：专为 NeoForge 1.21.1 和 Create v0.6 打造。

---

## 🏗️ 开发信息

- **构建环境**: JDK 21, Gradle 8.x
- **核心模块**: 机械动力 (Create), Catnip, Ponder
