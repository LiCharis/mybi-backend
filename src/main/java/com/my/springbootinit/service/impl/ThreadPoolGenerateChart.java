package com.my.springbootinit.service.impl;


import com.mongodb.client.result.UpdateResult;
import com.my.springbootinit.api.AiManager;
import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.common.ResultUtils;
import com.my.springbootinit.exception.BusinessException;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.ChartForMongo;
import com.my.springbootinit.model.enums.StateEnum;
import com.my.springbootinit.service.ChartService;
import com.my.springbootinit.service.GenerateChartStrategy;
import com.my.springbootinit.utils.ChartUtils;
import com.my.springbootinit.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.my.springbootinit.utils.ChartUtils.buildUserInput;

@Service(value = "generate_thread_pool")
@Slf4j
public class ThreadPoolGenerateChart implements GenerateChartStrategy {

    @Value("${model.modelName}")
    private String modelName;

    @Resource
    private ChartService chartService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    WebSocketServer webSocketServer;

    @Value("${model.modelId}")
    private Long modelId;

    @Resource
    private AiManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public BaseResponse executeGenChart(Chart chart){


        try {
            CompletableFuture.runAsync(() -> {
                if (chart == null) {
                    //图表为空，消费失败
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
                }

                Long chartId = chart.getId();


                //设置状态为执行中
                Chart genChart1 = new Chart();
                genChart1.setId(chartId);
                genChart1.setStatus(StateEnum.RUNNING.getValue());
                boolean save1 = chartService.updateById(genChart1);
                if (!save1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表'执行中'状态更新失败!");
                }

                String userInput = buildUserInput(chart);

                String response = null;
                try {
                    //            response = aiManager.doChat(modelId, userInput);

                    response = aiManager.doChat(modelId, userInput);

                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务错误!");
                }
                System.out.println(response);
                /**
                 * 正则表达式将结果处理一下
                 */
                String pattern1 = "【+"; // 匹配一个或多个 "【"
                String replacement1 = "【【【【【"; // 替换为五个 "【"

                Pattern p1 = Pattern.compile(pattern1);
                Matcher m1 = p1.matcher(response);
                String output = m1.replaceAll(replacement1);
                String[] split = output.split("【【【【【");
                if (split.length < 3) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成格式错误!");
                }

                String genChart = split[1].trim();

                genChart = genChart.replaceAll("```|json|javascript", "");

                String genResult = split[2].trim();
                /**
                 * 压缩生成的json字符串
                 */
                genChart = ChartUtils.compressJson(genChart);
//        Chart updateChart = new Chart();
//        updateChart.setId(chartId);
//        updateChart.setStatus(StateEnum.SUCCESS.getValue());
//        updateChart.setMessage(StateEnum.SUCCESS.getText());
                /**
                 * 将生成的结果存放到Mongodb中
                 * 局部更新
                 */
                Query query = new Query(Criteria.where("chartId").is(chartId));
                Update update = new Update()
                        .set("genResult", genResult)
                        .set("genChart", genChart)
                        .set("status", StateEnum.SUCCESS.getValue())
                        .set("message", StateEnum.SUCCESS.getText())
                        .set("createTime", chart.getUpdateTime());
                //更新查询满足条件的文档数据（全部）
                UpdateResult result = mongoTemplate.updateMulti(query, update, ChartForMongo.class);
                System.out.println("更新条数：" + result.getMatchedCount());
                //将结果返回
                ChartForMongo chartForMongo = new ChartForMongo();
                chartForMongo.setGenChart(genChart);
                chartForMongo.setGenResult(genResult);
                try {
                    webSocketServer.sendMessage("您的[" + chart.getName() + "]生成成功 , 前往 我的图表 进行查看", new HashSet<>(Arrays.asList(chart.getUserId().toString())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },threadPoolExecutor);

        } catch (BusinessException e) {
            Long chartId = chart.getId();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setStatus(StateEnum.FAIL.getValue());
            String errorMessage = e.getMessage();
            updateChartResult.setMessage(errorMessage);
            Query query = new Query(Criteria.where("chartId").is(chartId));
            Update update = new Update()
                    .set("status", StateEnum.FAIL.getValue())
                    .set("message", errorMessage);
            //更新查询满足条件的文档数据（全部）
            UpdateResult result = mongoTemplate.updateMulti(query, update, ChartForMongo.class);
            boolean updateById = chartService.updateById(updateChartResult);
            if (!updateById) {
                log.error("图标更新状态失败, " + chartId + " ," + errorMessage);
            }
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,e.getMessage());

        }

        return ResultUtils.success(chart.getId());
    }

}
