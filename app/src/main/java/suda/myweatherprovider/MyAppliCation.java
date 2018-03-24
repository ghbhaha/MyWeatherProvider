package suda.myweatherprovider;

import android.app.Application;

import suda.myweatherprovider.util.AssetsCopyUtil;
import suda.myweatherprovider.util.SPUtils;

/**
 * Created by ghbha on 2016/4/27.
 */
public class MyAppliCation extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if ((int)SPUtils.get(this, "new", 0) != 1)
            SPUtils.put(this, "new", 1);
            AssetsCopyUtil.copyEmbassy2Databases(this, "data/data/" + this.getPackageName() + "/databases/",
                "location.db");
    }
}
