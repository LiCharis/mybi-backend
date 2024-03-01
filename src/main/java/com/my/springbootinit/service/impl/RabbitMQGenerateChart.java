package com.my.springbootinit.service.impl;

import com.my.springbootinit.bimq.BiMessageProducer;
import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.common.ResultUtils;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.service.GenerateChartStrategy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service(value = "generate_mq")
public class RabbitMQGenerateChart implements GenerateChartStrategy {

    @Resource
    private BiMessageProducer biMessageProducer;

    @Override
    public BaseResponse executeGenChart(Chart chart) {
        Long newChartId = chart.getId();
        biMessageProducer.sandMessage(String.valueOf(newChartId));
        return ResultUtils.success(newChartId);
    }
}
