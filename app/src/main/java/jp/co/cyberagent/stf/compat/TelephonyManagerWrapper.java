package jp.co.cyberagent.stf.compat;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressLint({"MissingPermission", "HardwareIds"})
public class TelephonyManagerWrapper {
    private static final String SHELL_PACKAGE_NAME = "com.android.shell";
    private static final int SHELL_UID = 2000;

    private final TelephonyManager telephonyManager;

    public TelephonyManagerWrapper() {
        registerTelephonyServiceManager();

        Context context = getSystemContext();
        telephonyManager = context == null ? null : createTelephonyManager(asShell(context));
    }

    public Map<String, String> getSubscriberProperties() {
        Map<String, String> properties = new LinkedHashMap<String, String>();

        if (telephonyManager == null) {
            return properties;
        }

        put(properties, "imei", getImei());
        put(properties, "imsi", read(() -> telephonyManager.getSubscriberId()));
        put(properties, "iccid", read(() -> telephonyManager.getSimSerialNumber()));
        put(properties, "phoneNumber", read(() -> telephonyManager.getLine1Number()));

        return properties;
    }

    private String getImei() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String imei = read(() -> telephonyManager.getImei());
            if (!TextUtils.isEmpty(imei)) {
                return imei;
            }
        }
        return read(() -> telephonyManager.getDeviceId());
    }

    private static void put(Map<String, String> properties, String name, String value) {
        if (!TextUtils.isEmpty(value)) {
            properties.put(name, value);
        }
    }

    private static String read(StringReader reader) {
        try {
            return reader.read();
        }
        catch (Throwable e) {
            System.err.printf("Unable to read subscriber property: %s\n", e.getMessage());
            return null;
        }
    }

    private static void registerTelephonyServiceManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }

        try {
            Class<?> initializer =
                    Class.forName("android.telephony.TelephonyFrameworkInitializer");
            Class<?> serviceManager = Class.forName("android.os.TelephonyServiceManager");
            Method setter = initializer
                    .getMethod("setTelephonyServiceManager", serviceManager);
            setter.invoke(null, serviceManager.getConstructor().newInstance());
        }
        catch (Throwable e) {
            System.err.printf("Unable to register the telephony service manager: %s\n",
                    e.getMessage());
        }
    }

    private static Context getSystemContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getDeclaredMethod("systemMain").invoke(null);
            return (Context) activityThread.getDeclaredMethod("getSystemContext").invoke(thread);
        }
        catch (Throwable e) {
            System.err.printf("Unable to create a system context: %s\n", e.getMessage());
            return null;
        }
    }

    private static Context asShell(Context context) {
        return Process.myUid() == SHELL_UID ? new ShellContext(context) : context;
    }

    private static TelephonyManager createTelephonyManager(Context context) {
        try {
            Constructor<TelephonyManager> constructor =
                    TelephonyManager.class.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context);
        }
        catch (Throwable e) {
            return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    @FunctionalInterface
    private interface StringReader {
        String read();
    }

    private static class ShellContext extends ContextWrapper {
        ShellContext(Context base) {
            super(base);
        }

        @Override
        public String getPackageName() {
            return SHELL_PACKAGE_NAME;
        }

        @Override
        public String getOpPackageName() {
            return SHELL_PACKAGE_NAME;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @TargetApi(Build.VERSION_CODES.S)
        @Override
        public AttributionSource getAttributionSource() {
            return new AttributionSource.Builder(Process.myUid())
                    .setPackageName(SHELL_PACKAGE_NAME)
                    .build();
        }
    }
}
