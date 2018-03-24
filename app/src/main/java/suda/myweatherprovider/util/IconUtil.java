package suda.myweatherprovider.util;

import android.text.TextUtils;

import java.util.Calendar;
import java.util.HashMap;

import lineageos.providers.WeatherContract;

/**
 * Created by ghbha on 2016/5/2.
 */
public class IconUtil {


    public static int getWeatherCodeByType(String type) {
        return getWeatherCodeByType(type, "", "");
    }


    /**
     * 获取天气状态码
     *
     * @param type
     * @return
     */
    public static int getWeatherCodeByType(String type, String sunRise, String sunDown) {

        int riseHour = 6;
        int downHour = 19;
        if (!TextUtils.isEmpty(sunRise) && !TextUtils.isEmpty(sunDown)) {
            riseHour = Integer.parseInt(sunRise.split(":")[0]);
            downHour = Integer.parseInt(sunDown.split(":")[0]);
        }
        //晚间图标
        Calendar date = Calendar.getInstance();
        int hour = date.get(Calendar.HOUR_OF_DAY);
        if (hour <= riseHour || hour >= downHour) {
            for (String w : IconUtil.WEATHER_HAVE_NIGHT) {
                if (w.equals(type)) {
                    type = type + "晚";
                    break;
                }
            }
        }

        if (IconUtil.ICON_MAPPING.get(type) != null)
            return IconUtil.ICON_MAPPING.get(type);
        return WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
    }

    public static final String[] WEATHER_HAVE_NIGHT = {"晴", "多云"};

    public static final HashMap<String, Integer> ICON_MAPPING = new HashMap<>();

    static {
        //默认44个
        //       ICON_MAPPING.put("龙卷风", WeatherContract.WeatherColumns.WeatherCode.TORNADO);
//        ICON_MAPPING.put("热带风暴", WeatherContract.WeatherColumns.WeatherCode.TROPICAL_STORM);
//        ICON_MAPPING.put("飓风", WeatherContract.WeatherColumns.WeatherCode.HURRICANE);
//        ICON_MAPPING.put("强雷暴", WeatherContract.WeatherColumns.WeatherCode.SEVERE_THUNDERSTORMS);
//        ICON_MAPPING.put("雷暴", WeatherContract.WeatherColumns.WeatherCode.THUNDERSTORMS);
//        ICON_MAPPING.put("雨夹雪", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SNOW);
//        ICON_MAPPING.put("混合雨和冻雨", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SLEET);
//        ICON_MAPPING.put("混合雪和冻雨", WeatherContract.WeatherColumns.WeatherCode.MIXED_SNOW_AND_SLEET);
//        ICON_MAPPING.put("冻毛毛雨", WeatherContract.WeatherColumns.WeatherCode.FREEZING_DRIZZLE);
//        ICON_MAPPING.put("冻雨", WeatherContract.WeatherColumns.WeatherCode.FREEZING_RAIN);
//        ICON_MAPPING.put("毛毛雨", WeatherContract.WeatherColumns.WeatherCode.DRIZZLE);
//        ICON_MAPPING.put("阵雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
//        ICON_MAPPING.put("飘雪", WeatherContract.WeatherColumns.WeatherCode.SNOW_FLURRIES);
//        ICON_MAPPING.put("小雪", WeatherContract.WeatherColumns.WeatherCode.LIGHT_SNOW_SHOWERS);
//        ICON_MAPPING.put("吹雪", WeatherContract.WeatherColumns.WeatherCode.BLOWING_SNOW);
//        ICON_MAPPING.put("雪景", WeatherContract.WeatherColumns.WeatherCode.SNOW);
//        ICON_MAPPING.put("冰雹", WeatherContract.WeatherColumns.WeatherCode.HAIL);
//        ICON_MAPPING.put("冻雨", WeatherContract.WeatherColumns.WeatherCode.SLEET);
//        ICON_MAPPING.put("沙尘", WeatherContract.WeatherColumns.WeatherCode.DUST);
//        ICON_MAPPING.put("多雾", WeatherContract.WeatherColumns.WeatherCode.FOGGY);
//        ICON_MAPPING.put("阴霾", WeatherContract.WeatherColumns.WeatherCode.HAZE);
//        ICON_MAPPING.put("烟雾", WeatherContract.WeatherColumns.WeatherCode.SMOKY);
//        ICON_MAPPING.put("强风", WeatherContract.WeatherColumns.WeatherCode.BLUSTERY);
//        ICON_MAPPING.put("大风", WeatherContract.WeatherColumns.WeatherCode.WINDY);
//        ICON_MAPPING.put("冷", WeatherContract.WeatherColumns.WeatherCode.COLD);
//        ICON_MAPPING.put("阴天", WeatherContract.WeatherColumns.WeatherCode.CLOUDY);
//        ICON_MAPPING.put("大部多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_NIGHT);
//        ICON_MAPPING.put("大部多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_DAY);
//        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_NIGHT);
//        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_DAY);
//        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.CLEAR_NIGHT);
//        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.SUNNY);
//        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.FAIR_NIGHT);
//        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.FAIR_DAY);
//        ICON_MAPPING.put("雨夹冰雹", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_HAIL);
//        ICON_MAPPING.put("热", WeatherContract.WeatherColumns.WeatherCode.HOT);
//        ICON_MAPPING.put("局部雷暴雨", WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSTORMS);
//        ICON_MAPPING.put("局部雷雨", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_THUNDERSTORMS);
//        ICON_MAPPING.put("零星阵雨", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SHOWERS);
//        ICON_MAPPING.put("大雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);
//        ICON_MAPPING.put("零星阵雪", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SNOW_SHOWERS);
//        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY);
//        ICON_MAPPING.put("雷阵雨", WeatherContract.WeatherColumns.WeatherCode.THUNDERSHOWER);
//        ICON_MAPPING.put("阵雪", WeatherContract.WeatherColumns.WeatherCode.SNOW_SHOWERS);
//        ICON_MAPPING.put("局部雷阵雨", WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSHOWERS);
        //自定义
        ICON_MAPPING.put("晴", WeatherContract.WeatherColumns.WeatherCode.SUNNY);
        ICON_MAPPING.put("晴晚", WeatherContract.WeatherColumns.WeatherCode.CLEAR_NIGHT);
        ICON_MAPPING.put("多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_DAY);
        ICON_MAPPING.put("多云晚", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_NIGHT);

        ICON_MAPPING.put("小雨", WeatherContract.WeatherColumns.WeatherCode.DRIZZLE);
        ICON_MAPPING.put("中雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("大雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("阵雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("暴雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("雷阵雨", WeatherContract.WeatherColumns.WeatherCode.THUNDERSHOWER);

        ICON_MAPPING.put("阴", WeatherContract.WeatherColumns.WeatherCode.CLOUDY);
        ICON_MAPPING.put("雾", WeatherContract.WeatherColumns.WeatherCode.FOGGY);

        ICON_MAPPING.put("雨夹雪", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SLEET);
        ICON_MAPPING.put("小雪", WeatherContract.WeatherColumns.WeatherCode.LIGHT_SNOW_SHOWERS);
        ICON_MAPPING.put("阵雪", WeatherContract.WeatherColumns.WeatherCode.SNOW_SHOWERS);
        ICON_MAPPING.put("中雪", WeatherContract.WeatherColumns.WeatherCode.BLOWING_SNOW);
        ICON_MAPPING.put("大雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);
        ICON_MAPPING.put("暴雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);

        ICON_MAPPING.put("霾", WeatherContract.WeatherColumns.WeatherCode.HAZE);
        ICON_MAPPING.put("浮尘", WeatherContract.WeatherColumns.WeatherCode.DUST);
        ICON_MAPPING.put("扬沙", WeatherContract.WeatherColumns.WeatherCode.DUST);

        ICON_MAPPING.put("小到中雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("中到大雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("大到暴雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("暴雨到大暴雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("大暴雨到特大暴雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("小到中雪", WeatherContract.WeatherColumns.WeatherCode.BLOWING_SNOW);
        ICON_MAPPING.put("中到大雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);
        ICON_MAPPING.put("大到暴雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);

        //"小到中雨","22":"中到大雨","23":"大到暴雨","24":"暴雨到大暴雨","25":"大暴雨到特大暴雨","26":"小到中雪","27":"中到大雪","28":"大到暴雪"
    }
}
