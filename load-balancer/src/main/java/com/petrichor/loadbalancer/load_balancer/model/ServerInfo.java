package com.petrichor.loadbalancer.load_balancer.model;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerInfo {
    private String url;
    private AtomicInteger activeConnections;
    private volatile double avgResponseTime;
    private volatile boolean healthy;

    public ServerInfo(String url) {
        this.url = url;
        this.activeConnections = new AtomicInteger(0);
        this.avgResponseTime = 0.0;
        this.healthy = true;
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
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
}