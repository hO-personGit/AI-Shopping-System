package org.example.springboot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地一级缓存配置（Caffeine）。
 *
 * <p>缓存设计（两级缓存，面试/架构要点）：
 * <ul>
 *   <li>一级缓存：Caffeine 本地缓存，毫秒级访问、进程内共享，适合读多写少的热点数据。</li>
 *   <li>二级缓存：Redis 分布式缓存，多实例共享，缓存穿透 / 击穿 / 雪崩均做了兜底策略。</li>
 * </ul>
 * 配置切换：spring.cache.type=caffeine（本地）/ redis（分布式），默认本地缓存保证无 Redis 也能运行演示。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 构建带统计功能的 Caffeine 缓存规格。
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats(); // 开启统计功能
    }

    /**
     * Caffeine 缓存管理器（默认，无需 Redis 即可运行演示）。
     * 切换分布式缓存：application.properties 设置 spring.cache.type=redis。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager(Caffeine<Object, Object> caffeineConfig) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeineConfig);
        return manager;
    }
}
