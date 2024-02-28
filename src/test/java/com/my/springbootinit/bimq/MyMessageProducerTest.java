package com.my.springbootinit.bimq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author 黎海旭
 **/
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private BiMessageProducer myMessageProducer;

    @Test
    void sandMessage() {
        myMessageProducer.sandMessage("你好啊!");
    }
}