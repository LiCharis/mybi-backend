package com.my.springbootinit.manager;

import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 黎海旭
 **/
@Service
public class RedisLimiterManager {
    @Resource
    private RedissonClient redissonClient;

    public void doReteLimit(String key){
        //创建一个名称为user_limiter的限流器，每秒最多能访问2次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

        //每当一个操作来了后，就请求一个令牌
        boolean acquire = rateLimiter.tryAcquire(1);
        if (!acquire){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }

}
