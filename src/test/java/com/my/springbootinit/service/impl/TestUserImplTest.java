package com.my.springbootinit.service.impl;

import com.my.springbootinit.model.entity.TestUser;
import com.my.springbootinit.service.TestUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class TestUserImplTest {

    @Resource
    private TestUserRepository  testUserRepository;

    @Test
    void saveUser() {
        TestUser testUser = new TestUser();
        testUser.setId(1L);
        testUser.setUserName("小明");
        testUser.setPassWord("123456");
        testUserRepository.saveUser(testUser);
    }

    @Test
    void findUserByUserName() {
        TestUser user = testUserRepository.findUserByUserName("小明");
        System.out.println("user is " + user);
    }

    @Test
    void updateUser() {
        TestUser user = new TestUser();
        user.setId(1l);
        user.setUserName("天空");
        user.setPassWord("9999999");
        testUserRepository.updateUser(user);

    }

    @Test
    void deleteUserById() {
        testUserRepository.deleteUserById(1l);
    }
}