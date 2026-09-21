package oversecured.ovaa.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ResultTheftActivity extends Activity {
    private static final int PICK_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("*/*");
        startActivityForResult(pick, PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_CODE || data == null) {
            finish();
            return;
        }
        Uri uri = data.getData();
        if (uri != null) {
            copyToExternalStorage(uri);
            uploadToServer(uri, data.getStringExtra("server"));
            copyToClipboard(uri);
        }
        forwardResult(data);
        finish();
    }

    private byte[] read(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private void copyToExternalStorage(Uri uri) {
        File destination = new File(getExternalFilesDir(null), String.valueOf(uri.getLastPathSegment()));
        try (OutputStream out = new FileOutputStream(destination)) {
            out.write(read(uri));
        } catch (IOException ignored) {
        }
    }

    private void uploadToServer(Uri uri, String server) {
        if (server == null) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(server).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(read(uri));
            }
            connection.getResponseCode();
        } catch (IOException ignored) {
        }
    }

    private void copyToClipboard(Uri uri) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newRawUri("picked", uri));
    }

    private void forwardResult(Intent data) {
        setResult(RESULT_OK, data);
        startActivity(new Intent(this, VulnerableWebViewActivity.class)
                .putExtra("url", data.getStringExtra("next_url")));
    }
}
