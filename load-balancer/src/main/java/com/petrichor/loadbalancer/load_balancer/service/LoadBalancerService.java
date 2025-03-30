package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.factory.LoadBalancerAlgorithmFactory;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoadBalancerService {

    private final List<ServerInfo> serverInfos;
    private final RestTemplate restTemplate;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;

    public LoadBalancerService(LoadBalancerConfig config) {
        this.restTemplate = new RestTemplate();
        this.serverInfos = new ArrayList<>();
        this.config = config;
        // Initialize backend servers (these URLs can also be moved to config if needed)
        serverInfos.add(new ServerInfo("http://backend-server-1:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-2:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-3:8080/api/test"));

        // Select algorithm based on config property
        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());
    }

    public Optional<ServerInfo> selectServer() {
        return loadBalancerAlgorithm.selectServer(serverInfos);
    }

    public String forwardRequest() {
        Optional<ServerInfo> optionalServer = selectServer();
        if (optionalServer.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        ServerInfo server = optionalServer.get();
        server.incrementConnections();

        String response = null;
        int attempts = 0;
        while (attempts < config.getRetries()) {
            try {
                long start = System.currentTimeMillis();
                response = restTemplate.getForObject(server.getUrl(), String.class);
                long elapsed = System.currentTimeMillis() - start;
                // Update average response time (simple averaging)
                double currentAvg = server.getAvgResponseTime();
                double newAvg = (currentAvg + elapsed) / 2.0;
                server.setAvgResponseTime(newAvg);
                break; // Success; exit loop
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(config.getRetryDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        server.decrementConnections();
        if (response == null) {
            throw new RuntimeException("Failed to get response after " + config.getRetries() + " attempts");
        }
        return response;
    }
}
