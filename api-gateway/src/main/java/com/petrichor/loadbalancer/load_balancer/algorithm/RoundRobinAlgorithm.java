package com.petrichor.loadbalancer.load_balancer.algorithm;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;

public class RoundRobinAlgorithm implements LoadBalancerAlgorithm {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<ServerInfo> selectServer(List<ServerInfo> servers) {
        if (servers == null || servers.isEmpty()) {
            return Optional.empty();
        }

        List<ServerInfo> healthyServers = servers.stream()
                .filter(ServerInfo::isHealthy)
                .toList();

        if (healthyServers.isEmpty()) {
            return Optional.empty(); // No healthy servers available
        }
        
        int index = counter.getAndIncrement() % healthyServers.size();
        return Optional.of(healthyServers.get(index));
    }
}
