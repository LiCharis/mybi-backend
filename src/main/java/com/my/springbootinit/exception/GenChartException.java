package com.my.springbootinit.exception;

import com.my.springbootinit.common.ErrorCode;

public class GenChartException extends BusinessException {
    /**
     * 图表id
     */
    private Long chartId;

    public GenChartException(int code, String message) {
        super(code, message);
    }

    public GenChartException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public GenChartException(ErrorCode errorCode) {
        super(errorCode);
    }


    public GenChartException() {
        super(ErrorCode.SYSTEM_ERROR);
    }

    public Long getChartId() {
        return chartId;
    }

    public void setChartId(Long chartId) {
        this.chartId = chartId;
    }
}
