package com.example.realman;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> scheduleList;
    private FirebaseFirestore db;
    private CollectionReference scheduleCollection;
    private Button correctionButton, deleteButton, updateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        listView = findViewById(R.id.ListView);
        correctionButton = findViewById(R.id.correction_btn);
        deleteButton = findViewById(R.id.delete_btn);
        updateButton = findViewById(R.id.update_btn);
        db = FirebaseFirestore.getInstance();
        scheduleCollection = db.collection("cjryu").document("ZANKIWLXchApg24HfIyB").collection("subcollection");

        scheduleList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scheduleList);
        listView.setAdapter(adapter);

        loadScheduleList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedSchedule = adapter.getItem(position);
                showDeleteDialog(selectedSchedule);
            }
        });

        correctionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = listView.getCheckedItemPosition();
                if (position != ListView.INVALID_POSITION) {
                    String selectedSchedule = scheduleList.get(position);
                    showEditDialog(selectedSchedule);
                } else {
                    Toast.makeText(ListActivity.this, "수정할 일정을 선택해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = listView.getCheckedItemPosition();
                if (position != ListView.INVALID_POSITION) {
                    String selectedSchedule = scheduleList.get(position);
                    showDeleteDialog(selectedSchedule);
                } else {
                    Toast.makeText(ListActivity.this, "삭제할 일정을 선택해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadScheduleList();
                Toast.makeText(ListActivity.this, "일정 목록을 갱신했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadScheduleList() {
        scheduleCollection.orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    scheduleList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String schedule = document.getString("schedule");
                        String priority = document.getString("priority");
                        String listItem = date + " - " + schedule + " - " + priority;
                        scheduleList.add(listItem);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ListActivity.this, "일정 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("ListActivity", "Failed to load schedule list", e);
                });
    }

    private void showEditDialog(String selectedSchedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("일정 수정");
        builder.setMessage("일정을 수정하시겠습니까?");

        // 수정할 내용을 입력받을 EditText 추가
        final EditText editSchedule = new EditText(this);
        editSchedule.setText(selectedSchedule);
        builder.setView(editSchedule);

        builder.setPositiveButton("수정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 선택된 일정을 수정
                String newSchedule = editSchedule.getText().toString();
                scheduleCollection.whereEqualTo("schedule", selectedSchedule).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // 수정 작업 수행
                                document.getReference().update("schedule", newSchedule)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    loadScheduleList();
                                                    Toast.makeText(ListActivity.this, "일정을 수정했습니다.", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(ListActivity.this, "일정 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(ListActivity.this, "일정 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    private void showDeleteDialog(String selectedSchedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("일정 삭제");
        builder.setMessage("일정을 삭제하시겠습니까?");
        builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 선택된 일정 삭제
                scheduleCollection.whereEqualTo("schedule", selectedSchedule).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // 삭제 작업 수행
                                document.getReference().delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    loadScheduleList();
                                                    Toast.makeText(ListActivity.this, "일정을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(ListActivity.this, "일정 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(ListActivity.this, "일정 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
