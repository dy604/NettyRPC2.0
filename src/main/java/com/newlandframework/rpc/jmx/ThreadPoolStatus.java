/**
 * Copyright (C) 2016 Newland Group Holding Limited
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newlandframework.rpc.jmx;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * @author tangjie<https://github.com/tang-jie>
 * @filename:ThreadPoolStatus.java
 * @description:ThreadPoolStatus功能模块
 * @blogs http://www.cnblogs.com/jietang/
 * @since 2016/10/13
 * Spring通过annotation注解注册MBean到JMX实现监控Java运行状态
 * 线程池状态监控类
 */

@ManagedResource //将类的所有实例标识为JMX（Java Management Extensions，即Java管理扩展）受控资源
public class ThreadPoolStatus {
    private int poolSize;  //池中的当前线程数
    private int activeCount;  //主动执行任务的近似线程数
    private int corePoolSize;  //核心线程数
    private int maximumPoolSize;  //允许的最大线程数
    private int largestPoolSize;  //历史最大的线程数
    private long taskCount;  //曾计划执行的近似任务总数
    private long completedTaskCount;  //已完成执行的近似任务总数

    @ManagedOperation //将方法标识为JMX操作
    public int getPoolSize() {
        return poolSize;
    }

    @ManagedOperation
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @ManagedOperation
    public int getActiveCount() {
        return activeCount;
    }

    @ManagedOperation
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    @ManagedOperation
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @ManagedOperation
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    @ManagedOperation
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    @ManagedOperation
    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    @ManagedOperation
    public int getLargestPoolSize() {
        return largestPoolSize;
    }

    @ManagedOperation
    public void setLargestPoolSize(int largestPoolSize) {
        this.largestPoolSize = largestPoolSize;
    }

    @ManagedOperation
    public long getTaskCount() {
        return taskCount;
    }

    @ManagedOperation
    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }

    @ManagedOperation
    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    @ManagedOperation
    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }
}

