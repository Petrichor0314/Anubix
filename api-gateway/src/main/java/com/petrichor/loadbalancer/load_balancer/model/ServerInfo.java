package com.petrichor.loadbalancer.load_balancer.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerInfo {
    private final String url;
    private final AtomicInteger activeConnections;
    private final AtomicReference<Double> avgResponseTime;
    private final AtomicBoolean healthy;

    public ServerInfo(String url) {
        this.url = url;
        this.activeConnections = new AtomicInteger(0);
        this.avgResponseTime = new AtomicReference<>(0.0);
        this.healthy = new AtomicBoolean(true);
    }

    public String getUrl() {
        return url;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public double getAvgResponseTime() {
        return avgResponseTime.get();
    }

    public void setAvgResponseTime(double newTime) {
        avgResponseTime.set(newTime);
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public void setHealthy(boolean isHealthy) {
        healthy.set(isHealthy);
    }
}
