package com.my.springbootinit.utils;

import com.my.springbootinit.model.dto.ServerLoadInfo;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;


public class ServerMetricsUtil {
    private static OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private static MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    private static ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 获取当前服务器CPU使用占比
     *
     * @return CPU usage percentage.
     */
    public static double getCpuUsagePercentage() {
        return osBean.getProcessCpuLoad() * 100; // Convert to percentage
    }

    /**
     * 获取当前服务器内存使用占比
     *
     * @return Memory usage percentage.
     */
    public static double getMemoryUsagePercentage() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();

        return ((double) usedMemory / maxMemory) * 100; // Convert to percentage
    }


    public static ServerLoadInfo getLoadInfo() {
        double cpuUsagePercentage = getCpuUsagePercentage();
        double memoryUsagePercentage = getMemoryUsagePercentage();
        return new ServerLoadInfo(cpuUsagePercentage,memoryUsagePercentage);
    }
}