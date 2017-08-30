package com.example.sc.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sc on 2017/8/2 0002.
 */

public class Forecast {

    public String date;

    @SerializedName("tmp")
    public Temperature temperature;

    @SerializedName("cond")
    public More more;

    public class Temperature {

        public String max;

        public String min;

    }

    public class More {

        @SerializedName("txt_d")
        public String info;

    }

}
