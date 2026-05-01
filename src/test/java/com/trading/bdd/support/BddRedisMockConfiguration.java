package com.trading.bdd.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@TestConfiguration
public class BddRedisMockConfiguration {

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);
    }
}
