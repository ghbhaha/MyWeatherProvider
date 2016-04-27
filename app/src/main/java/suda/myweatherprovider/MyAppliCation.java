package suda.myweatherprovider;

import android.app.Application;

import suda.myweatherprovider.util.AssetsCopyUtil;

/**
 * Created by ghbha on 2016/4/27.
 */
public class MyAppliCation extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AssetsCopyUtil.copyEmbassy2Databases(this, "data/data/" + this.getPackageName() + "/databases/",
                "location.db");
    }
}
