package suda.myweatherprovider;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import suda.myweatherprovider.util.SPUtils;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRgForecast = (RadioGroup) findViewById(R.id.weather_forecast_provide);

        int select = SPUtils.gets(this, FORECAST_PROVIDE, 0);

        ((RadioButton) mRgForecast.getChildAt(select)).setChecked(true);

        mRgForecast.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton) findViewById(group.getCheckedRadioButtonId());
                SPUtils.put(SettingsActivity.this, FORECAST_PROVIDE, Integer.parseInt(radioButton.getTag().toString()));
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static final String FORECAST_PROVIDE = "FORECAST_PROVIDE";
    public static final int MIUI_FORECAST = 0;
    public static final int FLYME_FORECAST = 1;
    public static final int WNL_FORECAST = 2;

    private RadioGroup mRgForecast;
}
