package com.antigravity.create_dynamic_blocking;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态闭塞核心处理器 v4 (拓扑版)。
 *
 * 优化点：
 * 1. 拓扑跳跃：不再 1m 步进，而是逐个 Edge 检查。
 * 2. 方向感知：根据 scout 在 Edge 上的 position 变化趋势判定前方。
 * 3. Bezier 支持：完美支持长弯道内的精确距离。
 */
public class DynamicBlockingHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void enforceSpacing(Train currentTrain, boolean backwards) {
        if (currentTrain.graph == null) return;
        if (!DynamicBlockingConfig.enabled) return;

        TrackGraph graph = currentTrain.graph;

        // 1. 获取列车头部行驶点
        TravellingPoint leadingPoint;
        if (!backwards) {
            leadingPoint = currentTrain.carriages.get(0).getLeadingPoint();
        } else {
            leadingPoint = currentTrain.carriages.get(currentTrain.carriages.size() - 1).getTrailingPoint();
        }

        if (leadingPoint == null || leadingPoint.edge == null) return;

        double currentSpeed = Math.abs(currentTrain.speed);
        if (currentSpeed < 0.001 && Math.abs(currentTrain.targetSpeed) < 0.001) return;

        double acceleration = currentTrain.acceleration();
        double brakingDist = (currentSpeed * currentSpeed) / (2 * Math.max(0.01, acceleration));
        double scanDist = Math.max(Math.max(brakingDist * 2, DynamicBlockingConfig.slowdownDistance * 1.2), DynamicBlockingConfig.maxScanDistance);

        // 2. 将此图上其它列车的点映射到 Edge
        Map<TrackEdge, List<TravellingPoint>> occupancyMap = new HashMap<>();
        for (Train otherTrain : Create.RAILWAYS.trains.values()) {
            if (otherTrain == currentTrain || otherTrain.graph != graph || otherTrain.derailed) continue;
            for (Carriage c : otherTrain.carriages) {
                if (c.getLeadingPoint() != null && c.getLeadingPoint().edge != null) 
                    occupancyMap.computeIfAbsent(c.getLeadingPoint().edge, k -> new ArrayList<>()).add(c.getLeadingPoint());
                if (c.getTrailingPoint() != null && c.getTrailingPoint().edge != null) 
                    occupancyMap.computeIfAbsent(c.getTrailingPoint().edge, k -> new ArrayList<>()).add(c.getTrailingPoint());
            }
        }

        // 3. 拓扑扫描
        TravellingPoint scout = new TravellingPoint();
        scout.node1 = leadingPoint.node1;
        scout.node2 = leadingPoint.node2;
        scout.edge = leadingPoint.edge;
        scout.position = leadingPoint.position;

        double closestDist = Double.MAX_VALUE;
        double accumulatedDistance = 0;

        for (int i = 0; i < 50; i++) {
            if (accumulatedDistance > scanDist) break;
            TrackEdge currentEdge = scout.edge;
            if (currentEdge == null) break;

            // 探测当前 Edge 的目标 Node
            // 我们通过尝试 travel 一小段距离来确定是在往 node1 还是 node2 走
            double p0 = scout.position;
            TrackNode n1 = scout.node1;
            TrackNode n2 = scout.node2;
            TrackEdge e0 = scout.edge;
            
            scout.travel(graph, 0.1, currentTrain.navigation.controlSignalScout(), scout.ignoreEdgePoints(), scout.ignoreTurns());
            boolean movingToNode2 = (scout.edge == e0 && scout.position > p0) || (scout.edge != e0 && n2 != n1);
            
            // 还原位置以便检查当前 Edge
            scout.position = p0;
            scout.node1 = n1;
            scout.node2 = n2;
            scout.edge = e0;

            // 检查当前 Edge 是否有障碍
            List<TravellingPoint> obstacles = occupancyMap.get(currentEdge);
            if (obstacles != null) {
                for (TravellingPoint obs : obstacles) {
                    double d = -1;
                    if (movingToNode2) {
                        if (obs.position > scout.position + 0.1) d = obs.position - scout.position;
                    } else {
                        if (obs.position < scout.position - 0.1) d = scout.position - obs.position;
                    }
                    if (d > 0) closestDist = Math.min(closestDist, accumulatedDistance + d);
                }
            }

            if (closestDist < Double.MAX_VALUE) break;

            // 跳转到下一条边
            double len = currentEdge.getLength();
            double toEnd = movingToNode2 ? (len - scout.position) : scout.position;
            double actual = scout.travel(graph, toEnd + 0.1, currentTrain.navigation.controlSignalScout(), scout.ignoreEdgePoints(), scout.ignoreTurns());
            
            if (Math.abs(actual) < 0.001) break;
            accumulatedDistance += Math.abs(actual);
        }

        // 4. 制动
        if (closestDist >= scanDist || closestDist == Double.MAX_VALUE) return;

        if (closestDist <= DynamicBlockingConfig.finalStopDistance) {
            currentTrain.speed = 0;
            currentTrain.targetSpeed = 0;
            return;
        }

        double availableDist = closestDist - DynamicBlockingConfig.finalStopDistance;
        double maxSafeSpeed = Math.sqrt(2 * Math.max(0.01, acceleration) * availableDist * 0.85);
        double topSpeed = currentTrain.maxSpeed() * currentTrain.throttle;

        if (maxSafeSpeed > topSpeed) return;

        double sign = Math.signum(currentTrain.speed);
        if (currentSpeed > maxSafeSpeed) {
            currentTrain.speed = sign * maxSafeSpeed;
            currentTrain.targetSpeed = sign * maxSafeSpeed;
            
            if (DynamicBlockingConfig.debugLogging) {
                LOGGER.info("[动态闭塞] v4拓扑扫描: 距离={}m, 限速={}", 
                    String.format("%.2f", closestDist), String.format("%.2f", maxSafeSpeed));
            }
        }
    }
}
