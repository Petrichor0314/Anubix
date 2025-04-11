package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.factory.LoadBalancerAlgorithmFactory;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class LoadBalancerService {

    private final List<ServerInfo> serverInfos;
    private final RestTemplate restTemplate;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;

    public LoadBalancerService(LoadBalancerConfig config) {
        this.restTemplate = new RestTemplate();
        this.config = config;
        this.serverInfos = new CopyOnWriteArrayList<>();

        // Manually configured backend servers
        serverInfos.add(new ServerInfo("http://backend-server-1:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-2:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-3:8080/api/test"));

        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());
    }

    public List<ServerInfo> getServerInfos() {
        return serverInfos;
    }

    public Optional<ServerInfo> selectServer(List<ServerInfo> candidates) {
        return loadBalancerAlgorithm.selectServer(candidates);
    }

    public String forwardRequest() {
        int retries = config.getRetries(); // from environment or config
        int attempts = 0;

        while (attempts < retries) {
            List<ServerInfo> healthyServers = serverInfos.stream()
                    .filter(ServerInfo::isHealthy)
                    .collect(Collectors.toList());

            if (healthyServers.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No healthy servers available");
            }

            Optional<ServerInfo> optionalServer = selectServer(healthyServers);
            if (optionalServer.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No server could be selected");
            }

            ServerInfo server = optionalServer.get();
            server.incrementConnections();

            try {
                long start = System.currentTimeMillis();
                String response = restTemplate.getForObject(server.getUrl(), String.class);
                long elapsed = System.currentTimeMillis() - start;

                double currentAvg = server.getAvgResponseTime();
                double newAvg = (currentAvg + elapsed) / 2.0;
                server.setAvgResponseTime(newAvg);

                return response;

            } catch (Exception e) {
                server.setHealthy(false); // mark as unhealthy on failure
                System.err.println("Failed to reach " + server.getUrl() + ": " + e.getMessage());
                attempts++;
            } finally {
                server.decrementConnections();
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "All retries failed. No backend responded.");
    }
}
