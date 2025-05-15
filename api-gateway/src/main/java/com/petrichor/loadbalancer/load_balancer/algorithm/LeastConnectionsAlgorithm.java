package com.petrichor.loadbalancer.load_balancer.algorithm;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import java.util.List;
import java.util.Optional;

public class LeastConnectionsAlgorithm implements LoadBalancerAlgorithm {
    @Override
    public Optional<ServerInfo> selectServer(List<ServerInfo> servers) {
        return servers.stream()
                .filter(ServerInfo::isHealthy)
                .min((s1, s2) -> Integer.compare(s1.getActiveConnections(), s2.getActiveConnections()));
    }
}
