package com.aiagent;

import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.engine.AgentExecutionEngine;
import com.aiagent.entity.Agent;
import com.aiagent.repository.AgentRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@ActiveProfiles("test")
public class PerformanceTest {

    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private AgentExecutionEngine agentExecutionEngine;
    
    private Agent testAgent;
    
    @BeforeEach
    public void setup() {
        // 设置租户上下文
        TenantContextHolder.setTenantId(1L);
        
        // 创建测试Agent
        testAgent = new Agent();
        testAgent.setTenantId(1L);
        testAgent.setName("Performance Test Agent");
        testAgent.setAgentCode("performance-test-agent");
        testAgent.setDescription("Agent for performance testing");
        testAgent.setStatus(Agent.AgentStatus.PUBLISHED);
        testAgent.setCreatedBy(1L);
        testAgent = agentRepository.save(testAgent);
    }
    
    @Test
    public void testConcurrentExecution() throws InterruptedException {
        // 测试并发执行性能
        int threadCount = 10;
        int requestsPerThread = 10;
        int totalRequests = threadCount * requestsPerThread;
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalExecutionTime = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();
                        
                        AgentInvokeRequest request = new AgentInvokeRequest();
                        Map<String, Object> inputs = new HashMap<>();
                        inputs.put("message", "Hello, performance test!");
                        request.setInputs(inputs);
                        request.setAsync(false);
                        
                        AgentInvokeResponse response = agentExecutionEngine.invokeAgent(testAgent.getId(), request, "performance-test-" + System.currentTimeMillis(), 1L);
                        
                        if ("SUCCESS".equals(response.getStatus())) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        
                        totalExecutionTime.addAndGet(System.currentTimeMillis() - requestStartTime);
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        // 等待所有请求完成
        latch.await(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 打印性能测试结果
        System.out.println("=== Performance Test Results ===");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Success count: " + successCount.get());
        System.out.println("Failure count: " + failureCount.get());
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average response time: " + (totalExecutionTime.get() / totalRequests) + " ms");
        System.out.println("Throughput: " + (totalRequests * 1000.0 / totalTime) + " requests/second");
        
        executorService.shutdown();
    }
    
    @Test
    public void testResponseTime() {
        // 测试响应时间
        int requestCount = 20;
        long totalExecutionTime = 0;
        
        for (int i = 0; i < requestCount; i++) {
            long startTime = System.currentTimeMillis();
            
            AgentInvokeRequest request = new AgentInvokeRequest();
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("message", "Hello, response time test!" + i);
            request.setInputs(inputs);
            request.setAsync(false);
            
            AgentInvokeResponse response = agentExecutionEngine.invokeAgent(testAgent.getId(), request, "response-time-test-" + i, 1L);
            
            long endTime = System.currentTimeMillis();
            totalExecutionTime += endTime - startTime;
            
            System.out.println("Request " + (i + 1) + " response time: " + (endTime - startTime) + " ms");
        }
        
        // 打印响应时间结果
        System.out.println("=== Response Time Test Results ===");
        System.out.println("Total requests: " + requestCount);
        System.out.println("Total execution time: " + totalExecutionTime + " ms");
        System.out.println("Average response time: " + (totalExecutionTime / requestCount) + " ms");
    }
}
