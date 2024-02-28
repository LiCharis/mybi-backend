package com.my.springbootinit.repository;

import com.my.springbootinit.model.entity.ChartForMongo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ChartRepository extends MongoRepository<ChartForMongo, String> {
    @Query("{'userId': ?0}")
    List<ChartForMongo> findAllByUserId(long userId, Pageable pageable);

    long deleteAllByChartId(long chartId);

    @Query("{'chartId': ?0}")
    List<ChartForMongo> findAllByChartId(long chartId);
}
