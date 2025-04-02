package com.example.finallcheck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentWebViewActivity extends AppCompatActivity {
    private static final String TAG = "VNPAY_WEBVIEW";
    private WebView webView;
    private ProgressBar progressBar;
    private String amount;
    private String txnRef;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_webview);

        // Khởi tạo view
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // Lấy dữ liệu từ intent
        String paymentUrl = getIntent().getStringExtra("payment_url");
        amount = getIntent().getStringExtra("amount");
        txnRef = getIntent().getStringExtra("txnRef");

        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "URL thanh toán không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Payment URL: " + paymentUrl);

        // Cấu hình WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Hiển thị progress bar khi tải trang
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // Xử lý điều hướng và URL callback
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "URL Loading: " + url);

                if (url.contains("sdk.merchantbackapp") ||
                        url.contains("vnp_ResponseCode") ||
                        url.contains("vnp_TransactionStatus")) {
                    handlePaymentResult(url);
                    return true;
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Không thể mở URL: " + url, e);
                        Toast.makeText(PaymentWebViewActivity.this, "Lỗi khi mở URL", Toast.LENGTH_SHORT).show();
                    }
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            // Sử dụng phiên bản cũ của onReceivedError
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView Error: " + description + " (Code: " + errorCode + ") at " + failingUrl);
                Toast.makeText(PaymentWebViewActivity.this, "Lỗi tải trang: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        // Tải URL thanh toán
        webView.loadUrl(paymentUrl);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void handlePaymentResult(String url) {
        Log.d(TAG, "Handling payment result: " + url);
        String resultCode = "payment.error";

        Uri uri = Uri.parse(url);
        String responseCode = uri.getQueryParameter("vnp_ResponseCode");
        String transactionStatus = uri.getQueryParameter("vnp_TransactionStatus");

        if ("00".equals(responseCode) || "00".equals(transactionStatus)) {
            resultCode = "payment.success";
        } else if ("24".equals(responseCode)) {
            resultCode = "payment.cancelled";
        } else {
            resultCode = "payment.error";
        }

        Intent resultIntent = new Intent(this, ResultActivity.class);
        resultIntent.putExtra("result", resultCode);
        resultIntent.putExtra("amount", amount);
        resultIntent.putExtra("txnRef", txnRef);
        resultIntent.putExtra("response_url", url);
        startActivity(resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            Intent resultIntent = new Intent(this, ResultActivity.class);
            resultIntent.putExtra("result", "payment.cancelled");
            resultIntent.putExtra("amount", amount);
            resultIntent.putExtra("txnRef", txnRef);
            startActivity(resultIntent);
            finish();
        }
    }
}