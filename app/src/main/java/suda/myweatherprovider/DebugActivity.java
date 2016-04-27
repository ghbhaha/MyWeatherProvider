package suda.myweatherprovider;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.json.JSONException;

import java.util.ArrayList;

import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;
import suda.myweatherprovider.db.CityDao;
import suda.myweatherprovider.model.City;

public class DebugActivity extends AppCompatActivity {

    private static final String TAG = "MyWeather@@@@@@@@@@";
    private static final boolean DEBUG = true;

    private static final String URL_LOCATION =
            "http://weatherapi.market.xiaomi.com/wtr-v2/city/search?name=%s&language=zh_CN&imei=e32c8a29d0e8633283737f5d9f381d47&device=HM2013023&miuiVersion=JHBCNBD16.0&mod";

    private static final String URL_WEATHER =
            "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=%s&imei=e32c8a29d0e8633283737f5d9f381d47&device=HM2013023&miuiVersion=JHBCNBD16.0&modDevice=&source=miuiWeatherApp";

    private static final String URL_FORECAST =
            "http://wthrcdn.etouch.cn/weather_mini?citykey=%s";


    // http://wthrcdn.etouch.cn/weather_mini?city=北京
    // 通过城市名字获得天气数据，json数据
    //http://wthrcdn.etouch.cn/weather_mini?citykey=101010100

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
//        LookupCityNameRequestTask lookupTask = new LookupCityNameRequestTask("南通");
//        lookupTask.execute();

        WeatherUpdateRequestTask weatherTask = new WeatherUpdateRequestTask("101010100");
        weatherTask.execute();
    }


    private class WeatherUpdateRequestTask extends AsyncTask<Void, Void, WeatherInfo> {
        final String mRequest;

        public WeatherUpdateRequestTask(String request) {
            mRequest = request;
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {

            //TODO Read units from settings
            //String currentConditionURL = String.format(URL_WEATHER, mRequest.getRequestInfo().getWeatherLocation().getCityId());
            String currentConditionURL = String.format(URL_WEATHER, mRequest);
            if (DEBUG) Log.d(TAG, "Current condition URL " + currentConditionURL);
            String currentConditionResponse = HttpRetriever.retrieve(currentConditionURL);
            if (currentConditionResponse == null) return null;
            if (DEBUG) Log.d(TAG, "Response " + currentConditionResponse);
//            String forecastUrl = String.format(URL_FORECAST,
//                    mRequest.getRequestInfo().getWeatherLocation().getCityId());

            String forecastUrl = String.format(URL_FORECAST,
                    mRequest );

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
                //  JSONObject main = currentCondition.getJSONObject("main");
                // JSONObject wind = currentCondition.getJSONObject("wind");
                ArrayList<WeatherInfo.DayForecast> forecasts =
                        parseForecasts(forecast.getJSONArray("forecast"), true);

                String cityName = null;
//                if (mRequest.getRequestInfo().getRequestType()
//                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
//                    cityName = mRequest.getRequestInfo().getWeatherLocation().getCity();
//                }
                if (cityName == null || TextUtils.equals(cityName, "")) {
                    cityName = aqi.getString("city");
                    if (cityName == null) return null;
                }

                WeatherInfo.Builder weatherInfo = new WeatherInfo.Builder(
                        cityName, sanitizeTemperature(currentCondition.getDouble("temp"), true),
                        WeatherContract.WeatherColumns.TempUnit.CELSIUS);
                weatherInfo.setHumidity(accu_cc.getDouble("RelativeHumidity"));
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

//                if (mRequest.getRequestInfo().getRequestType()
//                        == RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ) {
//                    mLastWeatherLocation = mRequest.getRequestInfo().getWeatherLocation();
//                    mLastLocation = null;
//                } else if (mRequest.getRequestInfo().getRequestType()
//                        == RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ) {
//                    mLastLocation = mRequest.getRequestInfo().getLocation();
//                    mLastWeatherLocation = null;
//                }

                return weatherInfo.build();
            } catch (JSONException e) {
                //Received malformed or missing data
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

        private int getIconIdByType(String type) {
            return WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
        }

        // OpenWeatherMap sometimes returns temperatures in Kelvin even if we ask it
        // for deg C or deg F. Detect this and convert accordingly.
        private double sanitizeTemperature(double value, boolean metric) {
            // threshold chosen to work for both C and F. 170 deg F is hotter
            // than the hottest place on earth.
            if (value > 170d) {
                // K -> deg C
                value -= 273.15d;
                if (!metric) {
                    // deg C -> deg F
                    value = (value * 1.8d) + 32d;
                }
            }
            return value;
        }

        @Override
        protected void onPostExecute(WeatherInfo weatherInfo) {
            if (weatherInfo == null) {
                if (DEBUG) Log.d(TAG, "Received null weather info, failing request");
               // mRequest.fail();
            } else {
                if (DEBUG) Log.d(TAG, weatherInfo.toString());
                ServiceRequestResult result = new ServiceRequestResult.Builder(weatherInfo).build();
                //mRequest.complete(result);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    private class LookupCityNameRequestTask
            extends AsyncTask<Void, Void, ArrayList<WeatherLocation>> {

        final String mRequest;

        public LookupCityNameRequestTask(String city) {
            mRequest = city;
        }

        @Override
        protected ArrayList<WeatherLocation> doInBackground(Void... params) {
            ArrayList<WeatherLocation> locations = getLocations(mRequest);
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
                // mRequest.complete(request);
            } else {
                // mRequest.fail();
            }
        }

        private ArrayList<WeatherLocation> getLocations(String input) {
//            String url = String.format(URL_LOCATION, Uri.encode(input));
//            String response = HttpRetriever.retrieve(url);
//            if (response == null) {
//                return null;
//            }
//
//            try {
//
//                com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(response);
//                com.alibaba.fastjson.JSONArray cityInfos = jsonObject.getJSONArray("cityInfos");
//                ArrayList<WeatherLocation> results = new ArrayList<>();
//                for (int i = 0; i < cityInfos.size(); i++) {
//                    com.alibaba.fastjson.JSONObject cityInfo = cityInfos.getJSONObject(i);
//                    com.alibaba.fastjson.JSONObject metaData = cityInfo.getJSONObject("metaData");
//                    //com.alibaba.fastjson.JSONArray names = cityInfo.getJSONArray("names");
//                    String areaCode = metaData.getString("areaCode");
//                    String country = metaData.getString("country");
//                    WeatherLocation weatherLocation = new WeatherLocation.Builder(areaCode, input)
//                            .setCountry(country).build();
//                    results.add(weatherLocation);
//                    Log.d(TAG, "areaCode:" + areaCode);
//                }


//                for (int i = 0; i < count; i++) {
//                    JSONObject result = jsonResults.getJSONObject(i);
//                    String cityId = result.getString("id");
//                    String cityName = result.getString("name");
//                    String country = result.getJSONObject("sys").getString("country");
//
//                    WeatherLocation weatherLocation = new WeatherLocation.Builder(cityId, cityName)
//                            .setCountry(country).build();
//                    results.add(weatherLocation);
//                }
            ArrayList<WeatherLocation> results = new ArrayList<>();
            City city = new CityDao(DebugActivity.this).getCityByAreaName(input);
            String areaCode = city.getWeatherId();
            String country = "CN";
            WeatherLocation weatherLocation = new WeatherLocation.Builder(areaCode, input)
                    .setCountry(country).build();

            results.add(weatherLocation);
            return results;

        }
    }
}
