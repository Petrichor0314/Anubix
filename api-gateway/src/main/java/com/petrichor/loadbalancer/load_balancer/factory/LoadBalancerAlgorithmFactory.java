package com.petrichor.loadbalancer.load_balancer.factory;

import com.petrichor.loadbalancer.load_balancer.algorithm.AdaptiveAlgorithm;
import com.petrichor.loadbalancer.load_balancer.algorithm.LeastConnectionsAlgorithm;
import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import com.petrichor.loadbalancer.load_balancer.algorithm.RoundRobinAlgorithm;

public class LoadBalancerAlgorithmFactory {

    public static LoadBalancerAlgorithm getAlgorithm(String algorithmName) {
        switch (algorithmName.toLowerCase()) {
            case "round-robin":
                return new RoundRobinAlgorithm();
            case "least-connections":
                return new LeastConnectionsAlgorithm();
            case "adaptive":
                return new AdaptiveAlgorithm();
            default:
                throw new IllegalArgumentException("Unknown load balancing algorithm: " + algorithmName);
        }
    }
}
