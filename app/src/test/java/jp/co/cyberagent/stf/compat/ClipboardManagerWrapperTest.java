package jp.co.cyberagent.stf.compat;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.lang.reflect.Method;

public class ClipboardManagerWrapperTest {
    private static final String SHELL = "com.android.shell";

    // IClipboard.getPrimaryClip() as it is declared by the Android releases and by the vendor
    // builds that are known to differ from them. Only the parameter types matter here.
    interface Android9 {
        void getPrimaryClip(String pkg);
    }

    interface Android10 {
        void getPrimaryClip(String pkg, int userId);
    }

    interface Android13 {
        void getPrimaryClip(String pkg, String attributionTag, int userId);
    }

    interface Android14 {
        void getPrimaryClip(String pkg, String attributionTag, int userId, int deviceId);
    }

    interface VendorTagLast {
        void getPrimaryClip(String pkg, int userId, String attributionTag);
    }

    interface VendorUserOperate {
        void getPrimaryClip(String pkg, String attributionTag, int userId, int deviceId, boolean userOperate);
    }

    interface VendorMoreTags {
        void getPrimaryClip(String pkg, String attributionTag, String a, String b, int userId, int deviceId, boolean userOperate);
    }

    private static Object[] argumentsFor(Class<?> declaration) {
        Method[] methods = declaration.getDeclaredMethods();

        return ClipboardManagerWrapper.callerArguments(methods[0]);
    }

    @Test
    public void asksAsTheShellPackage() {
        assertArrayEquals(new Object[] {SHELL}, argumentsFor(Android9.class));
    }

    @Test
    public void asksForTheSystemUser() {
        assertArrayEquals(new Object[] {SHELL, 0}, argumentsFor(Android10.class));
    }

    @Test
    public void leavesTheAttributionTagOut() {
        assertArrayEquals(new Object[] {SHELL, null, 0}, argumentsFor(Android13.class));
    }

    @Test
    public void asksForTheDefaultDevice() {
        assertArrayEquals(new Object[] {SHELL, null, 0, 0}, argumentsFor(Android14.class));
    }

    @Test
    public void findsThePackageAndTheUserInVendorSignatures() {
        assertArrayEquals(new Object[] {SHELL, 0, null}, argumentsFor(VendorTagLast.class));
        assertArrayEquals(new Object[] {SHELL, null, 0, 0, true}, argumentsFor(VendorUserOperate.class));
        assertArrayEquals(new Object[] {SHELL, null, null, null, 0, 0, true}, argumentsFor(VendorMoreTags.class));
    }
}
