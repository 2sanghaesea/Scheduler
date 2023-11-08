package com.example.realman;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 알람에서 전달된 데이터 추출
        String schedule = intent.getStringExtra("schedule");
        String priority = intent.getStringExtra("priority");

        // 알림 생성
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        String channelId = "alarm_channel";
        String channelName = "Alarm Channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("일정 알림")
                .setContentText(schedule)
                .setAutoCancel(true);

        // 알림 표시
        notificationManager.notify(0, builder.build());
    }
}