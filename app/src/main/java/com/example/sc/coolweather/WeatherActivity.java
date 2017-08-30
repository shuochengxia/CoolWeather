package com.example.sc.coolweather;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.sc.coolweather.gson.Forecast;
import com.example.sc.coolweather.gson.Weather;
import com.example.sc.coolweather.util.HttpUtil;
import com.example.sc.coolweather.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    private ProgressDialog progressDialog;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button navButton;

    private Button helpButton;

    public LocationClient mLocationClient = null;

    public BDLocationListener myListener = new MyLocationListener();

    private StringBuffer currentLocation;

    private Button setupButton;

    public static final int UPDATE_TEXT = 1;

    public static final int DELAY_1_SECOND = 2;

    private BDLocation bdLocation;


    private TextMessageClient textMessageClient = new TextMessageClient();

    private EmailService.MyBinder myBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (EmailService.MyBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TEXT:
                    if (bdLocation.getLocType() == 161) {
                        sportText.setText("运行建议：信息获取失败");
                    }
                    break;
                case DELAY_1_SECOND:
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(WeatherActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        Log.d("Main", "xxxx");
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        helpButton = (Button) findViewById(R.id.help_button);
        setupButton = (Button) findViewById(R.id.setup_button);
        List<String> permissionList = new ArrayList<>();

        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );
        //注册监听函数
        initLocation();
        mLocationClient.start();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            showProgressDialog();
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WeatherActivity.this, "天气更新中...", Toast.LENGTH_SHORT).show();

                SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                String emailCt = pref.getString("emailContacts", "");
                String phoneContactsList = getPhoneContacts();
                if (phoneContactsList.length() == 0) {
                    Toast.makeText(WeatherActivity.this, "列表为空", Toast.LENGTH_SHORT).show();
                } else {
                    textMessageClient.initClient(phoneContactsList, currentLocation
                            .append("实时位置信息请查看邮箱：").append(emailCt).toString());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            textMessageClient.sendTextMessage();
                        }
                    }).start();
                }

                Intent sendEmailIntent = new Intent(WeatherActivity.this, EmailService.class);
                //Log.d("Weather", "xx1");
                startService(sendEmailIntent);
                //Log.d("Weather", "xx2");
                bindService(sendEmailIntent, connection, BIND_AUTO_CREATE);


                //假装更新天气
                swipeRefresh.setRefreshing(true);
                requestWeather(mWeatherId);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                            Message message = new Message();
                            message.what = DELAY_1_SECOND;
                            handler.sendMessage(message);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                //swipeRefresh.setRefreshing(false);
                //Toast.makeText(WeatherActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
            }
        });
        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this, ContactsActivity.class);
                intent.putExtra("location", currentLocation.toString());
                startActivity(intent);
            }
        });
    }

    //获取保存的联系人
    private String getPhoneContacts() {
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        String buffer = pref.getString("phoneContacts", "");
        //String[] contacts = buffer.split(" ");
        return buffer;
    }

    //请求天气信息
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=3c26e2b29dea4729b0eb8c4309f660ba";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    //显示天气信息
    private void showWeatherInfo(Weather weather) {
        //showProgressDialog();
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];

        //处理当前气温
        String dmax = weather.forecastList.get(0).temperature.max;
        String dmin = weather.forecastList.get(0).temperature.min;
        int nowTemperature = (Integer.parseInt(dmax) + Integer.parseInt(dmin))/2;
        String nowTemperatureString = String.valueOf(nowTemperature);

        String degree = nowTemperatureString + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);

        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运行建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);

        /*
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        */

        loadBingPic();
        closeProgressDialog();
        weatherLayout.setVisibility(View.VISIBLE);
    }

    //加载图片
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(WeatherActivity.this);
            progressDialog.setMessage("加载中");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    //初始化定位
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        //option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span=5000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            bdLocation = location;

            //获取定位结果
            currentLocation = new StringBuffer(256);

            currentLocation.append("time : ");
            currentLocation.append(location.getTime());    //获取定位时间

            //currentLocation.append("\nerror code : ");
            // currentLocation.append(location.getLocType());    //获取类型类型

            currentLocation.append("  纬度 : ");
            currentLocation.append(location.getLatitude());    //获取纬度信息

            currentLocation.append("  经度 : ");
            currentLocation.append(location.getLongitude());    //获取经度信息

            currentLocation.append("  精度半径 : ");
            currentLocation.append(location.getRadius());    //获取定位精准度


            if (location.getLocType() == BDLocation.TypeGpsLocation){

                // GPS定位结果
                currentLocation.append("  speed : ");
                currentLocation.append(location.getSpeed());    // 单位：公里每小时

                //currentLocation.append("\nsatellite : ");
                //currentLocation.append(location.getSatelliteNumber());    //获取卫星数

                currentLocation.append("  height : ");
                currentLocation.append(location.getAltitude());    //获取海拔高度信息，单位米

                currentLocation.append("  direction : ");
                currentLocation.append(location.getDirection());    //获取方向信息，单位度

                currentLocation.append("  addr : ");
                currentLocation.append(location.getAddrStr());    //获取地址信息

                //currentLocation.append("\ndescribe : ");
                //currentLocation.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){

                // 网络定位结果
                currentLocation.append("  addr : ");
                currentLocation.append(location.getAddrStr());    //获取地址信息

                //currentLocation.append("\noperationers : ");
                //currentLocation.append(location.getOperators());    //获取运营商信息

                //currentLocation.append("\ndescribe : ");
                //currentLocation.append("网络定位成功");

            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {

                // 离线定位结果
                currentLocation.append("  describe : ");
                currentLocation.append("离线定位成功，离线定位结果也是有效的");

            } else if (location.getLocType() == BDLocation.TypeServerError) {

                currentLocation.append("  describe : ");
                currentLocation.append("服务端网络定位失败");

            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {

                currentLocation.append("  describe : ");
                currentLocation.append("网络不同导致定位失败，请检查网络是否通畅");

            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {

                currentLocation.append("  describe : ");
                currentLocation.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");

            }

            currentLocation.append("  位置描述 : ");
            currentLocation.append(location.getLocationDescribe());    //位置语义化信息

            /*List<Poi> list = location.getPoiList();    // POI数据
            if (list != null) {
                currentLocation.append("\npoilist size = : ");
                currentLocation.append(list.size());
                for (Poi p : list) {
                    currentLocation.append("\npoi= : ");
                    currentLocation.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }*/

            Log.i("BaiduLocation", currentLocation.toString());
            //helpText.setText(currentLocation.toString());
            Message message = new Message();
            message.what = UPDATE_TEXT;
            handler.sendMessage(message);
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {}

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
    }

}