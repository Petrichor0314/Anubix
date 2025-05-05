package com.petrichor.loadbalancer.load_balancer.algorithm;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinAlgorithm implements LoadBalancerAlgorithm {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<ServerInfo> selectServer(List<ServerInfo> servers) {
        if (servers.isEmpty()) return Optional.empty();
        int index = counter.getAndIncrement() % servers.size();
        return Optional.of(servers.get(index));
    }
}
