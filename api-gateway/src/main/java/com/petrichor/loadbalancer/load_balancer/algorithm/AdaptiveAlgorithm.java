package com.petrichor.loadbalancer.load_balancer.algorithm;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;

public class AdaptiveAlgorithm implements LoadBalancerAlgorithm {
    @Override
    public Optional<ServerInfo> selectServer(List<ServerInfo> servers) {
        if (servers == null || servers.isEmpty()) {
            return Optional.empty();
        }

        return servers.stream()
                .filter(ServerInfo::isHealthy)
                .min(Comparator.comparingInt(ServerInfo::getActiveConnections)
                             .thenComparingDouble(ServerInfo::getAvgResponseTime));
    }
}
