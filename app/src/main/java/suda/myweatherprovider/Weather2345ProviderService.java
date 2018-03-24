package suda.myweatherprovider;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lineageos.providers.WeatherContract;
import lineageos.weather.RequestInfo;
import lineageos.weather.WeatherInfo;
import lineageos.weather.WeatherLocation;
import lineageos.weatherservice.ServiceRequest;
import lineageos.weatherservice.ServiceRequestResult;
import lineageos.weatherservice.WeatherProviderService;
import suda.myweatherprovider.db.CityDao;
import suda.myweatherprovider.model.City;
import suda.myweatherprovider.util.DateTimeUtil;
import suda.myweatherprovider.util.DecodeUtil;
import suda.myweatherprovider.util.HttpRetriever;
import suda.myweatherprovider.util.IconUtil;
import suda.myweatherprovider.util.TextUtil;

/**
 * Created by ghbha on 2016/4/27.
 */
public class Weather2345ProviderService extends WeatherProviderService {


    private static final String TAG = "MyWeather";
    private static final boolean DEBUG = true;

    private CityDao cityDao;

    private Map<ServiceRequest, WeatherUpdateRequestTask> mWeatherUpdateRequestMap = new HashMap<>();
    private Map<ServiceRequest, LookupCityNameRequestTask> mLookupCityRequestMap = new HashMap<>();

    private WeatherInfo lastWeatherInfo;

    private static final String URL_WEATHER_MIUI =
            "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=%s&language=zh_CN&imei=e32c8a29d0e8633283737f5d9f381d47&device=HM2013023&miuiVersion=JHBCNBD16.0&mod";
    private static final String URL_WEATHER_2345 =
            "http://tianqi.2345.com/t/new_mobile_json/%s.json";

    private String MEIZU_LOCATION = "http://tools.meizu.com/service/weather_weatherdataact/searchCityNameAndCode.jsonp?p0=%s";

    // private static final String GEO_URL = "http://maps.google.com/maps/api/geocode/json?latlng=%s,%s&language=zh-CN&sensor=false";
    private static final String GEO_URL = "http://api.map.baidu.com/geocoder/v2/?ak=zYXfHVG6r6xTqlxgHrnK650y&callback=renderReverse&location=%s,%s&output=json&pois=1";


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
                List<City> citys = cityDao.getCitysByAreaName(TextUtil.getFormatArea(input));
                for (City city : citys) {
                    WeatherLocation weatherLocation = new WeatherLocation.Builder(city.getWeatherId() + "," + city.getAreaId(), city.getAreaName())
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
            String weatherID = "";
            String areaID = "";
            try {
                String cityIds = null;
                if (mRequest.getRequestInfo().getRequestType()
                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
                    cityIds = mRequest.getRequestInfo().getWeatherLocation().getCityId();
                    weatherID = cityIds.split(",")[0];
                    areaID = cityIds.split(",")[1];
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
                    areaName = TextUtil.getFormatArea(areaName);
                    cityName = TextUtil.getFormatArea(cityName);
                    City city = cityDao.getCityByCityAndArea(cityName, areaName);
                    if (city == null) {
                        city = cityDao.getCityByCityAndArea(cityName, cityName);
                        if (city == null)
                            return null;
                    }
                    weatherID = city.getWeatherId();
                    areaID = city.getAreaId();
                } else {
                    return null;
                }

                //miui天气
                String miuiURL = String.format(URL_WEATHER_MIUI, weatherID);
                if (DEBUG) Log.d(TAG, "miuiURL " + miuiURL);
                String miuiResponse = HttpRetriever.retrieve(miuiURL);
                if (miuiResponse == null) return null;
                if (DEBUG) Log.d(TAG, "Rmiuiesponse " + miuiResponse);

                //2345天气
                String ttffUrl = String.format(URL_WEATHER_2345, areaID);
                if (DEBUG) Log.d(TAG, "ttffUrl " + ttffUrl);
                String ttffResponse = DecodeUtil.decodeResponse(HttpRetriever.retrieve(ttffUrl));
                if (ttffResponse == null) return null;
                if (DEBUG) Log.d(TAG, "ttffResponse " + ttffResponse);


                JSONObject ttffAll = JSON.parseObject(ttffResponse);
                String cityName = ttffAll.getString("cityName");
                //实时
                JSONObject sk = ttffAll.getJSONObject("sk");
                String humidity = sk.getString("humidity");
                String sk_temp = sk.getString("sk_temp");

                //日落日升
                JSONObject sunrise = ttffAll.getJSONObject("sunrise");

                ArrayList<WeatherInfo.DayForecast> forecasts =
                        parse2345(ttffAll.getJSONArray("days7"), true);

                WeatherInfo.Builder weatherInfo = null;
                weatherInfo = new WeatherInfo.Builder(
                        cityName, sanitizeTemperature(Double.parseDouble(sk_temp), true),
                        WeatherContract.WeatherColumns.TempUnit.CELSIUS);
                //湿度
                humidity = humidity.replace("%", "");
                weatherInfo.setHumidity(Double.parseDouble(humidity));

                if (miuiResponse != null) {
                    //风速，风向
                    JSONObject weather = JSON.parseObject(miuiResponse);
                    JSONObject accu_cc = weather.getJSONObject("accu_cc");
                    weatherInfo.setWind(accu_cc.getDouble("WindSpeed"), accu_cc.getDouble("WindDirectionDegrees"),
                            WeatherContract.WeatherColumns.WindSpeedUnit.KPH);
                }

                weatherInfo.setTimestamp(System.currentTimeMillis());
                weatherInfo.setForecast(forecasts);

                if (forecasts.size() > 0) {
                    weatherInfo.setTodaysLow(sanitizeTemperature(forecasts.get(0).getLow(), true));
                    weatherInfo.setTodaysHigh(sanitizeTemperature(forecasts.get(0).getHigh(), true));
                    weatherInfo.setWeatherCondition(IconUtil.getWeatherCodeByType(
                            ttffAll.getJSONArray("days7").getJSONObject(0).getString(
                                    DateTimeUtil.isNight(sunrise.getString("todayRise"), sunrise.getString("todaySet"))
                                            ? "nightWeaShort" : "dayWeaShort"), sunrise.getString("todayRise"), sunrise.getString("todaySet")));
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

        private ArrayList<WeatherInfo.DayForecast> parse2345(JSONArray forecasts, boolean metric) throws JSONException {
            ArrayList<WeatherInfo.DayForecast> result = new ArrayList<>();
            int count = forecasts.size();
            if (count == 0) {
                throw new JSONException("Empty forecasts array");
            }

            int base = 0;
            if (System.currentTimeMillis() > (forecasts.getJSONObject(1).getLongValue("time") * 1000))
                base = 1;

            for (int i = base; i < count - 1; i++) {
                JSONObject forecast = forecasts.getJSONObject(i);
                String tmpMin = forecast.getString("wholeTemp").split("～")[0];
                String tmpMax = forecast.getString("wholeTemp").split("～")[1];
                WeatherInfo.DayForecast item = new WeatherInfo.DayForecast.Builder(
                        IconUtil.getWeatherCodeByType(forecast.getString("dayWeaShort")))
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
