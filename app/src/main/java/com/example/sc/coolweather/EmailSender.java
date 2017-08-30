package com.example.sc.coolweather;

import com.sun.mail.util.MailSSLSocketFactory;

import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by sc on 2017/8/28 0028.
 */

public class EmailSender {

    private Session session;

    public EmailSender() throws GeneralSecurityException {
        // 指定发送邮件的主机为 smtp.qq.com
        String host = "smtp.qq.com";  //QQ 邮件服务器

        // 获取系统属性
        Properties properties = System.getProperties();

        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", host);

        properties.put("mail.smtp.auth", "true");

        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.socketFactory", sf);

        // 获取默认session对象
        session = Session.getDefaultInstance(properties, new Authenticator(){
            public PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication("1003616373@qq.com", ""); //发件人邮件用户名、密码
            }
        });
    }

    public void sendEmail( final String toAddress, final String textMessage) throws GeneralSecurityException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 收件人电子邮箱
                String to = toAddress;

                // 发件人电子邮箱
                String from = "1003616373@qq.com";

                try{
                    // 创建默认的 MimeMessage 对象
                    MimeMessage message = new MimeMessage(session);

                    // Set From: 头部头字段
                    message.setFrom(new InternetAddress(from));

                    // Set To: 头部头字段
                    message.addRecipient(Message.RecipientType.TO,
                            new InternetAddress(to));

                    // Set Subject: 头部头字段
                    message.setSubject("位置信息");

                    // 设置消息体
                    message.setText(textMessage);

                    // 发送消息
                    Transport.send(message);

                    System.out.println("Sent message successfully");
                }catch (MessagingException mex) {
                    mex.printStackTrace();
                }
            }
        }).start();

    }
}
