package com.example.finallcheck;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "VNPAY_APP";
    private static final int VNPAY_REQUEST_CODE = 1001;
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
                // Kiểm tra số thập phân
                if (amountStr.contains(".") || amountStr.contains(",")) {
                    Toast.makeText(this, "Số tiền không được chứa phần thập phân", Toast.LENGTH_SHORT).show();
                    return;
                }

                long amount = Long.parseLong(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                openVnpaySdk(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ, chỉ nhập số nguyên", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void openVnpaySdk(long amount) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String txnRef = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            long vnpAmount = amount * 100;

            if (vnpAmount < 1000) {
                Log.e("VNPAY_SDK", "Số tiền quá nhỏ: " + vnpAmount);
                Toast.makeText(this, "Số tiền tối thiểu là 10 VND", Toast.LENGTH_SHORT).show();
                return;
            }

            String createDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            String orderInfo = "Thanh toán đơn hàng " + txnRef; // Không encode ở đây, encode sau

            // Các tham số cần gửi, sắp xếp theo thứ tự alphabet
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Amount", String.valueOf(vnpAmount));
            params.put("vnp_Command", "pay");
            params.put("vnp_CreateDate", createDate);
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_Locale", "vn");
            params.put("vnp_OrderInfo", orderInfo);
            params.put("vnp_OrderType", "other");
            params.put("vnp_ReturnUrl", "https://sandbox.vnpayment.vn/return_url");
            params.put("vnp_TmnCode", "1VYBIYQP");
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_Version", "2.1.0");

            StringBuilder dataToSign = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (dataToSign.length() > 0) dataToSign.append("&");
                dataToSign.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }


            String secretKey = "NOH6MBGNLQL9O9OMMFMZ2AX8NIEP50W1";
            String vnpSecureHash = hmacSHA512(secretKey, dataToSign.toString());

            // Tạo URL thanh toán
            String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + dataToSign.toString() +
                    "&vnp_SecureHash=" + vnpSecureHash;

            Log.d("VNPAY_SDK", "Payment URL: " + paymentUrl);

            Intent intent = new Intent(this, PaymentWebViewActivity.class);
            intent.putExtra("payment_url", paymentUrl);
            intent.putExtra("amount", String.valueOf(amount));
            intent.putExtra("txnRef", txnRef);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("VNPAY_SDK", "Lỗi tạo URL: " + e.getMessage());
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm tính HMAC-SHA512
    private String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VNPAY_REQUEST_CODE && data != null) {
            String responseCode = data.getStringExtra("vnp_ResponseCode");
            if (responseCode != null) {
                handlePaymentResult(responseCode);
            } else {
                Toast.makeText(this, "Không nhận được kết quả từ VNPay", Toast.LENGTH_SHORT).show();
            }
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