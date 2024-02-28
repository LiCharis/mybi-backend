package com.my.springbootinit.bimq;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mongodb.client.result.UpdateResult;
import com.my.springbootinit.api.AiManager;
import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.exception.BusinessException;
import com.my.springbootinit.exception.GenChartException;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.ChartForMongo;
import com.my.springbootinit.model.enums.StateEnum;
import com.my.springbootinit.model.vo.ChartVO;
import com.my.springbootinit.repository.ChartRepository;
import com.my.springbootinit.service.ChartService;
import com.my.springbootinit.utils.ChartUtils;
import com.my.springbootinit.websocket.WebSocketServer;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.my.springbootinit.utils.ChartUtils.buildUserInput;

/**
 * @author 黎海旭
 **/
@Component
@Slf4j
public class BiMessageConsumer {

    @Value("${model.modelName}")
    private String modelName;


    @Value("${yuapi.modelId}")
    private Long modelId;

    @Resource
    private AiManager aiManager;

    @Resource
    private ChartService chartService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private WebSocketServer webSocketServer;

    //指定程序监听的消息队列和确认机制
//    @RabbitListener(queues = {BiMqConstant.QUEUE_NAME}, ackMode = "MANUAL")
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = BiMqConstant.QUEUE_NAME), exchange = @Exchange(name = BiMqConstant.EXCHANGE_NAME, type = ExchangeTypes.DIRECT), key = BiMqConstant.ROUTE_KEY))
    @Retryable(value = GenChartException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000 * 30))
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receiveMessage message = {}", message);

        //进入到这一步先修改图标状态为执行中(1),等执行成功后再改为执行成功(2),保存修改结果;失败则改为执行失败(3),记录失败信息

        /**
         * 取到传过来的消息(chartId)，执行ai服务
         */

        if (StringUtils.isBlank(message)) {
            //消息为空，消费失败
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);

        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            //图表为空，消费失败
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }


        //设置状态为执行中
        Chart genChart1 = new Chart();
        genChart1.setId(chartId);
        genChart1.setStatus(StateEnum.RUNNING.getValue());
        boolean save1 = chartService.updateById(genChart1);
        if (!save1) {
            handleChartUpdateError(chartId, "更新图表'执行中'状态更新失败!");
            throw new GenChartException(ErrorCode.SYSTEM_ERROR);
        }

        String userInput = buildUserInput(chart);

        String response = null;
        try {
//            response = aiManager.doChat(modelId, userInput);

            response = ChartUtils.getModelResponse(modelName,userInput);

        } catch (Exception e) {
            //AI 服务错误，消费失败
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chartId, "AI 服务错误!");
            throw new GenChartException(ErrorCode.SYSTEM_ERROR);
        }
        System.out.println(response);
        /**
         * 正则表达式将结果处理一下
         */
        String pattern1 = "#+"; // 匹配一个或多个 "#"
        String replacement1 = "#####"; // 替换为五个 "#"

        Pattern p1 = Pattern.compile(pattern1);
        Matcher m1 = p1.matcher(response);
        String output = m1.replaceAll(replacement1);
        String[] split = output.split("#####");
        if (split.length < 3) {
            //AI 生成格式错误，消费失败
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chartId, "AI 生成格式错误!");
            throw new GenChartException(ErrorCode.SYSTEM_ERROR);
        }

        String genChart = split[1].trim();

        genChart = genChart.replaceAll("```|json|javascript","");

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
        Query query= new Query(Criteria.where("chartId").is(chartId));
        Update update= new Update()
                .set("genResult", genResult)
                .set("genChart", genChart)
                .set("status",StateEnum.SUCCESS.getValue())
                .set("message",StateEnum.SUCCESS.getText())
                .set("createTime",chart.getUpdateTime());
        //更新查询满足条件的文档数据（全部）
        UpdateResult result = mongoTemplate.updateMulti(query, update, ChartForMongo.class);
        System.out.println("更新条数：" + result.getMatchedCount());

//        /**
//         *更新mysql图表数据
//         */
//        //设置状态为成功
//        boolean save = chartService.updateById(updateChart);
//        if (!save) {
//            channel.basicNack(deliveryTag, false, false);
//            handleChartUpdateError(chartId, "更新图表'成功'状态失败!");
//            throw new GenChartException(ErrorCode.SYSTEM_ERROR);
//        }
        //手动进行消息确认，也就是到这步就消费成功了
        channel.basicAck(deliveryTag, false);
        webSocketServer.sendMessage("您的[" + chart.getName() + "]生成成功 , 前往 我的图表 进行查看", new HashSet<>(Arrays.asList(chart.getUserId().toString())));


    }

    /**
     * 处理图表生成失败
     *
     * @param chartId
     * @param message
     */

    private void handleChartUpdateError(long chartId, String message) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(StateEnum.FAIL.getValue());
        updateChartResult.setMessage(message);
        Query query= new Query(Criteria.where("chartId").is(chartId));
        Update update= new Update()
                .set("status",StateEnum.FAIL.getValue())
                .set("message",message);
        //更新查询满足条件的文档数据（全部）
        UpdateResult result = mongoTemplate.updateMulti(query, update, ChartForMongo.class);
        boolean updateById = chartService.updateById(updateChartResult);
        if (!updateById) {
            log.error("图标更新状态失败, " + chartId + " ," + message);
        }
    }

    /**
     * 超过最重试次数上限
     *
     * @param e e
     */
    @Recover
    public void recoverFromMaxAttempts(GenChartException e) {
        boolean updateResult = chartService.update()
                .eq("id", e.getChartId())
                .set("status", StateEnum.FAIL.getValue())
                .set("execMessage", "图表生成失败,系统已重试多次,请检查您的需求或数据。")
                .update();
        log.info(String.format("图表ID:%d 已超过最大重试次数, 已更新图表执行信息", e.getChartId()));
    }


}
