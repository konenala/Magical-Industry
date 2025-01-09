package com.github.nalamodikk.common.network.ManaNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ManaNetworkManager {
    // 單例實例
    private static final ManaNetworkManager INSTANCE = new ManaNetworkManager();

    // 節點集合
    private final Set<ManaNetworkNode> nodes = new HashSet<>();

    // 私有構造函數，防止外部實例化
    private ManaNetworkManager() {}

    // 獲取單例實例
    public static ManaNetworkManager getInstance() {
        return INSTANCE;
    }

    // 註冊節點
    public void registerNode(ManaNetworkNode node) {
        nodes.add(node);
        rebuildNetwork();
    }

    // 註銷節點
    public void unregisterNode(ManaNetworkNode node) {
        nodes.remove(node);
        rebuildNetwork();
    }

    // 分配魔力
    public void distributeMana() {
        int totalAvailableMana = nodes.stream()
                .filter(ManaNetworkNode::isStorageNode)
                .mapToInt(ManaNetworkNode::getManaStored)
                .sum();

        int totalRequestedMana = nodes.stream()
                .filter(ManaNetworkNode::isConsumerNode)
                .mapToInt(ManaNetworkNode::getRequestedMana)
                .sum();

        if (totalAvailableMana == 0 || totalRequestedMana == 0) return;

        for (ManaNetworkNode consumer : nodes) {
            if (consumer.isConsumerNode()) {
                int request = consumer.getRequestedMana();
                for (ManaNetworkNode storage : nodes) {
                    if (storage.isStorageNode() && request > 0) {
                        int transferred = storage.transferMana(request);
                        consumer.receiveMana(transferred);
                        request -= transferred;
                    }
                }
            }
        }
    }

    // 獲取節點
    public ManaNetworkNode getNode(Level level, BlockPos pos) {
        return nodes.stream()
                .filter(node -> node.getLevel().equals(level) && node.getPosition().equals(pos))
                .findFirst()
                .orElse(null);
    }

    // 重建網絡
    public void rebuildNetwork() {
        Set<ManaNetworkNode> visited = new HashSet<>();
        Queue<ManaNetworkNode> queue = new LinkedList<>();

        for (ManaNetworkNode node : nodes) {
            if (visited.contains(node)) continue;

            queue.add(node);
            visited.add(node);

            while (!queue.isEmpty()) {
                ManaNetworkNode current = queue.poll();
                for (Direction direction : Direction.values()) {
                    ManaNetworkNode neighbor = getNode(current.getLevel(), current.getPosition().relative(direction));
                    if (neighbor != null && !visited.contains(neighbor)) {
                        queue.add(neighbor);
                        visited.add(neighbor);

                        // 連接當前節點與鄰居節點
                        current.addNeighbor(neighbor);
                        neighbor.addNeighbor(current);
                    }
                }
            }
        }
    }

}
