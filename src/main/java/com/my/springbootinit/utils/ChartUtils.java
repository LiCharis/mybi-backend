package com.my.springbootinit.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.my.springbootinit.model.entity.Chart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;



@Slf4j
public class ChartUtils {


    /**
     * 压缩json
     *
     * @param data 数据
     * @return {@link String}
     */
    @NotNull
    public static String compressJson(String data) {
        data = data.replaceAll("\t+", "");
        data = data.replaceAll(" +", "");
        data = data.replaceAll("\n+", "");
        return data;
    }

    /**
     * 处理拼接输入
     * @param chart
     * @return
     */
    @NotNull
    public static String buildUserInput(Chart chart) {
        StringBuilder userInput = new StringBuilder();
        String goal = chart.getGoal();
        String chartData = chart.getChartData();
        String chartType = chart.getCharType();

        userInput.append("分析需求:");
        if (StringUtils.isNotBlank(chartType)) {
            goal += ",请使用" + chartType;
        }
        userInput.append(goal).append("\n");
        userInput.append("原始数据:\n").append(chartData).append("\n");
        return userInput.toString();

    }

    /**
     * 选择分析图表的模型
     */
    public static String getModelResponse(String modelName,String userInput){
        String response = null;
        String url = null;
        if (modelName.equals("ChatGPT")){
           url = "http://101.43.233.52:8000/api/task/chartByGPT";
        }else if (modelName.equals("Qwen")){
            url = "http://101.43.233.52:8000/api/task/chartByQwen";
        }
        HttpResponse httpResponse = HttpRequest.post(url)
                .form("input",userInput)
                .execute();
        response = httpResponse.body();
        return response;
    }



}
