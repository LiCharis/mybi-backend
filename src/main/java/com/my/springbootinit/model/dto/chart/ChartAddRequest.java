package com.my.springbootinit.model.dto.chart;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建请求
 *

 */
@Data
public class ChartAddRequest implements Serializable {

    /**
     * 图表名字
     */
    private String name;


    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String charType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}