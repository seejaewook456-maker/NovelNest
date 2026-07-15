package org.example.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

// 이메일 발송을 요청 스레드와 분리하기 위한 비동기 인프라.
// 이메일 발송 전용 Executor를 따로 두어, 다른 비동기 작업이 생기더라도 서로 큐를 공유하지 않도록 한다.
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.email.core-pool-size}")
    private int corePoolSize;

    @Value("${app.async.email.max-pool-size}")
    private int maxPoolSize;

    @Value("${app.async.email.queue-capacity}")
    private int queueCapacity;

    @Value("${app.async.email.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        // 큐까지 가득 찬 경우 이메일을 유실시키지 않고 호출 스레드에서 직접 실행되도록 하여 자연스러운 backpressure를 건다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 종료 시 진행 중이던 발송 작업이 끝날 때까지 최대 20초 대기 후 종료
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // void 비동기 메서드는 예외가 호출자에게 전달되지 않으므로, 유실되지 않도록 별도로 로그를 남긴다.
        // 이메일 주소/인증번호 등 민감정보는 인자에 포함되어 있어도 절대 출력하지 않는다.
        return (throwable, method, params) -> {
            log.error("Uncaught async exception in method '{}': {}", method.getName(), throwable.getMessage());
            logMethodSignatureOnly(method);
        };
    }

    private void logMethodSignatureOnly(Method method) {
        log.error("Failed async method signature: {}", method);
    }
}
