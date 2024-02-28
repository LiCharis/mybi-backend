package com.my.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.my.springbootinit.annotation.AuthCheck;
import com.my.springbootinit.api.AiManager;
import com.my.springbootinit.common.BaseResponse;
import com.my.springbootinit.common.DeleteRequest;
import com.my.springbootinit.common.ErrorCode;
import com.my.springbootinit.common.ResultUtils;
import com.my.springbootinit.constant.UserConstant;
import com.my.springbootinit.exception.BusinessException;
import com.my.springbootinit.exception.ThrowUtils;
import com.my.springbootinit.manager.RedisLimiterManager;
import com.my.springbootinit.model.dto.chart.*;
import com.my.springbootinit.model.entity.Chart;
import com.my.springbootinit.model.entity.User;
import com.my.springbootinit.model.enums.FileUploadBizEnum;
import com.my.springbootinit.model.vo.ChartVO;
import com.my.springbootinit.service.ChartService;
import com.my.springbootinit.service.UserService;
import com.my.springbootinit.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 测试队列接口
 *

 */
@RestController
@RequestMapping("/queue")
@Profile({"dev", "local"})  //本接口只对本机或者开发环境有效
@Slf4j
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name) {
        CompletableFuture.runAsync(() -> {
            System.out.println("任务执行中" + name);
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }, threadPoolExecutor);
        //将任务放入到自己的线程池中，
        // 不然会放到默认的线程池ForkJoinPool,多个任务都放在该线程池下可能会有阻塞冲突
    }

    @GetMapping("/get")
    public String get() {
        HashMap<String, Object> hashMap = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        hashMap.put("队列长度", size);
        long taskCount = threadPoolExecutor.getTaskCount();
        hashMap.put("任务总数", taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        hashMap.put("已完成的线程数", completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        hashMap.put("正在工作的线程数", activeCount);
        return JSONUtil.toJsonStr(hashMap);


    }

}
