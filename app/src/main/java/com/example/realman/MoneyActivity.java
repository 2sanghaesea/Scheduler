package com.example.realman;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MoneyActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private DocumentReference balanceRef;
    private DocumentReference transactionsRef;
    private TextView balanceTextView;
    private RadioGroup transactionTypeRadioGroup;

    private Button addBtn;
    private double currentBalance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_money);

        db = FirebaseFirestore.getInstance();
        balanceRef = db.collection("cjryu").document("money");
        transactionsRef = db.collection("cjryu").document("money");

        balanceTextView = findViewById(R.id.balanceTextView);
        transactionTypeRadioGroup = findViewById(R.id.transactionTypeRadioGroup);


        addBtn = findViewById(R.id.addButton);

        // Load current balance from Firestore
        loadCurrentBalance();


        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder addmenu = new AlertDialog.Builder(MoneyActivity.this);
                LinearLayout layout = new LinearLayout(MoneyActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText editText1 = new EditText(MoneyActivity.this);
                final EditText editText2 = new EditText(MoneyActivity.this);
                editText1.setHint("금액을 입력해 주세요");
                editText2.setHint("입/출금 사유를 입력해주세요");
                layout.addView(editText1);
                layout.addView(editText2);

                addmenu.setView(layout);
                addmenu.setPositiveButton("Record", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String transactionType = getSelectedTransactionType();
                        String amountText = editText1.getText().toString().trim();
                        String reason = editText2.getText().toString().trim();

                        if (amountText.isEmpty()) {
                            Toast.makeText(MoneyActivity.this, "금액을 입력하세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double transactionAmount = Double.parseDouble(amountText);
                        if (transactionAmount <= 0) {
                            Toast.makeText(MoneyActivity.this, "금액은 0보다 큰 값을 입력해야 합니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (transactionType.equals("출금")) {
                            transactionAmount *= -1; // Negative amount for withdrawals
                        }
                        // Update the balance
                        currentBalance += transactionAmount;
                        updateBalanceTextView();

                        // Record the transaction in Firestore
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String timestamp = dateFormat.format(new Date());

                        Map<String, Object> transaction = new HashMap<>();
                        transaction.put("amount", transactionAmount);
                        transaction.put("reason", reason);
                        transaction.put("timestamp", timestamp);

                        transactionsRef.collection("transactions")
                                .document()
                                .set(transaction)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MoneyActivity.this, "트랜잭션 기록 완료", Toast.LENGTH_SHORT).show();
                                    loadTransactionHistory();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(MoneyActivity.this, "트랜잭션 기록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                });

                addmenu.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                addmenu.create().show();
            }
        });
    }

    private void loadCurrentBalance() {
        balanceRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double balance = documentSnapshot.getDouble("amount");
                if (balance != null) {
                    currentBalance = balance;
                    updateBalanceTextView();
                }
            }
        });
    }

    private void updateBalanceTextView() {
        String formattedBalance = String.format(Locale.getDefault(), "남은 잔액: %.2f원", currentBalance);
        String integerBalance = formattedBalance.substring(0, formattedBalance.indexOf("."));
        balanceTextView.setText(integerBalance);

        balanceRef.update("amount", currentBalance)
                .addOnSuccessListener(aVoid -> {
                    // Balance update successful
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }

//    private void recordTransaction() {
//        String transactionType = getSelectedTransactionType();
//        String amountText = transactionAmountEditText.getText().toString().trim();
//
//        if (amountText.isEmpty()) {
//            Toast.makeText(this, "금액을 입력하세요.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        double transactionAmount = Double.parseDouble(amountText);
//        if (transactionAmount <= 0) {
//            Toast.makeText(this, "금액은 0보다 큰 값을 입력해야 합니다.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (transactionType.equals("출금")) {
//            transactionAmount *= -1; // Negative amount for withdrawals
//        }
//
//        // Update the balance
//        currentBalance += transactionAmount;
//        updateBalanceTextView();
//
//        // Update the balance field in Firestore
//        balanceRef.update("amount", currentBalance)
//                .addOnSuccessListener(aVoid -> {
//                    // Balance update successful
//                })
//                .addOnFailureListener(e -> {
//                    // Handle failure
//                });
//
//        // Record the transaction in Firestore
//        String timestamp = DateFormat.getDateTimeInstance().format(new Date());
//
//        Map<String, Object> transaction = new HashMap<>();
//        transaction.put("amount", transactionAmount);
//        transaction.put("timestamp", timestamp);
//
//        transactionsRef.collection("transactions")
//                .add(transaction)
//                .addOnSuccessListener(documentReference -> {
//                    Toast.makeText(MoneyActivity.this, "트랜잭션 기록 완료", Toast.LENGTH_SHORT).show();
//                    clearTransactionFields();
//                    loadTransactionHistory();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(MoneyActivity.this, "트랜잭션 기록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                );
//    }
//    private void clearTransactionFields() {
//        transactionTypeRadioGroup.clearCheck();
//        transactionAmountEditText.setText("");
//    }

    private void loadTransactionHistory() {
        transactionsRef.collection("transactions")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder transactionHistory = new StringBuilder();
                    for (DocumentSnapshot document : querySnapshot) {
                        Double amount = document.getDouble("amount");
                        String timestamp = document.getString("timestamp");
                        String reason = document.getString("reason");
                        if (amount != null && timestamp != null) {
                            String transactionLine = String.format(Locale.getDefault(), "%s: %.2f원 사유: %s\n", timestamp, amount,reason);

                            transactionHistory.append(transactionLine);
                        }
                    }
                    TextView transactionHistoryTextView = findViewById(R.id.transactionHistoryContentTextView);
                    transactionHistoryTextView.setText(transactionHistory.toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MoneyActivity.this, "트랜잭션 내역 가져오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private String getSelectedTransactionType() {
        int selectedId = transactionTypeRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        return selectedRadioButton.getText().toString();
    }
}
