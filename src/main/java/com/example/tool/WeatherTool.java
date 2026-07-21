package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class WeatherTool {

    @Tool(name = "get_weather", description = "查询指定城市的实时天气信息，返回温度、湿度、天气状况和风力" ,readOnly = true )
    public String getWeather(
            @ToolParam(name = "city", description = "城市名称，例如 北京、上海、杭州") String city
    ) {
        // 模拟天气数据，实际项目中可替换为真实的天气 API 调用
        return String.format("""
                城市: %s
                温度: 26°C
                天气: 多云转晴
                湿度: 65%%
                风力: 东北风 3级
                更新时间: 2026-07-21 14:00""", city);
    }

    @Tool(name = "get_forecast",description = "查询指定城市未来几天的天气",readOnly = true)
    public String getForecast(
            @ToolParam(name ="city" ,description = "城市名称，如北京、上海、杭州") String city,
            @ToolParam(name = "days", description ="预报的天数 1-7") Integer days
    ){
        if( days == null)   days = 3 ;
        return String.format(
                """
                        {
                            "city": "%s",
                            "forecast": [
                                {"day": "今天", "temperature": "26-32°C", "condition": "多云"},
                                {"day": "明天", "temperature": "25-30°C", "condition": "阵雨"},
                                {"day": "后天", "temperature": "24-29°C", "condition": "阴"}
                            ]
                        }
                        """, city
        );
    }


}
