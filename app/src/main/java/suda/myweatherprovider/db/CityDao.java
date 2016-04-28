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
        Cursor cursor = db.query("weathers", null, "area_name=?",
                new String[]{areaName}, null, null, null);
        List<City> cities = new ArrayList<>();
        while (cursor.moveToNext()) {
            City city = new City();
            city.setAreaName(areaName);
            city.setProvinceName(cursor.getString(cursor.getColumnIndex("province_name")));
            city.setCityName(cursor.getString(cursor.getColumnIndex("city_name")));
            city.setWeatherId(cursor.getString(cursor.getColumnIndex("weather_id")));
            cities.add(city);
        }
        cursor.close();
        return cities;
    }

}
