package com.petrichor.loadbalancer.load_balancer.algorithm;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import java.util.List;
import java.util.Optional;

public interface LoadBalancerAlgorithm {
    Optional<ServerInfo> selectServer(List<ServerInfo> servers);
}
