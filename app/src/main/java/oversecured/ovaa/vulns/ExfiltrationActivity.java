package oversecured.ovaa.vulns;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import oversecured.ovaa.utils.LoginUtils;

public class ExfiltrationActivity extends Activity {
    private static final String TAG = "ovaa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getStringExtra("path");
        String server = getIntent().getStringExtra("server");
        String phone = getIntent().getStringExtra("phone");
        String secret = LoginUtils.getInstance(this).getLoginData().password;

        sendViaImplicitActivityIntent(secret);
        sendViaImplicitBroadcastIntent(secret);
        sendViaImplicitServiceIntent(secret);
        sendEmptyImplicitIntents();
        sendFileViaImplicitIntent(path);
        copyToClipboard(secret, path);
        sendViaSms(phone, secret, path);
        uploadToServer(server, secret, path);
        logSensitiveData(secret);
        setAttackerResult();
        finish();
    }

    private void sendViaImplicitActivityIntent(String secret) {
        Intent intent = new Intent("oversecured.ovaa.action.SHARE_PROFILE");
        intent.putExtra("password", secret);
        startActivity(intent);
    }

    private void sendViaImplicitBroadcastIntent(String secret) {
        Intent intent = new Intent("oversecured.ovaa.action.PROFILE_UPDATED");
        intent.putExtra("password", secret);
        sendBroadcast(intent);
    }

    private void sendViaImplicitServiceIntent(String secret) {
        Intent intent = new Intent("oversecured.ovaa.action.SYNC");
        intent.putExtra("password", secret);
        startService(intent);
    }

    private void sendEmptyImplicitIntents() {
        startActivity(new Intent("oversecured.ovaa.action.PING_ACTIVITY"));
        sendBroadcast(new Intent("oversecured.ovaa.action.PING_BROADCAST"));
        startService(new Intent("oversecured.ovaa.action.PING_SERVICE"));
    }

    private void sendFileViaImplicitIntent(String path) {
        if (path == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void copyToClipboard(String secret, String path) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("password", secret));
        if (path != null) {
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "file", Uri.parse(path)));
        }
    }

    private void sendViaSms(String phone, String secret, String path) {
        if (phone == null) {
            return;
        }
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(phone, null, secret, null, null);
        if (path != null) {
            try {
                manager.sendDataMessage(phone, null, (short) 0,
                        FileUtils.readFileToByteArray(new File(path)), null, null);
            } catch (IOException ignored) {
            }
        }
    }

    private void uploadToServer(String server, String secret, String path) {
        if (server == null) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(server).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-Session", secret);
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(("password=" + secret).getBytes());
                if (path != null) {
                    out.write(FileUtils.readFileToByteArray(new File(path)));
                }
            }
            connection.getResponseCode();
        } catch (IOException ignored) {
        }
    }

    private void logSensitiveData(String secret) {
        Log.d(TAG, "current password: " + secret);
        Log.v(TAG, "request path: " + getIntent().getStringExtra("path"));
    }

    private void setAttackerResult() {
        Intent result = (Intent) getIntent().getParcelableExtra("result_intent");
        if (result != null) {
            setResult(RESULT_OK, result);
        } else {
            Intent own = new Intent();
            own.putExtra("password", LoginUtils.getInstance(this).getLoginData().password);
            setResult(RESULT_OK, own);
        }
    }
}
