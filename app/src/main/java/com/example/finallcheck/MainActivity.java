package com.example.finallcheck;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vnpay.authentication.VNP_AuthenticationActivity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "VNPAY_APP";
    private EditText edtAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtAmount = findViewById(R.id.edtAmount);
        Button btnPay = findViewById(R.id.btnPay);

        btnPay.setOnClickListener(view -> {
            String amountStr = edtAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                long amount = Long.parseLong(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                openVnpaySdk(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openVnpaySdk(long amount) {
        try {
            String txnRef = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            long vnpAmount = amount * 100; // Chuyển sang đơn vị VNPay

            // Kiểm tra số tiền hợp lệ
            if (vnpAmount < 100000) {
                Log.e("VNPAY_SDK", "Số tiền quá nhỏ: " + vnpAmount);
                Toast.makeText(this, "Số tiền tối thiểu là 1,000 VND", Toast.LENGTH_SHORT).show();
                return;
            }

            String createDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

            // Mã hóa thông tin để tránh lỗi 97 (Invalid Parameter)
            String orderInfo = URLEncoder.encode("Thanh toán đơn hàng " + txnRef, StandardCharsets.UTF_8.toString());
            String returnUrl = URLEncoder.encode("https://sandbox.vnpayment.vn/return_url", StandardCharsets.UTF_8.toString());

            // Tạo URL thanh toán
            String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                    + "?vnp_Version=2.1.0"
                    + "&vnp_Command=pay"
                    + "&vnp_TmnCode=LWO9ZUAK"
                    + "&vnp_Amount=" + vnpAmount
                    + "&vnp_CurrCode=VND"
                    + "&vnp_TxnRef=" + txnRef
                    + "&vnp_OrderInfo=" + orderInfo
                    + "&vnp_OrderType=other"
                    + "&vnp_Locale=vn"
                    + "&vnp_ReturnUrl=" + returnUrl
                    + "&vnp_IpAddr=127.0.0.1"
                    + "&vnp_CreateDate=" + createDate;

            Log.d("VNPAY_SDK", "Payment URL: " + paymentUrl);

            // Gửi Intent đến VNPAY SDK
            Intent intent = new Intent(this, VNP_AuthenticationActivity.class);
            intent.putExtra("url", paymentUrl);
            intent.putExtra("tmn_code", "LWO9ZUAK");
            intent.putExtra("scheme", "paymentapp");
            intent.putExtra("is_sandbox", true);

            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("VNPAY_SDK", "Lỗi tạo URL: " + e.getMessage());
        }
    }
    private void handlePaymentResult(String resultCode) {
        switch (resultCode) {
            case "00":
                Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                break;
            case "24":
                Toast.makeText(this, "Bạn đã hủy giao dịch!", Toast.LENGTH_LONG).show();
                break;
            case "99":
                Toast.makeText(this, "Lỗi giao dịch, vui lòng thử lại!", Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Kết quả không xác định: " + resultCode, Toast.LENGTH_LONG).show();
                break;
        }
    }

}
