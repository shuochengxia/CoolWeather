package com.example.sc.coolweather;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ContactsActivity extends AppCompatActivity {

    private Button saveButton;

    private Button stopButton;

    private EditText phoneText;

    private EditText emailText;

    private String phoneContacts;

    private String emailContacts;

    private TextView locationText;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        saveButton = (Button) findViewById(R.id.save_button);
        stopButton = (Button) findViewById(R.id.stop_service);
        phoneText = (EditText) findViewById(R.id.phone_contacts);
        emailText = (EditText) findViewById(R.id.email_contacts);
        locationText = (TextView) findViewById(R.id.location_text);

        Intent sendEmailIntent = new Intent(ContactsActivity.this, EmailService.class);
        bindService(sendEmailIntent, connection, BIND_AUTO_CREATE);

        Intent intent = getIntent();
        locationText.setText(intent.getStringExtra("location"));
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        phoneContacts = pref.getString("phoneContacts", "");
        phoneText.setText(phoneContacts);
        emailContacts = pref.getString("emailContacts", "");
        emailText.setText(emailContacts);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ct = phoneText.getText().toString();
                String ect = emailText.getText().toString();
                if (allIsCellphoneNumber(ct)) {
                    SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                    editor.putString("phoneContacts", ct);
                    editor.putString("emailContacts", ect);
                    editor.apply();
                    Toast.makeText(ContactsActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ContactsActivity.this, "输入内容非手机号码", Toast.LENGTH_SHORT).show();
                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    myBinder.stopService();
                    Intent stopIntent = new Intent(ContactsActivity.this, EmailService.class);
                    unbindService(connection);
                    stopService(stopIntent);
                    Toast.makeText(ContactsActivity.this, "邮件发送服务已停止", Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ContactsActivity.this, "邮件发送服务未运行", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //判断列表内是否都为手机号码
    private boolean allIsCellphoneNumber(String contacts) {
        String[] phoneNumbers = contacts.split(" ");
        for (String phoneNumber : phoneNumbers) {
            if (!isCellphoneNumber(phoneNumber) && !phoneNumber.equals("")) {
                return false;
            }
        }
        return true;
    }

    //判断输入的是否是手机号码
    private boolean isCellphoneNumber(String phoneNumber) {
        if (phoneNumber.length() == 11) {
            for (int i = 0; i < phoneNumber.length(); i++) {
                if (!Character.isDigit(phoneNumber.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
