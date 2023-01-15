package jp.co.cyberagent.stf;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ClipboardListener {

    private final Context context;
    private String currentContent;

    private ClipboardManager clipboardManager = null;

    private static ClipboardListener _instance = null;

    public static ClipboardListener instance(Context context) {
        if (_instance == null) {
            _instance = new ClipboardListener(context);
        }
        return _instance;
    }

    private ClipboardListener(final Context ctx) {
        context = ctx;

        new Handler(Looper.getMainLooper()).post(() -> {
            clipboardManager = ContextCompat.getSystemService(context, ClipboardManager.class);
            clipboardManager.addPrimaryClipChangedListener(this::onClipboardChanged);
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            new Thread(() -> {
                try {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                    Process process = Runtime.getRuntime().exec(new String[]{"logcat", "-T", timeStamp, "ClipboardService:E", "*:S"});
                    BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(
                            process.getInputStream()
                        )
                    );
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains(BuildConfig.APPLICATION_ID)) {
                            context.startActivity(ClipboardMonitorActivity.getIntent(context));
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    public void onClipboardChanged() {
        String content = "";
        try {
            ClipboardManager clipboardManager =
                (ClipboardManager) Service.getClipboardManager();
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item clipItem = clipData.getItemAt(i);
                    String clip = clipItem.coerceToText(context.getApplicationContext()).toString();
                    if (!clip.isEmpty()) {
                        content = clip;
                        break;
                    }
                }
            }
            if (content.equals(currentContent)) {
                return;
            }
            currentContent = content;
        } catch (Exception e) {
        }
    }

    public String getCurrentContent() {
        return currentContent;
    }
}
