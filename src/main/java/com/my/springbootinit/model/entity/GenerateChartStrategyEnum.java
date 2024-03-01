package com.my.springbootinit.model.entity;


public enum GenerateChartStrategyEnum {

    GEN_MQ("generate_mq"),
    GEN_SYNC("generate_sync"),
    GEN_THREAD_POOL("generate_thread_pool"),
    GEN_REJECT("generate_reject");

    private String value;

    GenerateChartStrategyEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
