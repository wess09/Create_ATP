package com.antigravity.create_dynamic_blocking.mixin;

import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TrainMixin —— 预留扩展点。
 * 目前动态闭塞的核心逻辑全部在 NavigationMixin 中完成。
 * 此类保留以备后续扩展（例如列车间通信、状态广播等）。
 */
@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {
    // 预留：后续可在此添加列车状态同步逻辑
}
