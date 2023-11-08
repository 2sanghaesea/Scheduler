package com.example.realman;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//TODO: SETTING에 있는거 이미지뷰 누르면 다시 실행하는 거로 바꾸고 SETTING에 일정관리 들어가야해
public class FirstActivity extends AppCompatActivity {
    private ImageButton calendar, money, setting;
    private TextView scheduleList;
    private FirebaseFirestore db;
    private CollectionReference scheduleCollection;
    private TextView remainingBalanceTextView;
    private ImageView resetImage;
    private AlarmManager alarmManager;
    private static final String CHANNEL_ID = "MyChannelId";
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        calendar = findViewById(R.id.Calendar_Btn);
        money = findViewById(R.id.Money_Btn);
        setting = findViewById(R.id.Setting_Btn);
        scheduleList = findViewById(R.id.schedulelist);
        remainingBalanceTextView = findViewById(R.id.textView2);
        resetImage = findViewById(R.id.imageView);
        db = FirebaseFirestore.getInstance();
        scheduleCollection = db.collection("cjryu").document("ZANKIWLXchApg24HfIyB").collection("subcollection");
        // AlarmManager 인스턴스 생성
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        loadScheduleList();
        displayRemainingBalance();

        calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        money.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(FirstActivity.this, MoneyActivity.class);
                startActivity(intent2);
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent3 = new Intent(FirstActivity.this, ListActivity.class);
                startActivity(intent3);
            }
        });

        resetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent intent = getIntent();
                startActivity(intent);
            }
        });

        // 오늘보다 이전 일정 삭제
        deletePastSchedules();
        // 알람 설정
        setScheduleAlarms();
        // 알림 발송
        createNotificationChannel();
        sendNotification();
    }

    private void loadScheduleList() {
        scheduleCollection.orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder scheduleListBuilder = new StringBuilder();

                    if (queryDocumentSnapshots.isEmpty()) {
                        scheduleList.setText("일정이 없습니다.");
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String date = document.getString("date");
                            String hour = document.getString("hour");
                            String minute = document.getString("minute");
                            String schedule = document.getString("schedule");
                            String priority = document.getString("priority");

                            String scheduleItem = String.format("%s %s:%s - %s (%s)\n", date, hour, minute, schedule, priority);

                            scheduleListBuilder.append(scheduleItem);
                        }

                        scheduleList.setText(scheduleListBuilder.toString());
                    }
                });
    }

    private void displayRemainingBalance() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference balanceDocument = db.collection("cjryu").document("money");

        balanceDocument.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double remainingBalance = documentSnapshot.getDouble("amount");
                if (remainingBalance != null) {
                    String formattedBalance = String.format(Locale.getDefault(), "남은 잔액: %.2f원", remainingBalance);
                    String integerBalance = formattedBalance.substring(0, formattedBalance.indexOf("."));
                    remainingBalanceTextView.setText(integerBalance);
                } else {
                    remainingBalanceTextView.setText("잔액 정보 없음");
                }
            } else {
                remainingBalanceTextView.setText("잔액 정보 없음");
            }
        }).addOnFailureListener(e ->
                remainingBalanceTextView.setText("잔액 정보 불러오기 실패")
        );
    }

    private void deletePastSchedules() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateString = dateFormat.format(currentDate);

        scheduleCollection
                .whereLessThan("date", currentDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }
                    loadScheduleList();
                })
                .addOnFailureListener(e -> {
                    // 실패 시 호출되는 콜백 함수
                    Toast.makeText(FirstActivity.this, "일정 갱신 삭제 실패", Toast.LENGTH_SHORT).show();
                    Log.e("FirstActivity", "Failed to delete past schedules", e);
                });
    }

    private void setScheduleAlarms() {
        scheduleCollection
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 현재 날짜와 시간 정보
                    Calendar currentCalendar = Calendar.getInstance();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String hour = document.getString("hour");
                        String minute = document.getString("minute");
                        String schedule = document.getString("schedule");
                        String priority = document.getString("priority");

                        // 일정 날짜와 시간 정보를 Calendar 객체로 변환
                        Calendar scheduleCalendar = Calendar.getInstance();
                        scheduleCalendar.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
                        scheduleCalendar.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
                        scheduleCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(8)));
                        scheduleCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
                        scheduleCalendar.set(Calendar.MINUTE, Integer.parseInt(minute));
                        scheduleCalendar.set(Calendar.SECOND, 0);

                        // 일정 날짜와 시간이 현재 날짜와 시간과 동일한 경우에만 알람 설정
                        if (isSameDay(scheduleCalendar, currentCalendar)) {
                            // 알람 Intent 생성
                            Intent alarmIntent = new Intent(FirstActivity.this, AlarmReceiver.class);
                            alarmIntent.putExtra("schedule", schedule);
                            alarmIntent.putExtra("priority", priority);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    FirstActivity.this,
                                    0,
                                    alarmIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );

                            // 알람 시간 설정
                            long alarmTime = scheduleCalendar.getTimeInMillis();

                            // 알람 설정
                            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FirstActivity.this, "알람 설정 실패", Toast.LENGTH_SHORT).show();
                    Log.e("FirstActivity", "Failed to set schedule alarms", e);
                });
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Channel";
            String description = "My Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateString = dateFormat.format(currentDate);

        scheduleCollection
                .whereEqualTo("date", currentDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 첫 번째 문서의 데이터를 가져옴 (해당 예시에서는 첫 번째 문서만 처리)
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String scheduleTitle = document.getString("date") + "의 일정입니다";
                        String scheduleContent = document.getString("schedule");

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.notification_icon)
                                .setContentTitle(scheduleTitle)
                                .setContentText(scheduleContent)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    }
                })
                .addOnFailureListener(e -> {
                    // 실패 시 호출되는 콜백 함수
                    Toast.makeText(FirstActivity.this, "알림 발송 실패", Toast.LENGTH_SHORT).show();
                    Log.e("FirstActivity", "Failed to send notification", e);
                });
    }

}