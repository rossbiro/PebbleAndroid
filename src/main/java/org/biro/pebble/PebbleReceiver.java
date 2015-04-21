package org.biro.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PebbleReceiver extends BroadcastReceiver {
    private static final String TAG = "PebbleReceiverß";
    public PebbleReceiver(Pebble parent) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d(TAG, intent.getAction());
    }
}
