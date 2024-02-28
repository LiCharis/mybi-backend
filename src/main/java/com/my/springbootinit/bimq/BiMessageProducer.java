package com.my.springbootinit.bimq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 黎海旭
 **/
@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sandMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.EXCHANGE_NAME, BiMqConstant.ROUTE_KEY, message);
    }
}
