package oversecured.ovaa.vulns;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import oversecured.ovaa.utils.LoginUtils;

public class ConnectivityActivity extends Activity {
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String CHANNEL = "ovaa_connectivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getStringExtra("path");
        String payload = getIntent().getStringExtra("payload");
        String secret = LoginUtils.getInstance(this).getLoginData().password;

        sendOverBluetooth(path, payload, secret);
        sendOverNfc(path, payload, secret);
        postNotification();
        hideNotifications();
        finish();
    }

    private byte[] payloadFor(String path, String payload, String secret) {
        if (path != null) {
            try {
                return FileUtils.readFileToByteArray(new File(path));
            } catch (IOException ignored) {
            }
        }
        return (payload != null ? payload : secret).getBytes();
    }

    private void sendOverBluetooth(String path, String payload, String secret) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            try {
                BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID);
                socket.connect();
                try (OutputStream out = socket.getOutputStream()) {
                    out.write(payloadFor(path, payload, secret));
                }
                socket.close();
            } catch (IOException ignored) {
            }
            return;
        }
    }

    private void sendOverNfc(String path, String payload, String secret) {
        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }
        NdefRecord record = NdefRecord.createMime("application/octet-stream",
                payloadFor(path, payload, secret));
        Ndef ndef = Ndef.get(tag);
        try {
            ndef.connect();
            ndef.writeNdefMessage(new NdefMessage(new NdefRecord[] {record}));
            ndef.close();
        } catch (IOException | FormatException ignored) {
        }
    }

    private void postNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL, "ovaa", NotificationManager.IMPORTANCE_HIGH));
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle(getIntent().getStringExtra("title"))
                .setContentText(getIntent().getStringExtra("text"))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        manager.notify(getIntent().getIntExtra("id", 2), notification);
    }

    private void hideNotifications() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        int id = getIntent().getIntExtra("cancel_id", -1);
        if (id >= 0) {
            manager.cancel(id);
        }
        if (getIntent().getBooleanExtra("cancel_all", false)) {
            manager.cancelAll();
        }
    }
}
