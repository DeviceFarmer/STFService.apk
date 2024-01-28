package jp.co.cyberagent.stf.query;

import java.lang.reflect.Method;
import java.util.Set;
import android.content.Context;
import android.os.Build;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import jp.co.cyberagent.stf.proto.Wire;


public class DoCleanBluetoothBondedDevicesResponder extends AbstractResponder {
    private static final String TAG = DoCleanBluetoothBondedDevicesResponder.class.getSimpleName();

    public DoCleanBluetoothBondedDevicesResponder(Context context) {
        super(context);
    }

    @Override
    public GeneratedMessageLite respond(Wire.Envelope envelope) throws InvalidProtocolBufferException {
        Wire.DoCleanBluetoothBondedDevicesRequest request =
                Wire.DoCleanBluetoothBondedDevicesRequest.parseFrom(envelope.getMessage());

        boolean successful = false;

        // Return early if the API level is not sufficient
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return buildResponse(envelope, successful);
        }

        // Return early if Bluetooth is not available
        BluetoothManager bm = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) {
            return buildResponse(envelope, successful);
        }

        // Return early if cannot get Bluetooth adapter
        BluetoothAdapter ba = bm.getAdapter();
        if (ba == null) {
            return buildResponse(envelope, successful);
        }

        // Return early if no access to bonded devices
        Set<BluetoothDevice> pairedDevices;
        try {
            pairedDevices = ba.getBondedDevices();
        } catch (SecurityException exception) {
            Log.w(TAG, "Failed to un-pair devices: " + exception.getMessage());
            return buildResponse(envelope, successful);
        }

        int successCount = 0;
        for (BluetoothDevice device : pairedDevices) {
            try {
                Method method = device.getClass().getMethod("removeBond");
                method.invoke(device);
                successCount++;
            } catch (Exception exception) {
                Log.w(TAG, "Failed to un-pair device: " + device.getAddress());
            }
        }
        Log.d(TAG, "Un-paired " + successCount + " devices successfully");
        successful = true;
        return buildResponse(envelope, successful);
    }

    private GeneratedMessageLite buildResponse(Wire.Envelope envelope, boolean successful) {
        return Wire.Envelope.newBuilder()
                .setId(envelope.getId())
                .setType(Wire.MessageType.DO_CLEAN_BLUETOOTH_BONDED_DEVICES)
                .setMessage(Wire.DoCleanBluetoothBondedDevicesResponse.newBuilder()
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
