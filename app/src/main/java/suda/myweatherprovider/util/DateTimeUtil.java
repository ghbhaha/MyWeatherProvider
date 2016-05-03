package suda.myweatherprovider.util;

import java.util.Calendar;

/**
 * Created by ghbha on 2016/5/2.
 */
public class DateTimeUtil {

    public static boolean isNight() {
        Calendar date = Calendar.getInstance();
        int hour = date.get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 19) {
            return true;
        }
        return false;
    }

    public static boolean isNight(String sunRise, String sunDown) {
        //"todayRise":"05:11",
        //"todaySet":"18:40",
        int riseHour = Integer.parseInt(sunRise.split(":")[0]);
        int downHour = Integer.parseInt(sunDown.split(":")[0]);
        Calendar date = Calendar.getInstance();
        int hour = date.get(Calendar.HOUR_OF_DAY);
        if ((hour <= riseHour || hour >= downHour)) {
            return true;
        }
        return false;
    }


}
