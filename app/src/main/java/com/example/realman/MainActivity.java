package com.example.realman;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.metrics.Event;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import org.w3c.dom.Text;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
//TODO: 야 +버튼 누르면 생성되게 해라 샹련아 ㅋㅋ 그리고 일정 있는 날짜에는 .표시되게
public class MainActivity<FirebaseDatabase, DatabaseReference> extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private CalendarDay selectedDate;
    private ImageButton listBtn;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Calendar myCalendar = Calendar.getInstance();

    DatePickerDialog.OnDateSetListener myDatePicker = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        }


    };
    private void updateLabel() {
        String myFormat = "yyyy/MM/dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.KOREA);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalendarDay today = CalendarDay.today();//오늘날짜를 표시해주기 위해

        calendarView = findViewById(R.id.calendarView);

        listBtn = findViewById(R.id.ListBtn);

        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent3 = new Intent(MainActivity.this, ListActivity.class);
                startActivity(intent3);
            }
        });
        // 월, 요일을 한글로 보이게 설정 (MonthArrayTitleFormatter의 작동을 확인하려면 밑의 setTitleFormatter()를 지운다)
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(getResources().getTextArray(R.array.custom_months)));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(getResources().getTextArray(R.array.custom_weekdays)));

        List<CalendarDay> eventDates = new ArrayList<>();
        CollectionReference eventDatesRef = db.collection("cjryu").document("ZANKIWLXchApg24HfIyB").collection("subcollection");
        eventDatesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<CalendarDay> eventDates = new ArrayList<>(); // eventDates 변수 생성

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // Firestore에서 날짜 데이터 가져오기
                        String date = document.getString("date");

                        // 날짜를 Calendar 객체로 변환
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date parsedDate = null;
                        try {
                            parsedDate = dateFormat.parse(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if (parsedDate != null) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(parsedDate);

                            // CalendarDay 객체 생성 및 eventDates에 추가
                            CalendarDay calendarDay = CalendarDay.from(calendar);
                            eventDates.add(calendarDay);
                        }
                    }
                    //토일 색상 바꾸기
                    calendarView.addDecorators(new SaturdayDecorator(),
                            new SundayDecorator(),
                            new TodayColorDecorator(today),
                            new EventDecorator(eventDates));


                    calendarView.setOnDateChangedListener(new OnDateSelectedListener() { //달력의 날짜를 더블클릭하면 다이얼로그 호출
                        private long lastClickTime = 0;

                        //priority option
                        @Override
                        public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                            selectedDate = date;
                            long clickTime = System.currentTimeMillis();
                            if (clickTime - lastClickTime < 500) { //더블클릭 감지 : 500밀리초 이내로 클릭이 두번 발생하면 더블클릭으로 판단
                                // 더블클릭 이벤트 처리

                                AlertDialog.Builder plusmenu = new AlertDialog.Builder(MainActivity.this);
                                final EditText editText = new EditText(MainActivity.this);


                                editText.setHint("일정 내용을 추가해 주세요");

                                plusmenu.setView(editText);
                                final String[] items = getResources().getStringArray(R.array.priority_options); //string.xml에 있는 우선순위 목록 배열추가
                                final ArrayList<String> selectedItem = new ArrayList<String>();
                                selectedItem.add(items[0]);
                                plusmenu.setIcon(R.mipmap.ic_launcher);
                                plusmenu.setTitle("일정을 추가해보세요 ><");

                                plusmenu.setSingleChoiceItems(R.array.priority_options, 0, new DialogInterface.OnClickListener() { //단일 선택 목록(라디오버튼)
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        selectedItem.clear();
                                        selectedItem.add(items[which]);
                                    }
                                });

                                plusmenu.setPositiveButton("일정생성", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int year = selectedDate.getYear();
                                        int month = selectedDate.getMonth() + 1; // 월은 0부터 시작하므로 1을 더해줍니다.
                                        int day = selectedDate.getDay();
                                        String formattedDate = String.format("%04d-%02d-%02d", year, month, day);

                                        int selectedHour = myCalendar.get(Calendar.HOUR_OF_DAY);
                                        int selectedMinute = myCalendar.get(Calendar.MINUTE);
                                        String schedule = editText.getText().toString();
                                        String priority = selectedItem.get(0);

                                        String hourString = String.valueOf(selectedHour);
                                        String minuteString = String.valueOf(selectedMinute);

                                        Map<String, Object> data = new HashMap<>();
                                        data.put("date", formattedDate);
                                        data.put("hour", hourString);
                                        data.put("minute", minuteString);
                                        data.put("schedule", schedule);
                                        data.put("priority", priority);

                                        CollectionReference collectionRef = db.collection("cjryu").document("ZANKIWLXchApg24HfIyB").collection("subcollection");//Firestore의 collection에 접근

                                        collectionRef.document(formattedDate)
                                                .set(data)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(getApplicationContext(),
                                                                "선택한 날짜인 " + formattedDate + "에 일정 " + schedule + "를 추가했습니다. " + priority,
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(getApplicationContext(),
                                                                "일정 추가에 실패했습니다.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
//
                                    }
                                });

                                plusmenu.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog alertDialog = plusmenu.create();
                                alertDialog.show();
                            }
                            lastClickTime = clickTime;
                        }
                    });


                }
            }


        });
    }
}






