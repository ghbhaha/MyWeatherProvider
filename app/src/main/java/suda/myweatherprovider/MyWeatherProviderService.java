package suda.myweatherprovider;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;
import cyanogenmod.weatherservice.WeatherProviderService;
import suda.myweatherprovider.db.CityDao;
import suda.myweatherprovider.model.City;
import suda.myweatherprovider.util.HttpRetriever;
import suda.myweatherprovider.util.IconUtil;
import suda.myweatherprovider.util.SPUtils;

/**
 * Created by ghbha on 2016/4/27.
 */
public class MyWeatherProviderService extends WeatherProviderService {


    private static final String TAG = "MyWeather";
    private static final boolean DEBUG = true;

    private CityDao cityDao;

    private Map<ServiceRequest, WeatherUpdateRequestTask> mWeatherUpdateRequestMap = new HashMap<>();
    private Map<ServiceRequest, LookupCityNameRequestTask> mLookupCityRequestMap = new HashMap<>();

    private WeatherInfo lastWeatherInfo;

    //http://aider.meizu.com/app/weather/listWeather?cityIds=101200101
    //http://aider.meizu.com/app/weather/listRealtime?cityIds=101200101
    //http://tools.meizu.com/service/weather_weatherdataact/searchCityNameAndCode.jsonp?p0=南通

    //private static final String URL_LOCATION =
    //      "http://weatherapi.market.xiaomi.com/wtr-v2/city/search?name=%s&language=zh_CN&imei=e32c8a29d0e8633283737f5d9f381d47&device=HM2013023&miuiVersion=JHBCNBD16.0&mod";

    //https://api.heweather.com/x3/weather?cityid=CN%s&key=13d63a6fe83c44c897d62002f4c98551
    private static final String URL_LOCATION =
            "http://aider.meizu.com/app/city/searchByKeyword?p0=%s";

    private static final String URL_HE_WEATHER = "https://api.heweather.com/x3/weather?cityid=CN%s&key=13d63a6fe83c44c897d62002f4c98551";
    private static final String GEO_URL = "http://api.map.baidu.com/geocoder/v2/?ak=zYXfHVG6r6xTqlxgHrnK650y&callback=renderReverse&location=%s,%s&output=json&pois=1";

    private static final String URL_WEATHER =
            "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=%s&language=zh_CN&imei=e32c8a29d0e8633283737f5d9f381d47&device=HM2013023&miuiVersion=JHBCNBD16.0&mod";
    private static final String URL_FORECAST_WNL =
            "http://wthrcdn.etouch.cn/weather_mini?citykey=%s";
    private static final String URL_FORECAST_FLYME =
            "http://aider.meizu.com/app/weather/listWeather?cityIds=%s";

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

                    //凌晨0:30前不获取更新，因为服务端数据还未更新
                    Calendar calendar = Calendar.getInstance();
                    boolean beforeDawn = (calendar.get(Calendar.HOUR_OF_DAY) == 0) && (calendar.get(Calendar.MINUTE) < 30);
                    if (lastWeatherInfo != null && beforeDawn) {
                        serviceRequest.fail();
                        return;
                    }

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
                lastWeatherInfo = null;
                ServiceRequestResult request = new ServiceRequestResult.Builder(locations).build();
                mRequest.complete(request);
            } else {
                mRequest.fail();
            }
        }

        private ArrayList<WeatherLocation> getLocations(String input) {
            ArrayList<WeatherLocation> results = new ArrayList<>();
            try {
                List<City> citys = cityDao.getCitysByAreaName(input);
                for (City city : citys) {
                    WeatherLocation weatherLocation = new WeatherLocation.Builder(city.getWeatherId(),  city.getAreaName())
                            .setCountry("中国").setState(city.getCityName() + "/" + city.getProvinceName())
                            .setCountryId("0086")
                            .build();
                    results.add(weatherLocation);
                }
            } catch (Exception e) {

            }
            if (results.size() == 0)
                return null;
            else
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
            try {
                String cityId = null;
                if (mRequest.getRequestInfo().getRequestType()
                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
                    cityId = mRequest.getRequestInfo().getWeatherLocation().getCityId();
                } else if (mRequest.getRequestInfo().getRequestType() ==
                        RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ) {
                    double lat = mRequest.getRequestInfo().getLocation().getLatitude();
                    double lng = mRequest.getRequestInfo().getLocation().getLongitude();

                    String cityNameResponse = HttpRetriever.retrieve(String.format(GEO_URL, lat, lng));
                    if (TextUtils.isEmpty(cityNameResponse)) {
                        return null;
                    }
                    cityNameResponse = cityNameResponse.replace("renderReverse&&renderReverse(", "").replace(")", "");
                    Log.d(TAG, "cityNameResponse" + cityNameResponse);
                    JSONObject jsonObjectCity = JSON.parseObject(cityNameResponse);
                    String areaName = jsonObjectCity.getJSONObject("result").getJSONObject("addressComponent").getString("district");
                    String cityName = jsonObjectCity.getJSONObject("result").getJSONObject("addressComponent").getString("city");
                    if (areaName.length() > 2 && areaName.contains("县")) {
                        areaName = areaName.replace("县", "");
                    }
                    if (cityName.contains("市")) {
                        cityName = cityName.replace("市", "");
                    }
                    City city = cityDao.getCityByCityAndArea(cityName, areaName);
                    if (city == null) {
                        city = cityDao.getCityByCityAndArea(cityName, cityName);
                        if (city == null)
                            return null;
                    }
                    cityId = city.getWeatherId();
                } else {
                    return null;
                }

                //miui天气
                String miuiURL = String.format(URL_WEATHER, cityId);
                if (DEBUG) Log.d(TAG, "miuiURL" + miuiURL);
                String miuiResponse = HttpRetriever.retrieve(miuiURL);
                if (miuiResponse == null) return null;
                if (DEBUG) Log.d(TAG, "Rmiuiesponse " + miuiResponse);

                //flyme天气
                String flymeUrl = String.format(URL_FORECAST_FLYME, cityId);
                if (DEBUG) Log.d(TAG, "flymeUrl " + flymeUrl);
                String flymeResponse = HttpRetriever.retrieve(flymeUrl);
                if (flymeUrl == null) return null;
                if (DEBUG) Log.d(TAG, "flymeResponse " + flymeResponse);
                JSONObject weather = JSON.parseObject(miuiResponse);
                JSONObject currentCondition = weather.getJSONObject("realtime");
                JSONObject aqi = weather.getJSONObject("aqi");
                JSONObject accu_cc = weather.getJSONObject("accu_cc");

                ArrayList<WeatherInfo.DayForecast> forecasts = null;
                int forecastProvide = SPUtils.gets(MyWeatherProviderService.this, SettingsActivity.FORECAST_PROVIDE, 0);

                //一周预报
                if (forecastProvide == SettingsActivity.MIUI_FORECAST) {
                    forecasts =
                            parseForecastsMiui(weather.getJSONObject("forecast"), true);
                } else if (forecastProvide == SettingsActivity.FLYME_FORECAST) {
                    JSONArray value = JSON.parseObject(flymeResponse).getJSONArray("value");
                    if (value.size() > 0) {
                        forecasts =
                                parseForecastsFlyme(value.getJSONObject(0).getJSONArray("weathers"), true);
                    }
                } else if (forecastProvide == SettingsActivity.WNL_FORECAST) {
                    String forecastUrl = String.format(URL_FORECAST_WNL,
                            mRequest.getRequestInfo().getWeatherLocation().getCityId());
                    if (DEBUG) Log.d(TAG, "Forecast URL " + forecastUrl);
                    String forecastResponse = HttpRetriever.retrieve(forecastUrl);
                    if (forecastUrl == null) return null;
                    if (DEBUG) Log.d(TAG, "Response " + forecastResponse);
                    JSONObject forecast = JSON.parseObject(forecastResponse).getJSONObject("data");
                    forecasts =
                            parseForecastsWnL(forecast.getJSONArray("forecast"), true);
                }

                String cityName = null;
                if (mRequest.getRequestInfo().getRequestType()
                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
                    cityName = mRequest.getRequestInfo().getWeatherLocation().getCity();
                }
                if (cityName == null || TextUtils.equals(cityName, "")) {
                    cityName = aqi.getString("city");
                    if (cityName == null) return null;
                }

                WeatherInfo.Builder weatherInfo = null;

                if (flymeResponse != null) {
                    JSONArray value = JSON.parseObject(flymeResponse).getJSONArray("value");
                    JSONObject realTime = value.getJSONObject(0).getJSONObject("realtime");
                    weatherInfo = new WeatherInfo.Builder(
                            cityName, sanitizeTemperature(realTime.getDoubleValue("temp"), true),
                            WeatherContract.WeatherColumns.TempUnit.CELSIUS);
                    //湿度
                    weatherInfo.setHumidity(realTime.getDouble("sD"));
                    weatherInfo.setWeatherCondition(IconUtil.getWeatherCodeByType(realTime.getString("weather")));
                } else if (miuiResponse != null) {
                    weatherInfo = new WeatherInfo.Builder(
                            cityName, sanitizeTemperature(currentCondition.getDouble("temp"), true),
                            WeatherContract.WeatherColumns.TempUnit.CELSIUS);
                    //湿度
                    String humidity = currentCondition.getString("SD").replace("%", "");
                    weatherInfo.setHumidity(Double.parseDouble(humidity));
                    weatherInfo.setWeatherCondition(IconUtil.getWeatherCodeByType(currentCondition.getString("weather")));
                } else
                    return null;

                //风速，风向
                weatherInfo.setWind(accu_cc.getDouble("WindSpeed"), accu_cc.getDouble("WindDirectionDegrees"),
                        WeatherContract.WeatherColumns.WindSpeedUnit.KPH);

                weatherInfo.setTimestamp(System.currentTimeMillis());
                weatherInfo.setForecast(forecasts);

                if (forecasts.size() > 0) {
                    weatherInfo.setTodaysLow(sanitizeTemperature(forecasts.get(0).getLow(), true));
                    weatherInfo.setTodaysHigh(sanitizeTemperature(forecasts.get(0).getHigh(), true));
                }

                if (lastWeatherInfo != null)
                    lastWeatherInfo = null;

                lastWeatherInfo = weatherInfo.build();

                return lastWeatherInfo;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "JSONException while processing weather update", e);
            }
            return null;
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


        /**
         * 解析miui一周天气
         *
         * @param forecast
         * @param metric
         * @return
         */
        private ArrayList<WeatherInfo.DayForecast> parseForecastsMiui(JSONObject forecast, boolean metric) {
            ArrayList<WeatherInfo.DayForecast> result = new ArrayList<>();
            result.add(createDayForecast(forecast.getString("temp1"), forecast.getString("img_title1"), metric));
            result.add(createDayForecast(forecast.getString("temp2"), forecast.getString("img_title3"), metric));
            result.add(createDayForecast(forecast.getString("temp3"), forecast.getString("img_title5"), metric));
            result.add(createDayForecast(forecast.getString("temp4"), forecast.getString("img_title7"), metric));
            result.add(createDayForecast(forecast.getString("temp5"), forecast.getString("img_title9"), metric));
            return result;
        }

        private WeatherInfo.DayForecast createDayForecast(String temp, String weather, boolean metric) {
            String tempMin = temp.split("~")[1].replace("℃", "");
            String tempMax = temp.split("~")[0].replace("℃", "");
            return new WeatherInfo.DayForecast.Builder(IconUtil.getWeatherCodeByType(weather))
                    .setLow(sanitizeTemperature(Double.parseDouble(tempMin), metric))
                    .setHigh(sanitizeTemperature(Double.parseDouble(tempMax), metric)).build();
        }

        /**
         * 解析flyme一周天气
         *
         * @param forecasts
         * @param metric
         * @return
         * @throws JSONException
         */
        private ArrayList<WeatherInfo.DayForecast> parseForecastsFlyme(JSONArray forecasts, boolean metric)
                throws JSONException {
            ArrayList<WeatherInfo.DayForecast> result = new ArrayList<>();
            int count = forecasts.size();
            if (count == 0) {
                throw new JSONException("Empty forecasts array");
            }
            for (int i = 0; i < count - 1; i++) {
                JSONObject forecast = forecasts.getJSONObject(i);
                String tmpMin = forecast.getString("temp_night_c");
                String tmpMax = forecast.getString("temp_day_c");
                WeatherInfo.DayForecast item = new WeatherInfo.DayForecast.Builder(IconUtil.getWeatherCodeByType(forecast.getString("weather")))
                        .setLow(sanitizeTemperature(Double.parseDouble(tmpMin), metric))
                        .setHigh(sanitizeTemperature(Double.parseDouble(tmpMax), metric)).build();
                result.add(item);
            }
            return result;
        }

        /**
         * 解析万年历一周天气
         *
         * @param forecasts
         * @param metric
         * @return
         * @throws JSONException
         */
        private ArrayList<WeatherInfo.DayForecast> parseForecastsWnL(JSONArray forecasts, boolean metric)
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
                WeatherInfo.DayForecast item = new WeatherInfo.DayForecast.Builder(IconUtil.getWeatherCodeByType(forecast.getString("type")))
                        .setLow(sanitizeTemperature(Double.parseDouble(tmpMin), metric))
                        .setHigh(sanitizeTemperature(Double.parseDouble(tmpMax), metric)).build();
                result.add(item);
            }
            return result;
        }

        /**
         * 格式化温度
         *
         * @param value
         * @param metric
         * @return
         */
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

    }

}
