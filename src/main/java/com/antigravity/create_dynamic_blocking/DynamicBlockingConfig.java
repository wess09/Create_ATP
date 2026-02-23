package com.antigravity.create_dynamic_blocking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 动态闭塞的全局配置。
 * 支持从 config/create_dynamic_blocking.json 读取和保存。
 */
public class DynamicBlockingConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "create_dynamic_blocking.json");

    // ===== 配置项 =====
    
    /** 是否启用动态闭塞功能 */
    public static boolean enabled = true;
    
    /** 最大扫描距离（方块数）。超过此距离的列车不会被检测 */
    public static double maxScanDistance = 128.0;
    
    /** 减速开始距离（方块数）。从此距离开始平滑减速 */
    public static double slowdownDistance = 60.0;
    
    /** 紧急制动距离（方块数）。低于此距离时列车按比例急停 */
    public static double emergencyStopDistance = 30.0;
    
    /** 最终停车距离（方块数）。低于此距离时列车完全强制停车 (speed = 0) */
    public static double finalStopDistance = 5.0;
    
    /** 是否在日志中输出调试信息 */
    public static boolean debugLogging = false;

    // 解析使用的内部数据类
    private static class ConfigData {
        public boolean enabled = DynamicBlockingConfig.enabled;
        public double maxScanDistance = DynamicBlockingConfig.maxScanDistance;
        public double slowdownDistance = DynamicBlockingConfig.slowdownDistance;
        public double emergencyStopDistance = DynamicBlockingConfig.emergencyStopDistance;
        public double finalStopDistance = DynamicBlockingConfig.finalStopDistance;
        public boolean debugLogging = DynamicBlockingConfig.debugLogging;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    enabled = data.enabled;
                    maxScanDistance = data.maxScanDistance;
                    slowdownDistance = data.slowdownDistance;
                    emergencyStopDistance = data.emergencyStopDistance;
                    finalStopDistance = data.finalStopDistance;
                    debugLogging = data.debugLogging;
                }
            } catch (Exception e) {
                LOGGER.error("[动态闭塞] 读取配置文件失败，将使用默认值", e);
            }
        }
        // 每次加载后保存以格式化文件并写入任何缺失的默认参数
        save();
    }

    public static void save() {
        ConfigData data = new ConfigData();
        // 自动捕获当前静态字段的值
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("[动态闭塞] 保存配置文件失败", e);
        }
    }
}
