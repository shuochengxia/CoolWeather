package com.example.sc.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sc on 2017/8/2 0002.
 */

public class Basic {

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {

        @SerializedName("loc")
        public String updateTime;

    }

}
