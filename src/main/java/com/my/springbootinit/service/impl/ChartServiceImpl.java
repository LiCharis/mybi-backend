package com.my.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.my.springbootinit.constant.CommonConstant;
import com.my.springbootinit.mapper.ChartMapper;
import com.my.springbootinit.model.dto.chart.ChartQueryRequest;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.ChartForMongo;
import com.my.springbootinit.repository.ChartRepository;
import com.my.springbootinit.service.ChartService;
import com.my.springbootinit.service.UserService;
import com.my.springbootinit.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Li
 * @description 针对数据信息表【chart(图标信息表)】的数据库操作Service实现
 * @createDate 2023-12-04 19:43:58
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private MongoTemplate mongoTemplate;


    @Resource
    private ChartRepository chartRepository;

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String charType = chartQueryRequest.getCharType();
        Long userId = chartQueryRequest.getUserId();
        String name = chartQueryRequest.getName();
        long current = chartQueryRequest.getCurrent();
        long pageSize = chartQueryRequest.getPageSize();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(charType), "charType", charType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public Page<ChartForMongo> getChartList(ChartQueryRequest chartQueryRequest) {
        long count = chartRepository.count();
        // page size
        // 页号 每一页的大小
        // 这个API的页号是从0开始的
        // 默认按照时间降序
        PageRequest pageRequest = PageRequest.of(chartQueryRequest.getCurrent() - 1, chartQueryRequest.getPageSize(), Sort.by("createTime").descending());
        Long userId = chartQueryRequest.getUserId();

        String name = chartQueryRequest.getName();
        // 查找符合搜索名称的chart
        List<ChartForMongo> charts = null;
        if (StringUtils.isNotBlank(name)) {
            // . 可以重复 0~n次 , 匹配所有满足的name
            String regex = ".*" + name + ".*";
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(userId).and("name").regex(regex));
            query.with(pageRequest);
            charts = mongoTemplate.find(query, ChartForMongo.class);
        } else {
            charts = chartRepository.findAllByUserId(userId,pageRequest);
        }
        return new PageImpl<>(charts, pageRequest, count);
    }

    @Override
    public boolean saveDocument(ChartForMongo chartForMongo) {
        ChartForMongo save = chartRepository.save(chartForMongo);
        return true;
    }

}




