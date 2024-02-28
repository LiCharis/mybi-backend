package com.my.springbootinit.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 黎海旭
 **/
@Slf4j
public class ExcelUtils {
    public static void main(String[] args) throws FileNotFoundException {
        String s = excelToCsv(null);

    }



    public static String excelToCsv(MultipartFile multipartFile) throws FileNotFoundException {

        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误");
            e.printStackTrace();
        }
        System.out.println(list);

        //如果为空
        if (CollUtil.isEmpty(list)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        //转换为csv
        //读取表头
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap) list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        System.out.println(StringUtils.join(headerList, ","));
        builder.append(StringUtils.join(headerList, ",")).append("\n");

        //读取数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            System.out.println(StringUtils.join(dataList, ","));
            builder.append(StringUtils.join(dataList, ",")).append("\n");

        }
        return builder.toString();

    }
}
