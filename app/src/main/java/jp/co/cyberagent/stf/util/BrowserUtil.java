package jp.co.cyberagent.stf.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import java.util.List;

public class BrowserUtil {

    static final String HTTP_URL = "http://localhost";

    public static List<ResolveInfo> getBrowsers(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BrowserUtil.HTTP_URL));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL));
            }
            return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        }
        return pm.queryIntentActivities(intent, 0);
    }

    public static ResolveInfo getDefaultBrowser(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BrowserUtil.HTTP_URL));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return BrowserUtil.filterChooser(pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY)));
            }
            return BrowserUtil.filterChooser(pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY));
        }
        return BrowserUtil.filterChooser(pm.resolveActivity(intent, 0));
    }

    public static ResolveInfo filterChooser(ResolveInfo info) {
        return info.activityInfo.packageName.equals("android") ? null : info;
    }

    public static boolean isSameBrowser(ResolveInfo browserOne, ResolveInfo browserTwo) {
        return browserOne != null && browserTwo != null
            && browserOne.activityInfo != null && browserTwo.activityInfo != null
            && browserOne.activityInfo.packageName.equals(browserTwo.activityInfo.packageName)
            && browserOne.activityInfo.name.equals(browserTwo.activityInfo.name);
    }

    public static String getComponent(ResolveInfo info) {
        String packageName = info.activityInfo.packageName;
        String activityName = info.activityInfo.name;

        if (activityName.startsWith(packageName)) {
            return String.format("%s/%s", packageName, activityName.substring(packageName.length()));
        }

        return String.format("%s/%s", packageName, activityName);
    }
}
