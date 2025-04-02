package com.example.finallcheck;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "VNPAY_RESULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView txtResult = findViewById(R.id.txtResult);
        Button btnBack = findViewById(R.id.btnBack);

        Intent intent = getIntent();
        String result = intent.getStringExtra("result");
        String amount = intent.getStringExtra("amount");
        String txnRef = intent.getStringExtra("txnRef");
        String responseUrl = intent.getStringExtra("response_url");

        // Log thông tin để debug
        Log.d(TAG, "Payment result: " + (result != null ? result : "null"));
        if (responseUrl != null) {
            Log.d(TAG, "Response URL: " + responseUrl);
        }

        // Xử lý kết quả thanh toán
        String resultText;
        if (result != null) {
            switch (result) {
                case "payment.success":
                    if (amount == null || txnRef == null) {
                        resultText = "Thanh toán thành công!\n\nDữ liệu giao dịch không đầy đủ.";
                        Log.w(TAG, "Missing amount or txnRef for successful payment");
                    } else {
                        resultText = "Thanh toán thành công!\n\n" +
                                "Số tiền: " + formatAmount(amount) + " VNĐ\n" +
                                "Mã giao dịch: " + txnRef;
                    }
                    break;
                case "payment.cancelled":
                    resultText = "Bạn đã hủy giao dịch!";
                    break;
                case "payment.error":
                    resultText = "Giao dịch thất bại!\n\n" +
                            "Vui lòng thử lại sau.";
                    break;
                default:
                    resultText = "Kết quả không xác định: " + result;
                    Log.w(TAG, "Unknown result code: " + result);
                    break;
            }
        } else {
            resultText = "Không nhận được kết quả thanh toán.";
            Log.e(TAG, "Result is null");
            Toast.makeText(this, "Không nhận được kết quả từ VNPay", Toast.LENGTH_SHORT).show();
        }

        txtResult.setText(resultText);

        // Xử lý nút Back
        btnBack.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        });
    }

    // Định dạng số tiền
    private String formatAmount(String amountStr) {
        if (amountStr == null) {
            return "N/A";
        }
        try {
            long amount = Long.parseLong(amountStr);
            return String.format(Locale.getDefault(), "%,d", amount).replace(',', '.');
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid amount format: " + amountStr, e);
            return amountStr;
        }
    }

    // Xử lý nút back của thiết bị
    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }
}