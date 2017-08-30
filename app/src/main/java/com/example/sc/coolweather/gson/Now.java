package com.example.sc.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sc on 2017/8/2 0002.
 */

public class Now {

    @SerializedName("temp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("txt")
        public String info;

    }

}
