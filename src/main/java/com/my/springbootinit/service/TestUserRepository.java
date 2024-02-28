package com.my.springbootinit.service;

import com.my.springbootinit.model.entity.TestUser;

public interface TestUserRepository {
    public void saveUser(TestUser testUser);

    public TestUser findUserByUserName(String userName);

    public long updateUser(TestUser user);

    public void deleteUserById(Long id);

}
