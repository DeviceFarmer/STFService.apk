package jp.co.cyberagent.stf.query;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.util.Log;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.core.content.ContextCompat;


import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import jp.co.cyberagent.stf.proto.Wire;

public class SetBluetoothEnabledResponder extends AbstractResponder {
    private static final String TAG = SetBluetoothEnabledResponder.class.getSimpleName();
    
    public SetBluetoothEnabledResponder(Context context) {
        super(context);
    }

    @Override
    public GeneratedMessageLite respond(Wire.Envelope envelope) throws InvalidProtocolBufferException {
        Wire.SetBluetoothEnabledRequest request =
                Wire.SetBluetoothEnabledRequest.parseFrom(envelope.getMessage());

        boolean successful;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // getAdapter() is only available since Android API level 18
            Log.e(TAG, "API level is not sufficient");
            return buildResponse(envelope, false);
        }
        BluetoothManager bm = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) {
            // No Bluetooth available
            Log.e(TAG, "BluetoothManager is null");
            return buildResponse(envelope, false);
        }
        BluetoothAdapter ba = bm.getAdapter();
        if (ba == null) {
            // No Bluetooth available
            Log.e(TAG, "BluetoothAdapter is null");
            return buildResponse(envelope, false);
        }
        int dumpPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
        if (dumpPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted to change Bluetooth state");
        } else {
            Log.w(TAG, "Permission denied to change Bluetooth state");
        }
            
        try {
            if (request.getEnabled()) {
                ba.enable();
            } else {
                ba.disable();
            }
            return buildResponse(envelope, true);
        } catch (SecurityException exception) {
            Log.e(TAG, "Failed to set Bluetooth enabled: " + exception.getMessage());
            return buildResponse(envelope, false);
        }
    }
    private GeneratedMessageLite buildResponse(Wire.Envelope envelope, boolean successful) {
        return Wire.Envelope.newBuilder()
                .setId(envelope.getId())
                .setType(Wire.MessageType.SET_BLUETOOTH_ENABLED)
                .setMessage(Wire.SetBluetoothEnabledResponse.newBuilder()
                        .setSuccess(successful)
                        .build()
                        .toByteString())
                .build();
    }


    @Override
    public void cleanup() {
        // No-op
    }
}
