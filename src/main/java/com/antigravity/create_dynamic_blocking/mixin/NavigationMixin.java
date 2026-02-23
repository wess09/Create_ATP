package com.antigravity.create_dynamic_blocking.mixin;

import com.antigravity.create_dynamic_blocking.DynamicBlockingHandler;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 动态闭塞 Mixin v2 —— 注入 Navigation.tick() 方法。
 *
 * 注入策略变更说明：
 * v1: TAIL + 修改 distanceToSignal → 失败，因为速度已经算完了
 * v2: TAIL + 直接修改 train.speed → 在原生速度计算完成后，强制覆盖为安全速度
 *
 * 这样做的好处是：
 * 1. 不需要与信号系统交互，避免了 currentSignalResolved() 清除伪信号的问题
 * 2. 在所有原生逻辑执行完后做最终裁决，确保动态闭塞拥有最高优先级
 * 3. 制动曲线由 DynamicBlockingHandler 自行计算，与原生公式一致
 */
@Mixin(value = Navigation.class, remap = false)
public abstract class NavigationMixin {

    @Shadow
    public Train train;

    @Shadow
    public GlobalStation destination;

    /**
     * 在 Navigation.tick() 方法的最末尾注入。
     * 此时原生逻辑已经计算完速度（包括信号、制动弯道等）。
     * 我们在此基础上，检查前方是否有列车，必要时进一步降速。
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void createDynamicBlocking_afterTick(Level level, CallbackInfo ci) {
        if (destination == null) return;
        if (train.graph == null) return;

        // 交给核心处理器执行动态闭塞逻辑
        DynamicBlockingHandler.enforceSpacing(train, train.currentlyBackwards);
    }

    // Mixin 加载确认：静态代码块会在类加载时输出日志
    static {
        com.mojang.logging.LogUtils.getLogger().info("[动态闭塞] NavigationMixin 已成功注入 Navigation 类！");
    }
}
