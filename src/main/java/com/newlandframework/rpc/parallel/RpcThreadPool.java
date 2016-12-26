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
package com.newlandframework.rpc.parallel;

import com.newlandframework.rpc.core.RpcSystemConfig;
import com.newlandframework.rpc.jmx.ThreadPoolMonitorProvider;
import com.newlandframework.rpc.jmx.ThreadPoolStatus;
import com.newlandframework.rpc.parallel.policy.AbortPolicy;
import com.newlandframework.rpc.parallel.policy.BlockingPolicy;
import com.newlandframework.rpc.parallel.policy.CallerRunsPolicy;
import com.newlandframework.rpc.parallel.policy.DiscardedPolicy;
import com.newlandframework.rpc.parallel.policy.RejectedPolicy;
import com.newlandframework.rpc.parallel.policy.RejectedPolicyType;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * @author tangjie<https://github.com/tang-jie>
 * @filename:RpcThreadPool.java
 * @description:RpcThreadPool功能模块
 * @blogs http://www.cnblogs.com/jietang/
 * @since 2016/10/7
 * RPC异步处理线程池
 */
public class RpcThreadPool {
    private static final Timer timer = new Timer("ThreadPoolMonitor", true);
    private static long monitorDelay = 100;
    private static long monitorPeriod = 300;

    /**
     * 由于超出线程范围和队列容量而使执行被阻塞时所使用的的处理程序
     * NettyRPC的线程池模型，当遇到线程池也无法处理的情形的时候，具体的应对措施策略主要有以下策略
     * @return
     */
    private static RejectedExecutionHandler createPolicy() {
        RejectedPolicyType rejectedPolicyType = RejectedPolicyType.fromString(System.getProperty(RpcSystemConfig.SystemPropertyThreadPoolRejectedPolicyAttr, "AbortPolicy"));

        switch (rejectedPolicyType) {
            case BLOCKING_POLICY:
                return new BlockingPolicy();
            /**
             * 这个主要因为过多的并行请求会加剧系统的负载，线程之间的调度操作系统会频繁的进行上下文切换。
             * 当遇到线程池满的情况，与其频繁的切换、中断。不如把并行的请求，全部串行化处理，保证尽量少的延时
             */
            case CALLER_RUNS_POLICY: //不抛弃任务，也不抛出异常，而是调用者自己来运行。
                return new CallerRunsPolicy();
            case ABORT_POLICY: //直接拒绝执行，抛出rejectedExecution异常
                return new AbortPolicy();
            case REJECTED_POLICY:
                return new RejectedPolicy();
            case DISCARDED_POLICY: //从任务队列的头部开始直接丢弃一半的队列元素，为任务队列“减负”
                return new DiscardedPolicy();
        }

        return null;
    }

    /**
     * NettyRPC的线程池支持的任务队列类型主要有以下三种
     * @param queues
     * @return
     */
    private static BlockingQueue<Runnable> createBlockingQueue(int queues) {
        BlockingQueueType queueType = BlockingQueueType.fromString(System.getProperty(RpcSystemConfig.SystemPropertyThreadPoolQueueNameAttr, "LinkedBlockingQueue"));

        switch (queueType) {
            case LINKED_BLOCKING_QUEUE: //采用链表方式实现的无界任务队列，当然你可以额外指定其容量，使其有界
                return new LinkedBlockingQueue<Runnable>();
            case ARRAY_BLOCKING_QUEUE: //有界的数组任务队列
                return new ArrayBlockingQueue<Runnable>(RpcSystemConfig.PARALLEL * queues);
            case SYNCHRONOUS_QUEUE: //任务队列的容量固定为1，当客户端提交执行任务过来的时候，有进行阻塞。直到有个处理线程取走这个待执行的任务，否则会一直阻塞下去。
                return new SynchronousQueue<Runnable>();
        }

        return null;
    }

    /**
     * public ThreadPoolExecutor(int corePoolSize, //池中所保存的线程数，包括空闲线程
                                 int maximumPoolSize, //池中允许的最大线程数
                                 long keepAliveTime, //当线程数大于核心时，此为终止前多于的空闲线程等待新任务的最长时间
                                 TimeUnit unit, //keepAliveTime参数的时间单位
                                 BlockingQueue<Runnable> workQueue, //执行前用于保持任务的队列。此队列仅保持由execute方法提交的Runnable任务
                                 ThreadFactory threadFactory, //执行程序创建新线程时所用的工厂
                                 RejectedExecutionHandler handler) //由于超出线程范围和队列容量而使执行被阻塞时所使用的的处理程序
     * @param threads
     * @param queues
     * @return
     */
    public static Executor getExecutor(int threads, int queues) {
        String name = "RpcThreadPool";
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                createBlockingQueue(queues),
                new NamedThreadFactory(name, true), createPolicy());
        return executor;
    }

    public static Executor getExecutorWithJmx(int threads, int queues) {
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) getExecutor(threads, queues);
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                ThreadPoolStatus status = new ThreadPoolStatus();
                status.setPoolSize(executor.getPoolSize());
                status.setActiveCount(executor.getActiveCount());
                status.setCorePoolSize(executor.getCorePoolSize());
                status.setMaximumPoolSize(executor.getMaximumPoolSize());
                status.setLargestPoolSize(executor.getLargestPoolSize());
                status.setTaskCount(executor.getTaskCount());
                status.setCompletedTaskCount(executor.getCompletedTaskCount());

                try {
                    ThreadPoolMonitorProvider.monitor(status);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (MalformedObjectNameException e) {
                    e.printStackTrace();
                } catch (ReflectionException e) {
                    e.printStackTrace();
                } catch (MBeanException e) {
                    e.printStackTrace();
                } catch (InstanceNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }, monitorDelay, monitorDelay);
        return executor;
    }
}

