package com.my.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


/**
 * @author 黎海旭
 **/
@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doReteLimit() throws InterruptedException {
        String userId = "1";
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doReteLimit(userId);
            System.out.println("成功!");
        }

        Thread.sleep(1000);

        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doReteLimit(userId);
            System.out.println("成功!");
        }
    }
}