package suda.myweatherprovider.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import suda.myweatherprovider.model.City;

/**
 * Created by ghbha on 2016/4/27.
 */
public class CityDao {

    private Context context;
    private DBOpenHelper dbOpenHelper;

    public CityDao(Context context) {
        this.context = context;
        dbOpenHelper = new DBOpenHelper(context);
    }

    public List<City> getCitysByAreaName(String areaName) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select a.[areaId],b.[weather_id],a.[provinceName],a.[cityName]  From citys a,weathers b where " +
                " a.[provinceName] = b.[province_name]  and a.[areaName] = b.[area_name] and b.[area_name] =  '" + areaName + "'", null);
        List<City> cities = new ArrayList<>();
        while (cursor.moveToNext()) {
            City city = new City();
            city.setAreaName(areaName);
            city.setProvinceName(cursor.getString(cursor.getColumnIndex("provinceName")));
            city.setCityName(cursor.getString(cursor.getColumnIndex("cityName")));
            city.setWeatherId(cursor.getString(cursor.getColumnIndex("weather_id")));
            city.setAreaId(cursor.getString(cursor.getColumnIndex("areaId")));
            cities.add(city);
        }
        cursor.close();
        return cities;
    }

    public City getCityByCityAndArea(String cityName, String areaName) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select a.[areaId],b.[weather_id],a.[provinceName],a.[cityName]  From citys a,weathers b where " +
                " a.[provinceName] = b.[province_name] and a.[areaName] = b.[area_name] and a.[cityName] =  '" + cityName + "' and a.[areaName] = '" + areaName + "'", null);
        City city = new City();
        if (cursor.moveToNext()) {
            city.setAreaName(cursor.getString(cursor.getColumnIndex("cityName")));
            city.setProvinceName(cursor.getString(cursor.getColumnIndex("provinceName")));
            city.setCityName(cursor.getString(cursor.getColumnIndex("cityName")));
            city.setWeatherId(cursor.getString(cursor.getColumnIndex("weather_id")));
            city.setAreaId(cursor.getString(cursor.getColumnIndex("areaId")));
            cursor.close();
        } else {
            cursor.close();
            return null;
        }

        return city;
    }

    public City getCityByWeatherID(String weather_id) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select a.[areaId],b.[weather_id],a.[provinceName],a.[cityName]  From citys a,weathers b where " +
                " a.[provinceName] = b.[province_name] and a.[areaName] = b.[area_name] and b.[weather_id] =  '" + weather_id + "'", null);
        City city = new City();
        if (cursor.moveToNext()) {
            city.setAreaName(cursor.getString(cursor.getColumnIndex("cityName")));
            city.setProvinceName(cursor.getString(cursor.getColumnIndex("provinceName")));
            city.setCityName(cursor.getString(cursor.getColumnIndex("cityName")));
            city.setWeatherId(cursor.getString(cursor.getColumnIndex("weather_id")));
            city.setAreaId(cursor.getString(cursor.getColumnIndex("areaId")));
            cursor.close();
        } else {
            cursor.close();
            return null;
        }

        return city;
    }

}
