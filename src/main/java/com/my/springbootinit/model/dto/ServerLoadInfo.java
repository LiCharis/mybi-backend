package com.my.springbootinit.model.dto;

import lombok.Data;

/**
 * 储存当前服务求负载状况的对象
 */
@Data
public class ServerLoadInfo {

    /**
     * cpu阈值
     */
    private double cpuUsage;

    /**
     * 内存阈值
     */
    private double memoryUsage;

    public ServerLoadInfo(double cpuUsagePercentage, double memoryUsagePercentage) {
        this.cpuUsage = cpuUsagePercentage;
        this.memoryUsage = memoryUsagePercentage;
    }

    public ServerLoadInfo() {
    }

    /**
     * 非常高负载
     *
     * @return boolean
     */
    public boolean isVeryHighLoad() {
        return cpuUsage > 90 && memoryUsage > 80; // CPU > 90%, 内存 > 80%
    }

    /**
     * 高负载
     *
     * @return boolean
     */
    public boolean isHighLoad() {
        return cpuUsage > 60 || memoryUsage > 40; // CPU > 80% or 内存 > 70%
    }

    /**
     * 中等负荷
     *
     * @return boolean
     */
    public boolean isMediumLoad() {
        return cpuUsage > 30 && memoryUsage > 30; // CPU > 30%, 内存 > 30%
    }
}
