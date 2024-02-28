package com.my.springbootinit.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


/**
 * @author 黎海旭
 **/
@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

    @Test
    void doChat() {
        String doChat = aiManager.doChat(1654785040361893889L, "你是谁?");
        System.out.println(doChat);
    }
}