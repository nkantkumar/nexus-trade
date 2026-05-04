package com.trading.bdd.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Mockito cannot mock {@link StringRedisTemplate}; BDD uses Redis via Testcontainers (requires Docker).
 */
@TestConfiguration(proxyBeanMethods = false)
public class BddRedisContainerConfiguration {

    static {
        TestcontainersDockerBootstrap.ensureDockerHostConfigured();
    }

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    private static synchronized void ensureRedisStarted() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    @Bean
    @Primary
    LettuceConnectionFactory redisConnectionFactory() {
        ensureRedisStarted();
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
