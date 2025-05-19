package com.petrichor.loadbalancer.load_balancer.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;

@Component
public class ServerInfoRegistry {
    private final Map<String, ServerInfo> serverInfoMap = new ConcurrentHashMap<>();

    /**
     * Retrieves an existing ServerInfo for the given URL or creates a new one if not found.
     * This ensures that the same ServerInfo instance is used for a given server URL,
     * allowing its state (active connections, avg response time, health) to be managed consistently.
     *
     * @param serverUrl The unique URL of the server instance.
     * @return The existing or newly created ServerInfo instance.
     */
    public ServerInfo getOrCreateServerInfo(String serverUrl) {
        return serverInfoMap.computeIfAbsent(serverUrl, ServerInfo::new);
    }

    /**
     * Provides access to the underlying map, primarily for observation or advanced scenarios.
     * Use with caution as direct modification outside this registry might break consistency.
     *
     * @return The map of server URLs to ServerInfo objects.
     */
    public Map<String, ServerInfo> getServerInfoMap() {
        return serverInfoMap;
    }

    /**
     * Can be used by a health checking mechanism to update the health status of a server.
     * @param serverUrl The URL of the server.
     * @param isHealthy The new health status.
     */
    public void updateServerHealth(String serverUrl, boolean isHealthy) {
        ServerInfo serverInfo = getOrCreateServerInfo(serverUrl);
        serverInfo.setHealthy(isHealthy);
    }
} 