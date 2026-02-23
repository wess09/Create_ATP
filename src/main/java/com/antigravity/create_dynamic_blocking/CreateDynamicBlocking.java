package com.antigravity.create_dynamic_blocking;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 机械动力：动态闭塞 —— 模组主类
 *
 * 本模组为机械动力(Create)的列车系统添加"动态闭塞"功能。
 * 在真实铁路中，动态闭塞（Moving Block / 移动闭塞）意味着
 * 列车之间不依赖固定的信号区间，而是根据前车位置实时调整安全距离。
 *
 * 技术实现：
 * - 通过 Mixin 注入 Navigation.tick()
 * - 在原生信号扫描后，额外检测前方列车
 * - 如果发现前车距离过近，模拟一个虚拟信号来触发制动
 */
@Mod(CreateDynamicBlocking.MODID)
public class CreateDynamicBlocking {
    public static final String MODID = "create_dynamic_blocking";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateDynamicBlocking(net.neoforged.bus.api.IEventBus modEventBus) {
        // 读取或创建配置文件
        DynamicBlockingConfig.load();

        LOGGER.info("[动态闭塞] 模组已加载！开始距离: {}格, 停车距离: {}格",
                DynamicBlockingConfig.slowdownDistance,
                DynamicBlockingConfig.emergencyStopDistance);

        // 注册服务器命令事件
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        // 注册 /cdb 命令，包含一个 reload 子命令
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("cdb")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限 (OP)
                .then(net.minecraft.commands.Commands.literal("reload")
                        .executes(context -> {
                            DynamicBlockingConfig.load();
                            context.getSource().sendSuccess(() ->
                                    net.minecraft.network.chat.Component.literal("§a[动态闭塞] 配置已热重载！" +
                                            "当前安全距离: " + DynamicBlockingConfig.finalStopDistance), true);
                            return 1;
                        })
                )
        );
    }
}
