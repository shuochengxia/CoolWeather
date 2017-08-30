package com.example.sc.coolweather;

import android.telephony.SmsManager;

import java.util.List;

/**
 * Created by sc on 2017/8/18 0018.
 */

public class TextMessageClient {

    private String ceilPhoneContacts;

    private String currentLocation;

    public void initClient(String ceilPhoneContacts, String currentLocation) {
        this.ceilPhoneContacts = ceilPhoneContacts;
        //this.emailContacts = emailContacts;
        this.currentLocation = currentLocation;
    }

    public void sendTextMessage() {
        SmsManager sms = SmsManager.getDefault();
        List<String> texts = sms.divideMessage(this.currentLocation);
        for (String phoneNumber : ceilPhoneContacts.split(" ")) {
            if (!phoneNumber.equals("")) {
                for (String text : texts) {
                    sms.sendTextMessage(phoneNumber, null, text, null, null);
                }
            }
        }
    }

}
