package jp.co.cyberagent.stf.query;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import jp.co.cyberagent.stf.proto.Wire;

public class SetWifiEnabledResponder extends AbstractResponder {
    private static final String TAG = SetWifiEnabledResponder.class.getSimpleName();

    public SetWifiEnabledResponder(Context context) {
        super(context);
    }

    @Override
    public GeneratedMessageLite respond(Wire.Envelope envelope) throws InvalidProtocolBufferException {
        Wire.SetWifiEnabledRequest request =
                Wire.SetWifiEnabledRequest.parseFrom(envelope.getMessage());

        WifiManager wm = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null) {
            Log.e(TAG, "WifiManager is null");
            return buildResponse(envelope, false);
        }
        boolean success = false;
        try {
            success = wm.setWifiEnabled(request.getEnabled());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set wifi enabled", e);
        }
        return buildResponse(envelope, success);
    }
    private GeneratedMessageLite buildResponse(Wire.Envelope envelope, boolean successful) {
        return Wire.Envelope.newBuilder()
                .setId(envelope.getId())
                .setType(Wire.MessageType.SET_WIFI_ENABLED)
                .setMessage(Wire.SetWifiEnabledResponse.newBuilder()
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
