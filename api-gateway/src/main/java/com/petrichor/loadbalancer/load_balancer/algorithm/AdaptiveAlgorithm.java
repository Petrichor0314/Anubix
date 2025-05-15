package com.petrichor.loadbalancer.load_balancer.algorithm;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import java.util.List;
import java.util.Optional;

public class AdaptiveAlgorithm implements LoadBalancerAlgorithm {
    @Override
    public Optional<ServerInfo> selectServer(List<ServerInfo> servers) {
        return servers.stream()
                .filter(ServerInfo::isHealthy)
                .min((s1, s2) -> Double.compare(s1.getAvgResponseTime(), s2.getAvgResponseTime()));
    }
}
