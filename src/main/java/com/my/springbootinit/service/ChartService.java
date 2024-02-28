package com.my.springbootinit.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.my.springbootinit.model.dto.chart.ChartQueryRequest;
import com.my.springbootinit.model.dto.post.PostQueryRequest;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.ChartForMongo;
import com.my.springbootinit.model.entity.Post;
import org.springframework.data.domain.Page;

/**
* @author Li
* @description 针对表【chart(图标信息表)】的数据库操作Service
* @createDate 2023-12-04 19:43:58
*/
public interface ChartService extends IService<Chart> {

    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);


    /**
     * 获取生成的图表集合
     * @param chartQueryRequest
     * @return
     */
   Page<ChartForMongo> getChartList(ChartQueryRequest chartQueryRequest);

    /**
     * 保存chart文档
     * @param chartForMongo
     * @return
     */
   boolean saveDocument(ChartForMongo chartForMongo);

}
