package com.my.springbootinit.config;

import com.my.springbootinit.model.dto.ServerLoadInfo;
import com.my.springbootinit.model.entity.GenerateChartStrategyEnum;
import com.my.springbootinit.service.GenerateChartStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class StrategySelector {

    /**
     * Spring会自动将strategy接口的实现类注入到这个Map中，key为bean id,value值则为对应的策略实现类
     */
    @Resource
    Map<String,  GenerateChartStrategy> strategyMap;
   
    /**
     * 选择对应的生成图表执行策略
     *
     * @param info 服务器当前负载信息
     * @return {@link  GenerateChartStrategy}
     */
    public  GenerateChartStrategy selectStrategy(ServerLoadInfo info) {
        if (info.isVeryHighLoad()) {
            return strategyMap.get( GenerateChartStrategyEnum.GEN_REJECT.getValue());
        } else if (info.isHighLoad()) {
            return strategyMap.get( GenerateChartStrategyEnum.GEN_MQ.getValue());
        } else if (info.isMediumLoad()) {
            return strategyMap.get( GenerateChartStrategyEnum.GEN_THREAD_POOL.getValue());
        } else {
            return strategyMap.get( GenerateChartStrategyEnum.GEN_SYNC.getValue());
        }
    }


}
