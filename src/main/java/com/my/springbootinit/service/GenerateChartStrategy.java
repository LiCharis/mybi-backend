package com.my.springbootinit.service;

import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.model.entity.Chart;

import java.io.IOException;

public interface GenerateChartStrategy {

    /**
     * 选择图表生成策略
     * @param chart
     * @return
     */
    BaseResponse  executeGenChart(Chart chart);
}
