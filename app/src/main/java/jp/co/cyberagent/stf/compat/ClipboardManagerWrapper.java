package jp.co.cyberagent.stf.compat;

import android.content.ClipData;
import android.net.Uri;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.co.cyberagent.stf.util.InternalApi;

/**
 * Reads the clipboard as the shell user.
 *
 * Since Android 10 only the default IME and the focused app may read the clipboard, so the
 * service gets nothing back. The shell package holds READ_CLIPBOARD_IN_BACKGROUND, and the agent
 * runs with the shell uid, so it can ask the clipboard service directly on behalf of that package.
 */
public class ClipboardManagerWrapper {
    private static final String SHELL_PACKAGE = "com.android.shell";

    private final Object clipboard;
    private final Method getPrimaryClip;

    public ClipboardManagerWrapper() {
        clipboard = InternalApi.getServiceAsInterface("clipboard", "android.content.IClipboard$Stub");

        if (clipboard == null) {
            throw new UnsupportedOperationException("The clipboard service is not available");
        }

        getPrimaryClip = findMethod("getPrimaryClip");
    }

    /**
     * @return the first non-empty text on the clipboard, or null if there is none
     */
    public CharSequence getText() throws InvocationTargetException, IllegalAccessException {
        ClipData clipData = (ClipData) getPrimaryClip.invoke(clipboard, callerArguments(getPrimaryClip));

        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                CharSequence text = item.getText();

                if (text == null) {
                    // There is no Context to resolve the content with, the address is all we have
                    Uri uri = item.getUri();
                    text = uri == null ? null : uri.toString();
                }

                if (text != null && text.length() > 0) {
                    return text;
                }
            }
        }

        return null;
    }

    private Method findMethod(String name) {
        for (Method method : clipboard.getClass().getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        throw new UnsupportedOperationException("IClipboard." + name + " is not available");
    }

    /*
     * The caller is described differently in every other Android release:
     *
     *   getPrimaryClip(String pkg)                                              up to Android 9
     *   getPrimaryClip(String pkg, int userId)                                  Android 10 to 12
     *   getPrimaryClip(String pkg, String attributionTag, int userId)           Android 13
     *   getPrimaryClip(String pkg, String attributionTag, int userId, int deviceId) since Android 14
     *
     * and some vendors append their own parameters. In all of them the first String is the
     * calling package and the first int is the user, so the arguments are derived from the
     * parameter types instead of listing every known signature. The remaining Strings are
     * optional tags, the remaining ints are ids whose default is 0, and a trailing boolean,
     * where a vendor has one, tells that the request comes from the user.
     */
    private static Object[] callerArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        boolean packageSet = false;

        for (int i = 0; i < types.length; i++) {
            if (types[i] == String.class) {
                arguments[i] = packageSet ? null : SHELL_PACKAGE;
                packageSet = true;
            }
            else if (types[i] == int.class) {
                // The shell always belongs to the system user and the default device
                arguments[i] = 0;
            }
            else if (types[i] == boolean.class) {
                arguments[i] = true;
            }
        }

        return arguments;
    }
}
