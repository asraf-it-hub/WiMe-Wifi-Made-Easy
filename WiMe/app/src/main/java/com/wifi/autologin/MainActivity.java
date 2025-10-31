package com.wifi.autologin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.*;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements NetworkMonitor.NetworkStateListener {
    private WebView webView;
    private CredentialManager credentialManager;
    private NetworkMonitor networkMonitor;
    private static final String URL = "https://hfw.vitap.ac.in:8090/httpclient.html";
    private boolean isAutoLoginAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        credentialManager = new CredentialManager(this);
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.setNetworkStateListener(this);

        setupWebView();
        networkMonitor.startMonitoring();
        
        // Initial load
        loadLoginPage();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebView", "Page finished loading: " + url);
                handlePageLoad();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e("WebView", "Error loading page: " + error.getDescription() + " for URL: " + request.getUrl());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.e("WebView", "SSL error: " + error.toString());
                // Ignore SSL certificate errors for the college's self-signed certificates
                handler.proceed(); // This allows the connection despite SSL errors
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("WebView", "Loading URL: " + url);
                return false; // Return false to let the WebView handle the URL normally
            }
        });
    }

    private void loadLoginPage() {
        webView.loadUrl(URL);
    }

    private void handlePageLoad() {
        String[] credentials = credentialManager.getCredentials();
        String username = credentials[0];
        String password = credentials[1];

        if (!username.isEmpty() && !password.isEmpty() && !isAutoLoginAttempted) {
            // Auto-fill credentials with proper escaping to prevent XSS
            String escapedUsername = escapeJavaScriptString(username);
            String escapedPassword = escapeJavaScriptString(password);
            
            webView.loadUrl("javascript:" +
                "document.getElementById('username').value = '" + escapedUsername + "';" +
                "document.getElementById('password').value = '" + escapedPassword + "';" +
                "submitRequest();");
            isAutoLoginAttempted = true;
        } else if (username.isEmpty() || password.isEmpty()) {
            // Show credentials input dialog
            showCredentialsDialog();
        }
    }

    private String escapeJavaScriptString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void showCredentialsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        final EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        layout.addView(usernameInput);
        layout.addView(passwordInput);
        
        new AlertDialog.Builder(this)
            .setTitle("Enter Credentials")
            .setView(layout)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String username = usernameInput.getText().toString();
                    String password = passwordInput.getText().toString();
                    
                    if (!username.isEmpty() && !password.isEmpty()) {
                        credentialManager.saveCredentials(username, password);
                        
                        // Auto-fill and submit after saving with proper escaping
                        String escapedUsername = escapeJavaScriptString(username);
                        String escapedPassword = escapeJavaScriptString(password);
                        
                        webView.loadUrl("javascript:" +
                            "document.getElementById('username').value = '" + escapedUsername + "';" +
                            "document.getElementById('password').value = '" + escapedPassword + "';" +
                            "submitRequest();");
                        isAutoLoginAttempted = true;
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        if (isConnected && networkMonitor.isOnCollegeWifi()) {
            // Network is available and we're on college WiFi, try to load and auto-login
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadLoginPage();
                    isAutoLoginAttempted = false;
                }
            }, 2000); // Wait 2 seconds for network to stabilize
        }
    }

    @Override
    public void onWifiConnected(String ssid) {
        // WiFi connected, check if it's the college network and auto-login
        if (ssid != null && (ssid.contains("VIT-AP") || ssid.contains("vitap"))) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadLoginPage();
                    isAutoLoginAttempted = false;
                }
            }, 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up network monitor if needed
    }
}
