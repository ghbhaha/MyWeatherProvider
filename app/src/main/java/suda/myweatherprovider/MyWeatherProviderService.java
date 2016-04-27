package suda.myweatherprovider;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;
import cyanogenmod.weatherservice.WeatherProviderService;
import suda.myweatherprovider.db.CityDao;
import suda.myweatherprovider.model.City;

/**
 * Created by ghbha on 2016/4/27.
 */
public class MyWeatherProviderService extends WeatherProviderService {


    private static final String TAG = "MyWeather";
    private static final boolean DEBUG = true;

    private CityDao cityDao;

    private Map<ServiceRequest, WeatherUpdateRequestTask> mWeatherUpdateRequestMap = new HashMap<>();
    private Map<ServiceRequest, LookupCityNameRequestTask> mLookupCityRequestMap = new HashMap<>();

    private static final String URL_LOCATION =
            "http://weatherapi.market.xiaomi.com/wtr-v2/city/search?name=%s";


    private static final String URL_WEATHER =
            "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=%s";

    private static final String URL_FORECAST =
            "http://wthrcdn.etouch.cn/weather_mini?citykey=%s";


    @Override
    protected void onConnected() {
        super.onConnected();
        cityDao = new CityDao(this);
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
    }

    @Override
    protected void onRequestSubmitted(ServiceRequest serviceRequest) {
        RequestInfo requestInfo = serviceRequest.getRequestInfo();
        int requestType = requestInfo.getRequestType();
        if (DEBUG) Log.d(TAG, "Received request type " + requestType);

        switch (requestType) {
            case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
            case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
                synchronized (mWeatherUpdateRequestMap) {
                    WeatherUpdateRequestTask weatherTask = new WeatherUpdateRequestTask(serviceRequest);
                    mWeatherUpdateRequestMap.put(serviceRequest, weatherTask);
                    weatherTask.execute();
                }
                break;
            case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                synchronized (mLookupCityRequestMap) {
                    LookupCityNameRequestTask lookupTask = new LookupCityNameRequestTask(serviceRequest);
                    mLookupCityRequestMap.put(serviceRequest, lookupTask);
                    lookupTask.execute();
                }
                break;
        }
    }

    @Override
    protected void onRequestCancelled(ServiceRequest serviceRequest) {
        switch (serviceRequest.getRequestInfo().getRequestType()) {
            case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
            case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
                synchronized (mWeatherUpdateRequestMap) {
                    WeatherUpdateRequestTask task = mWeatherUpdateRequestMap.remove(serviceRequest);
                    if (task != null) {
                        task.cancel(true);
                    }
                    return;
                }
            case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                synchronized (mLookupCityRequestMap) {
                    LookupCityNameRequestTask task = mLookupCityRequestMap.remove(serviceRequest);
                    if (task != null) {
                        task.cancel(true);
                    }
                }
                return;
            default:
                Log.w(TAG, "Received unknown request type "
                        + serviceRequest.getRequestInfo().getRequestType());
                break;
        }
    }

    private class LookupCityNameRequestTask
            extends AsyncTask<Void, Void, ArrayList<WeatherLocation>> {

        final ServiceRequest mRequest;

        public LookupCityNameRequestTask(ServiceRequest request) {
            mRequest = request;
        }

        @Override
        protected ArrayList<WeatherLocation> doInBackground(Void... params) {
            ArrayList<WeatherLocation> locations = getLocations(
                    mRequest.getRequestInfo().getCityName());
            return locations;
        }

        @Override
        protected void onPostExecute(ArrayList<WeatherLocation> locations) {
            if (locations != null) {
                if (DEBUG) {
                    for (WeatherLocation location : locations) {
                        Log.d(TAG, location.toString());
                    }
                }
                ServiceRequestResult request = new ServiceRequestResult.Builder(locations).build();
                mRequest.complete(request);
            } else {
                mRequest.fail();
            }
        }

        private ArrayList<WeatherLocation> getLocations(String input) {


            String url = String.format(URL_LOCATION, Uri.encode(input));
            String response = HttpRetriever.retrieve(url);
            if (response == null) {
                return null;
            }
            ArrayList<WeatherLocation> results = new ArrayList<>();
            try {
                com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(response);
                com.alibaba.fastjson.JSONArray cityInfos = jsonObject.getJSONArray("cityInfos");
                for (int i = 0; i < cityInfos.size(); i++) {
                    com.alibaba.fastjson.JSONObject cityInfo = cityInfos.getJSONObject(i);
                    com.alibaba.fastjson.JSONObject metaData = cityInfo.getJSONObject("metaData");
                    City city = cityDao.getCityByAreaName(input);
                    if (city == null)
                        return results;
                    String areaCode = city.getWeatherId();
                    String country = "中国";
                    WeatherLocation weatherLocation = new WeatherLocation.Builder(areaCode, input)
                            .setCountry(country).setState(city.getCityName() + "/" + city.getProvinceName())
                            .setCountryId(metaData.getString("country"))
                            .setPostalCode(metaData.getString("areaCode"))
                            .build();

                    results.add(weatherLocation);
                    Log.d(TAG, "areaCode:" + areaCode);
                }
            } catch (Exception e) {

            }
            return results;
        }
    }

    private class WeatherUpdateRequestTask extends AsyncTask<Void, Void, WeatherInfo> {
        final ServiceRequest mRequest;

        public WeatherUpdateRequestTask(ServiceRequest request) {
            mRequest = request;
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {
            //TODO Read units from settings
            String currentConditionURL = String.format(URL_WEATHER, mRequest.getRequestInfo().getWeatherLocation().getCityId());

            if (DEBUG) Log.d(TAG, "Current condition URL " + currentConditionURL);
            String currentConditionResponse = HttpRetriever.retrieve(currentConditionURL);
            if (currentConditionResponse == null) return null;
            if (DEBUG) Log.d(TAG, "Response " + currentConditionResponse);
            String forecastUrl = String.format(URL_FORECAST,
                    mRequest.getRequestInfo().getWeatherLocation().getCityId());

            if (DEBUG) Log.d(TAG, "Forecast URL " + forecastUrl);
            String forecastResponse = HttpRetriever.retrieve(forecastUrl);
            if (forecastUrl == null) return null;
            if (DEBUG) Log.d(TAG, "Response " + forecastResponse);

            try {
                JSONObject weather = JSON.parseObject(currentConditionResponse);
                JSONObject forecast = JSON.parseObject(forecastResponse).getJSONObject("data");
                JSONObject currentCondition = weather.getJSONObject("realtime");
                JSONObject main = weather.getJSONObject("today");
                JSONObject aqi = weather.getJSONObject("aqi");
                JSONObject accu_cc = weather.getJSONObject("accu_cc");
                ArrayList<WeatherInfo.DayForecast> forecasts =
                        parseForecasts(forecast.getJSONArray("forecast"), true);

                String cityName = null;
                if (mRequest.getRequestInfo().getRequestType()
                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
                    cityName = mRequest.getRequestInfo().getWeatherLocation().getCity();
                }
                if (cityName == null || TextUtils.equals(cityName, "")) {
                    cityName = aqi.getString("city");
                    if (cityName == null) return null;
                }

                WeatherInfo.Builder weatherInfo = new WeatherInfo.Builder(
                        cityName, sanitizeTemperature(currentCondition.getDouble("temp"), true),
                        WeatherContract.WeatherColumns.TempUnit.CELSIUS);

                String humidity = currentCondition.getString("SD").replace("%", "");
                weatherInfo.setHumidity(Double.parseDouble(humidity));

                weatherInfo.setWind(accu_cc.getDouble("WindSpeed"), accu_cc.getDouble("WindDirectionDegrees"),
                        WeatherContract.WeatherColumns.WindSpeedUnit.KPH);
                String tempMin = main.getString("tempMin");
                String tempMax = main.getString("tempMax");

                weatherInfo.setTodaysLow(sanitizeTemperature(Double.parseDouble(tempMin), true));
                weatherInfo.setTodaysHigh(sanitizeTemperature(Double.parseDouble(tempMax), true));

                //NOTE: The timestamp provided by OpenWeatherMap corresponds to the time the data
                //was last updated by the stations. Let's use System.currentTimeMillis instead
                weatherInfo.setTimestamp(System.currentTimeMillis());
                weatherInfo.setWeatherCondition(getIconIdByType(currentCondition.getString("weather")));
                weatherInfo.setForecast(forecasts);

                return weatherInfo.build();
            } catch (JSONException e) {
                if (DEBUG) Log.w(TAG, "JSONException while processing weather update", e);
            }
            return null;
        }

        private ArrayList<WeatherInfo.DayForecast> parseForecasts(JSONArray forecasts, boolean metric)
                throws JSONException {
            ArrayList<WeatherInfo.DayForecast> result = new ArrayList<>();
            int count = forecasts.size();
            if (count == 0) {
                throw new JSONException("Empty forecasts array");
            }
            for (int i = 0; i < count; i++) {
                JSONObject forecast = forecasts.getJSONObject(i);
                String tmpMin = forecast.getString("low").split(" ")[1].replace("℃", "");
                String tmpMax = forecast.getString("high").split(" ")[1].replace("℃", "");
                WeatherInfo.DayForecast item = new WeatherInfo.DayForecast.Builder(getIconIdByType(forecast.getString("type")))
                        .setLow(sanitizeTemperature(Double.parseDouble(tmpMin), metric))
                        .setHigh(sanitizeTemperature(Double.parseDouble(tmpMax), metric)).build();
                result.add(item);
            }
            return result;
        }

        private double sanitizeTemperature(double value, boolean metric) {
            if (value > 170d) {
                value -= 273.15d;
                if (!metric) {
                    // deg C -> deg F
                    value = (value * 1.8d) + 32d;
                }
            }
            return value;
        }

        private int getIconIdByType(String type) {
            if (ICON_MAPPING.get(type) != null)
                return ICON_MAPPING.get(type);
            return WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
        }

        @Override
        protected void onPostExecute(WeatherInfo weatherInfo) {
            if (weatherInfo == null) {
                if (DEBUG) Log.d(TAG, "Received null weather info, failing request");
                mRequest.fail();
            } else {
                if (DEBUG) Log.d(TAG, weatherInfo.toString());
                ServiceRequestResult result = new ServiceRequestResult.Builder(weatherInfo).build();
                mRequest.complete(result);
            }
        }
    }

    private static final HashMap<String, Integer> ICON_MAPPING = new HashMap<>();

    static {
        //默认44个
        ICON_MAPPING.put("龙卷风", WeatherContract.WeatherColumns.WeatherCode.TROPICAL_STORM);
        ICON_MAPPING.put("热带风暴", WeatherContract.WeatherColumns.WeatherCode.HURRICANE);
        ICON_MAPPING.put("飓风", WeatherContract.WeatherColumns.WeatherCode.SEVERE_THUNDERSTORMS);
        ICON_MAPPING.put("强雷暴", WeatherContract.WeatherColumns.WeatherCode.THUNDERSTORMS);
        ICON_MAPPING.put("雷暴", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SNOW);
        ICON_MAPPING.put("雨夹雪", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SLEET);
        ICON_MAPPING.put("混合雨和冻雨", WeatherContract.WeatherColumns.WeatherCode.MIXED_SNOW_AND_SLEET);
        ICON_MAPPING.put("混合雪和冻雨", WeatherContract.WeatherColumns.WeatherCode.FREEZING_DRIZZLE);
        ICON_MAPPING.put("冻毛毛雨", WeatherContract.WeatherColumns.WeatherCode.FREEZING_RAIN);
        ICON_MAPPING.put("毛毛雨", WeatherContract.WeatherColumns.WeatherCode.DRIZZLE);
        ICON_MAPPING.put("冰雨", WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        ICON_MAPPING.put("阵雨", WeatherContract.WeatherColumns.WeatherCode.SNOW_FLURRIES);
        ICON_MAPPING.put("飘雪", WeatherContract.WeatherColumns.WeatherCode.LIGHT_SNOW_SHOWERS);
        ICON_MAPPING.put("小雪", WeatherContract.WeatherColumns.WeatherCode.BLOWING_SNOW);
        ICON_MAPPING.put("吹雪", WeatherContract.WeatherColumns.WeatherCode.SNOW);
        ICON_MAPPING.put("雪景", WeatherContract.WeatherColumns.WeatherCode.HAIL);
        ICON_MAPPING.put("冰雹", WeatherContract.WeatherColumns.WeatherCode.SLEET);
        ICON_MAPPING.put("冻雨", WeatherContract.WeatherColumns.WeatherCode.DUST);
        ICON_MAPPING.put("沙尘", WeatherContract.WeatherColumns.WeatherCode.FOGGY);
        ICON_MAPPING.put("多雾", WeatherContract.WeatherColumns.WeatherCode.HAZE);
        ICON_MAPPING.put("阴霾", WeatherContract.WeatherColumns.WeatherCode.SMOKY);
        ICON_MAPPING.put("烟雾", WeatherContract.WeatherColumns.WeatherCode.BLUSTERY);
        ICON_MAPPING.put("强风", WeatherContract.WeatherColumns.WeatherCode.WINDY);
        ICON_MAPPING.put("大风", WeatherContract.WeatherColumns.WeatherCode.COLD);
        ICON_MAPPING.put("阴天", WeatherContract.WeatherColumns.WeatherCode.CLOUDY);
        ICON_MAPPING.put("大部多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_NIGHT);
        ICON_MAPPING.put("大部多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_DAY);
        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_NIGHT);
        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_DAY);
        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.CLEAR_NIGHT);
        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.SUNNY);
        ICON_MAPPING.put("晴天", WeatherContract.WeatherColumns.WeatherCode.FAIR_NIGHT);
        ICON_MAPPING.put("晴天积云", WeatherContract.WeatherColumns.WeatherCode.FAIR_DAY);
        ICON_MAPPING.put("雨夹冰雹", WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_HAIL);
        ICON_MAPPING.put("热", WeatherContract.WeatherColumns.WeatherCode.HOT);
        ICON_MAPPING.put("局部雷暴雨", WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSTORMS);
        ICON_MAPPING.put("局部雷雨", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_THUNDERSTORMS);
        ICON_MAPPING.put("零星阵雨", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SHOWERS);
        ICON_MAPPING.put("大雪", WeatherContract.WeatherColumns.WeatherCode.HEAVY_SNOW);
        ICON_MAPPING.put("零星阵雪", WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SNOW_SHOWERS);
        ICON_MAPPING.put("晴间多云", WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY);
        ICON_MAPPING.put("雷阵雨", WeatherContract.WeatherColumns.WeatherCode.THUNDERSHOWER);
        ICON_MAPPING.put("阵雪", WeatherContract.WeatherColumns.WeatherCode.SNOW_SHOWERS);
        ICON_MAPPING.put("局部雷阵雨", WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSHOWERS);
        //自定义
        ICON_MAPPING.put("晴", WeatherContract.WeatherColumns.WeatherCode.SUNNY);
        ICON_MAPPING.put("多云", WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_DAY);
        ICON_MAPPING.put("小雨", WeatherContract.WeatherColumns.WeatherCode.DRIZZLE);
        ICON_MAPPING.put("阴", WeatherContract.WeatherColumns.WeatherCode.CLOUDY);

    }
}
