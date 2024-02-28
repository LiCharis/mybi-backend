package com.my.springbootinit.model.vo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图标信息表VO
 * @TableName chart
 */
@Data
public class ChartVO implements Serializable {

    /**
     * id
     */
    @JsonSerialize(using= ToStringSerializer.class)
    private long id;

    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结论
     */
    private String genResult;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}