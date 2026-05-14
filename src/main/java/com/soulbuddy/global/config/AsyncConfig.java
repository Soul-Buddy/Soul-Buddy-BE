package com.soulbuddy.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Async 백그라운드 처리용 Executor.
 *
 * 주 용도 (PR-2 v2.3):
 *   - InSessionSummaryService.compactIfNeeded — 80턴 도달 시 가장 오래된 20개를
 *     네이버 요약 API 로 압축. 채팅 응답 지연 0 을 위해 별도 Executor 로 분리.
 *
 * 채팅 응답 자체는 동기 흐름 그대로 유지. 본 Executor 는 백그라운드 압축 전용.
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    public static final String AI_BACKGROUND_EXECUTOR = "aiBackgroundExecutor";

    @Bean(name = AI_BACKGROUND_EXECUTOR)
    public Executor aiBackgroundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-bg-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
