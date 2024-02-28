package com.my.springbootinit.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.my.springbootinit.model.entity.TestUser;
import com.my.springbootinit.service.TestUserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TestUserImpl implements TestUserRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 创建对象
     * @param testUser
     */
    @Override
    public void saveUser(TestUser testUser) {
        mongoTemplate.save(testUser);
    }


    /**
     * 根据用户名查询对象
     *
     * @param userName
     * @return
     */
    @Override
    public TestUser findUserByUserName(String userName) {
        Query query = new Query(Criteria.where("userName").is(userName));
        TestUser testUser = mongoTemplate.findOne(query, TestUser.class);
        return testUser;
    }



    @Override
    public long updateUser(TestUser user) {
        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update().set("userName", user.getUserName()).set("passWord", user.getPassWord());
        //更新查询返回结果集的第一条
        UpdateResult result = mongoTemplate.updateFirst(query, update, TestUser.class);
        // 更新查询返回结果集的所有
        // mongoTemplate.updateMulti(query,update,UserEntity.class);
        if (result != null) {
            return result.getMatchedCount();
        }
        return 0;
    }

    @Override
    public void deleteUserById(Long id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, TestUser.class);
    }
}
