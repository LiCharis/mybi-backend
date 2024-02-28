package com.my.springbootinit.model.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document("chart")
public class ChartForMongo {

    public static final String COLLECTION_NAME = "chart";
    /**
     * MongoDB自动生成的唯一ID
     */
    @Id
    private String id;

    /**
     * 图表id
     */
    @Indexed
    @JsonSerialize(using= ToStringSerializer.class)
    private Long chartId;


    /**
     * 用户ID
     */
    @Indexed
    @JsonSerialize(using=ToStringSerializer.class)
    private Long userId;

    /**
     * 表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String charType;

    /**
     * wait,running,succeed,failed
     */
    private Integer status;

    /**
     * 执行信息
     */
    private String message;


    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结论
     */
    private String genResult;

    /**
     * 创建时间
     */
    private Date createTime;


    private static final long serialVersionUID = 1L;
}
