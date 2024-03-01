package com.my.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.google.gson.Gson;
import com.my.springbootinit.annotation.AuthCheck;
import com.my.springbootinit.api.AiManager;
import com.my.springbootinit.bimq.BiMessageConsumer;
import com.my.springbootinit.bimq.BiMessageProducer;
import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.common.DeleteRequest;
import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.common.ResultUtils;
import com.my.springbootinit.constant.UserConstant;
import com.my.springbootinit.exception.BusinessException;
import com.my.springbootinit.exception.ThrowUtils;
import com.my.springbootinit.manager.RedisLimiterManager;
import com.my.springbootinit.model.dto.ServerLoadInfo;
import com.my.springbootinit.model.dto.chart.*;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.ChartForMongo;
import com.my.springbootinit.model.entity.User;

import com.my.springbootinit.model.enums.FileUploadBizEnum;
import com.my.springbootinit.model.enums.StateEnum;
import com.my.springbootinit.model.vo.ChartVO;
import com.my.springbootinit.repository.ChartRepository;
import com.my.springbootinit.service.ChartService;
import com.my.springbootinit.service.UserService;
import com.my.springbootinit.utils.ExcelUtils;
import com.my.springbootinit.utils.ServerMetricsUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Value("${yuapi.modelId}")
    private Long modelId;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private AiManager aiManager;

    @Resource
    private ChartService chartService;

    @Resource
    private ChartRepository chartRepository;

    @Resource
    private UserService userService;

    @Resource
    private BiMessageProducer biMessageProducer;


    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        long l = chartRepository.deleteAllByChartId(id);
        return ResultUtils.success(b && l > 0);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 重新生成图表
     *
     * @param chartId
     * @return
     */
    @GetMapping("/regen/chart")
    @Operation(summary = "重新生成图表")
    public BaseResponse<Boolean> regenerateChart(@RequestParam("chartId") Long chartId, HttpServletRequest request) {

        // 取出数据
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart.getChartData().length() > 1000, ErrorCode.SYSTEM_ERROR, "原始信息过长!");
        // 获取用户信息
        User loginUser = userService.getLoginUser(request);

        redisLimiterManager.doReteLimit("generateByAi_" + loginUser.getId());
        chart.setStatus(StateEnum.WAITING.getValue());
        // 更新状态信息
        boolean updateById = chartService.updateById(chart);
        ThrowUtils.throwIf(!updateById, ErrorCode.SYSTEM_ERROR, "重新生成图表失败");
        // 2. send to rabbitMQ
        long newChartId = chart.getId();
        biMessageProducer.sandMessage(String.valueOf(newChartId));
        return ResultUtils.success(true);
    }


    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ChartForMongo>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                             HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<ChartForMongo> chartPage =
                chartService.getChartList(chartQueryRequest);
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<ChartForMongo>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                               HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<ChartForMongo> chartPage =
                chartService.getChartList(chartQueryRequest);
        return ResultUtils.success(chartPage);
    }


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 文件上传,智能分析(提供API调用)
     *
     * @param input
     * @return
     */
    @PostMapping(value = "/generateByAPI")
    public BaseResponse<String> generateByAPI(String input) {

        /**
         * 对用户做限流,及针对某用户对调用AI请求做出限制
         */
        redisLimiterManager.doReteLimit("generateByAi");
        try {
            String response = aiManager.doChat(modelId, input);
            System.out.println(response);
            return ResultUtils.success(response);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口调用失败");
        }
    }


    /**
     * 文件上传,智能分析(消息队列)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/generate/mq")
    public BaseResponse generateByAiMq(@RequestPart(name = "file", required = false) MultipartFile multipartFile,
                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        /**
         *  校验
         */
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "请求为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(goal) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        /**
         * 校验文件大小
         */
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大，超过1MB");
        /**
         * 校验文件后缀名 todo (当然不排除用户直接改后缀名的可能性，这种情况下校验是没用的，可以再优化)
         */
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        /**
         * 对用户做限流,及针对某用户对调用AI请求做出限制
         */
        redisLimiterManager.doReteLimit("generateByAi_" + loginUser.getId());


//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下格式给你提供内容:\n" +
//                "分析需求:\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据:\n" +
//                "{csv格式的原始数据，用,作为数据分割符}\n" +
//                "请根据这两部分内容，按照以下格式生成内容(此外不要输出任何多余的开头，结尾或者注释)\n" +
//                "==========\n" +
//                "{前端Echarts V5 的option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容比如注释等}\n" +
//                "==========\n" +
//                "{明确的数据分析结论，越详细越好，不要生成多余的注释}";


        try {

            String csvResult = ExcelUtils.excelToCsv(multipartFile);

            /**
             * 插入数据到数据库
             */
            Chart chart = new Chart();
            chart.setName(name);
            chart.setGoal(goal);
            chart.setChartData(csvResult);
            chart.setCharType(chartType);
            //等待状态
            chart.setStatus(StateEnum.WAITING.getValue());
            chart.setUserId(loginUser.getId());
            boolean save = chartService.save(chart);
            if (!save) {
                handleChartUpdateError(chart.getId(), "数据信息表保存错误");
            }
            //将结果存放到mongodb
            ChartForMongo chartForMongo = new ChartForMongo();
            chartForMongo.setMessage(chart.getMessage());
            chartForMongo.setChartId(chart.getId());
            chartForMongo.setStatus(chart.getStatus());
            chartForMongo.setUserId(chart.getUserId());
            chartForMongo.setName(chart.getName());
            chartForMongo.setCharType(chart.getCharType());
            chartForMongo.setGoal(chart.getGoal());
            Chart chart1 = chartService.getById(chart.getId());
            chartForMongo.setCreateTime(chart1.getCreateTime());
            chartService.saveDocument(chartForMongo);


            //todo 这里可以改成根据反向压力设计成策略模式动态选取同步执行、线程池异步、消息队列异步或者拒绝策略(服务器压力过大)

            /**
             * 根据反向压力，策略模式选择对应的处理模式
             */
            //为了演示，可以根据前端选择的方案来写选择具体的执行策略
            ServerLoadInfo info = new ServerLoadInfo();
            String strategy = genChartByAiRequest.getStrategy();
            if (strategy.equals("smartSelect")){
                info = ServerMetricsUtil.getLoadInfo();
            } else if (strategy.equals("rabbitMQ")) {
                info.setCpuUsage(70);
                info.setMemoryUsage(70);
            } else if (strategy.equals("threadPool")) {
                info.setCpuUsage(40);
                info.setMemoryUsage(40);
            } else if (strategy.equals("synchronize")) {
                info.setCpuUsage(20);
                info.setMemoryUsage(20);
            }
            BaseResponse baseResponse = chartService.genChart(chart1, info);


            return baseResponse;
        } catch (Exception e) {

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }

    private void handleChartUpdateError(long chartId, String message) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(3);
        updateChartResult.setMessage(message);
        boolean updateById = chartService.updateById(updateChartResult);
        if (!updateById) {
            log.error("图标更新状态失败, " + chartId + " ," + message);
        }
    }


    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }


}
