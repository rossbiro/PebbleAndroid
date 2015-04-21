package org.biro.pebble;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Random;
import java.util.UUID;

/**
 * AndroidRun, basic runner's android application. Calculates distance, speed
 * and other useful values taken from GPS device.
 * <p/>
 * This file is part of the Pebble Canvas Interface
 * <p/>
 * Copyright (C) 2015 Ross Biro
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * <p/>
 * <p/>
 * Created by rossb on 4/17/15.
 */
public class Pebble {
    private final static String TAG = "Pebble: ";
    public static final int FUNC_NO_FUNC = 0;
    public static final int FUNC_NEW_WINDOW = 1;
    public static final int FUNC_NEW_TEXT_LAYER = 2;
    public static final int FUNC_APPLY_ATTRIBUTES = 3;
    public static final int FUNC_PUSH_WINDOW = 4;

    public static final int KEY_STATUS = 0;
    public static final int KEY_API_VERSION = 1;
    public static final int KEY_ERROR_CODE = 2;
    public static final int KEY_RETURN_VALUE = 3;
    public static final int KEY_TRANSACTION_ID = 4;
    public static final int KEY_WINDOW_ID = 5;
    public static final int KEY_TEXT_LAYER_ID = 6;
    public static final int KEY_METHOD_ID = 7;
    public static final int KEY_ATTRIBUTE_FONT = 8;
    public static final int KEY_ATTRIBUTE_BG_COLOR = 9;
    public static final int KEY_ATTRIBUTE_FG_COLOR = 10;
    public static final int KEY_ATTRIBUTE_TEXT = 11;
    public static final int KEY_ATTRIBUTE_ALIGNMENT = 12;
    public static final int KEY_ATTRIBUTE_RECT = 13;

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERR = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_STOPPED = 3;

    public static final int ROOT_WINDOW_HANDLE = 0;

    public static final int ENOMEM = 1;
    public static final int ENOWINDOW = 2;
    public static final int ENOLAYER = 3;
    public static final int EINVALID_OP = 4;
    public static final int EINVALID_TRANSACTION = 5;

    public static final int COLOR_BLACK = 0;
    public static final int COLOR_WHITE = 1;
    public static final int COLOR_CLEAR = -1;

    public static final int TEXT_ALIGNMENT_LEFT = 0;
    public static final int TEXT_ALIGNMENT_CENTER = 1;
    public static final int TEXT_ALIGNMENT_RIGHT = 2;

    public interface PebbleFinishedCallback {
        public void processIncoming(Context ctx, int tid,
                                    PebbleDictionary resp, PebbleDictionary req);
    };

    private class PacketInfo {
        PebbleFinishedCallback w;
        PebbleDictionary data;

        PacketInfo(PebbleFinishedCallback w, PebbleDictionary data) {
            this.w = w;
            this.data = data;
        }
    };

    private final static UUID PEBBLE_APP_UUID =
                UUID.fromString("9312d524-6e77-47e4-96ed-e67bd11ce1d5");

    private boolean started = false;
    private boolean connected = false;

    private PebbleReceiver mPebbleReceiver = new PebbleReceiver(this);

    private PebbleKit.PebbleDataReceiver mPebbleDataReceiver =
                new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
        @Override
        public void receiveData(Context ctx, int ptid, final PebbleDictionary pebbleDictionary) {
            if (pebbleDictionary.contains(KEY_STATUS)) {
                // It's a response to something we sent or a status message.
                switch(pebbleDictionary.getUnsignedIntegerAsLong(KEY_STATUS).intValue()) {
                    case STATUS_OK:
                    case STATUS_ERR:
                        if (!pebbleDictionary.contains(KEY_TRANSACTION_ID)) {
                            Log.d(TAG, "Packet Without Transaction ID");
                            nack(ctx, ptid);
                            return;
                        }
                        ack(ctx, ptid);
                        int tid = pebbleDictionary.getUnsignedIntegerAsLong(KEY_TRANSACTION_ID).intValue();
                        PacketInfo info = getPebbleFinished(tid);
                        removeInflight(tid);
                        if (info != null && info.w != null) {
                            info.w.processIncoming(ctx, tid, pebbleDictionary, info.data);
                        }
                        return;

                    case STATUS_STARTED:
                        started = true;
                        ack(ctx, ptid);
                        return;

                    case STATUS_STOPPED:
                        started = false;
                        ack(ctx, ptid);
                        return;

                    default:
                        nack(ctx, ptid);
                        Log.e(TAG, "Unknown status in read: " + pebbleDictionary.getUnsignedIntegerAsLong(KEY_STATUS));
                        return;
                }
            }
        }
    };

    private void removeInflight(int tid) {
        synchronized (inflight) {
            inflight.delete(tid);
        }
    }

    private PebbleKit.PebbleAckReceiver mPebbleAckReceiver =
            new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
        @Override
        public void receiveAck(Context context, int i) {

        }
    };

    private PebbleKit.PebbleNackReceiver mPebbleNackReceiver =
            new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
        @Override
        public void receiveNack(Context context, int i) {

        }
    };

    private PebbleKit.PebbleDataLogReceiver mPebbleLogReceiver =
            new PebbleKit.PebbleDataLogReceiver(PEBBLE_APP_UUID) {
        @Override
        public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, Long data) {
            super.receiveData(context, logUuid, timestamp, tag, data);
        }

        @Override
        public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, byte[] data) {
            super.receiveData(context, logUuid, timestamp, tag, data);
        }

        @Override
        public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, int data) {
            super.receiveData(context, logUuid, timestamp, tag, data);
        }

        @Override
        public void onFinishSession(Context context, UUID logUuid, Long timestamp, Long tag) {
            super.onFinishSession(context, logUuid, timestamp, tag);
        }

    };

    // My transaction id, not to be confused with the Pebble TID.
    static int transaction_id;
    static {
        transaction_id = new Random().nextInt();
    }

    synchronized static int nextTransactionID() {
        return ++transaction_id;
    }

    private void nack(Context ctx, int transaction_id) {
        PebbleKit.sendNackToPebble(ctx, transaction_id);
    }

    private void ack(Context ctx, int transaction_id) {
        PebbleKit.sendAckToPebble(ctx, transaction_id);
    }

    private final SparseArray<PacketInfo> inflight = new SparseArray<>();

    private PacketInfo getPebbleFinished(int tid) {
        synchronized (inflight) {
            PacketInfo pi = inflight.get(tid);
            return pi;
        }
    }

    public void sendMessage(Context ctx,  PebbleFinishedCallback w, PebbleDictionary data) {
        int tid = nextTransactionID();

        data.addUint32(KEY_TRANSACTION_ID, tid);
        synchronized (inflight) {
            inflight.put(tid, new PacketInfo(w, data));
        }

        PebbleKit.sendDataToPebbleWithTransactionId(ctx, PEBBLE_APP_UUID,
                data, tid);
    }

    private void resendMessage(Context ctx, int tid) {
        synchronized (inflight) {
            PebbleKit.sendDataToPebbleWithTransactionId(ctx, PEBBLE_APP_UUID,
                    inflight.get(tid).data, tid);
        }
    }

    public void registerHandlers(Context ctx) {
        PebbleKit.registerPebbleConnectedReceiver(ctx, mPebbleReceiver);
        PebbleKit.registerPebbleDisconnectedReceiver(ctx, mPebbleReceiver);
        PebbleKit.registerReceivedAckHandler(ctx, mPebbleAckReceiver);
        PebbleKit.registerReceivedDataHandler(ctx, mPebbleDataReceiver);
        PebbleKit.registerDataLogReceiver(ctx, mPebbleLogReceiver);
        PebbleKit.registerReceivedNackHandler(ctx, mPebbleNackReceiver);

        connected = PebbleKit.isWatchConnected(ctx);
    }

    public void start(Context ctx) {
        PebbleKit.startAppOnPebble(ctx, PEBBLE_APP_UUID);
    }

    public void stop(Context ctx) {
        PebbleKit.closeAppOnPebble(ctx, PEBBLE_APP_UUID);
    }

    public boolean isBusy() {
        synchronized (inflight) {
            return (inflight.size() > 0);
        }
    }

}
