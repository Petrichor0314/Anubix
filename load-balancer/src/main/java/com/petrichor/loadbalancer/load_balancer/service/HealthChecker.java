package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class HealthChecker {

    private final List<ServerInfo> serverInfos;
    private final RestTemplate restTemplate;
    private final LoadBalancerConfig config;

    public HealthChecker(LoadBalancerConfig config, LoadBalancerService loadBalancerService) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.serverInfos = loadBalancerService.getServerInfos();
    }

    @Scheduled(fixedRateString = "${loadbalancer.healthcheck.interval:5000}")
    public void checkHealth() {
        for (ServerInfo server : serverInfos) {

            String healthUrl = server.getUrl().replace("/api/test", config.getHealthCheckPath());
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                server.setHealthy(response.getStatusCode().is2xxSuccessful());
            } catch (Exception e) {
                server.setHealthy(false);
            }
        }
    }
}
