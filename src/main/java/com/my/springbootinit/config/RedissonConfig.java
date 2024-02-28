package com.my.springbootinit.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 黎海旭
 **/
@Data
@ConfigurationProperties(prefix = "spring.redis")
@Configuration
public class RedissonConfig {

    private Integer dataBase;
    private String host;
    private Integer port;

    @Bean
    public RedissonClient getRedissonClient() {
        Config config = new Config();
        //单机
        config.useSingleServer()
                .setDatabase(dataBase)
                .setAddress("redis://" + host + ":" + port);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
