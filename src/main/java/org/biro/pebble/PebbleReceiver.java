package org.biro.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.Constants;

public class PebbleReceiver extends BroadcastReceiver {
    private static final String TAG = "PebbleReceiverß";
    Pebble parent = null;

    public PebbleReceiver(Pebble parent) {
        this.parent = parent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d(TAG, intent.getAction());
        if (intent.getAction() == Constants.INTENT_PEBBLE_CONNECTED) {
            parent.pebbleConnected(context);
        } else if (intent.getAction() == Constants.INTENT_PEBBLE_DISCONNECTED) {
            parent.pebbleDisconnected(context);
        }
    }
}
