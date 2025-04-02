package com.example.finallcheck;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

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
        Log.d(TAG, "Payment result: " + result);
        if (responseUrl != null) {
            Log.d(TAG, "Response URL: " + responseUrl);
        }

        if (result != null) {
            String resultText = "Kết quả thanh toán: " + result;

            if ("payment.success".equals(result)) {
                resultText = "Thanh toán thành công!\n\n" +
                        "Số tiền: " + formatAmount(amount) + " VNĐ\n" +
                        "Mã giao dịch: " + txnRef;
            } else if ("payment.cancelled".equals(result)) {
                resultText = "Bạn đã hủy giao dịch!";
            } else if ("payment.error".equals(result)) {
                resultText = "Giao dịch thất bại!\n\n" +
                        "Vui lòng thử lại sau.";
            }

            txtResult.setText(resultText);
        } else {
            txtResult.setText("Không nhận được kết quả thanh toán.");
        }

        btnBack.setOnClickListener(v -> {
            // Quay về MainActivity và xóa stack
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            finish();
        });
    }

    private String formatAmount(String amountStr) {
        try {
            long amount = Long.parseLong(amountStr);
            return String.format(Locale.getDefault(), "%,d", amount).replace(',', '.');
        } catch (NumberFormatException e) {
            return amountStr;
        }
    }
}