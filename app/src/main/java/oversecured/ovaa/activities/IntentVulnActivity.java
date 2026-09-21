package oversecured.ovaa.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class IntentVulnActivity extends Activity {
    private static final String CHANNEL = "ovaa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        redirectParcelableIntent();
        redirectParsedUri(getIntent().getStringExtra("uri"));
        launchConfiguredComponent();
        startConfiguredFragment();
        grantViaExplicitUri(getIntent().getStringExtra("grant_uri"));
        sendMutablePendingIntent();
        notifyWithPendingIntent();
        finish();
    }

    private void redirectParcelableIntent() {
        Intent forward = getIntent().getParcelableExtra("forward_intent");
        if (forward == null) {
            return;
        }
        startActivity(forward);
        sendBroadcast(forward);
        startService(forward);
    }

    private void redirectParsedUri(String uri) {
        if (uri == null) {
            return;
        }
        try {
            Intent parsed = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            startActivity(parsed);
        } catch (Exception ignored) {
        }
    }

    private void launchConfiguredComponent() {
        String packageName = getIntent().getStringExtra("package");
        String className = getIntent().getStringExtra("class");
        String action = getIntent().getStringExtra("action");
        if (packageName == null || className == null) {
            return;
        }
        Intent intent = new Intent(action);
        intent.setClassName(packageName, className);
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
    }

    private void startConfiguredFragment() {
        String fragmentClass = getIntent().getStringExtra("fragment");
        if (fragmentClass == null) {
            return;
        }
        try {
            android.app.Fragment fragment = (android.app.Fragment)
                    Class.forName(fragmentClass).getDeclaredConstructor().newInstance();
            getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
        } catch (Exception ignored) {
        }
    }

    private void grantViaExplicitUri(String uri) {
        String packageName = getIntent().getStringExtra("package");
        if (uri == null || packageName == null) {
            return;
        }
        grantUriPermission(packageName, Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private void sendMutablePendingIntent() {
        Intent empty = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, empty, PendingIntent.FLAG_MUTABLE);
        Intent carrier = new Intent("oversecured.ovaa.action.CALLBACK");
        carrier.putExtra("callback", pendingIntent);
        sendBroadcast(carrier);
    }

    private void notifyWithPendingIntent() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL, "ovaa", NotificationManager.IMPORTANCE_DEFAULT));

        Intent content = getIntent().getParcelableExtra("notification_intent");
        if (content == null) {
            content = new Intent();
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, content, PendingIntent.FLAG_MUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle(getIntent().getStringExtra("title"))
                .setContentText(getIntent().getStringExtra("text"))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
        manager.notify(1, notification);
    }
}
