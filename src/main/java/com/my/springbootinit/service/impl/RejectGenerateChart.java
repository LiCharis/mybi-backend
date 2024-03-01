package com.my.springbootinit.service.impl;


import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.exception.BusinessException;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.service.GenerateChartStrategy;
import org.springframework.stereotype.Service;

@Service(value = "generate_reject")
public class RejectGenerateChart implements GenerateChartStrategy {
    @Override
    public BaseResponse executeGenChart(Chart chart) {
        throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "服务器繁忙,请稍后重试!");
    }
}
