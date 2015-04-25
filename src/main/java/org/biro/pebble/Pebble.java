package org.biro.pebble;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
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
    public static final int FUNC_REQUEST_CLICKS = 5;
    public static final int FUNC_GET_DICTIONARY_BY_ID = 6;
    public static final int FUNC_GET_TEXT_LAYER_BY_ID = 7;
    public static final int FUNC_CLEAR_WINDOW = 8;
    public static final int FUNC_RESET_WINDOWS = 9;

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
    public static final int KEY_CLICK = 14;
    public static final int KEY_BUTTON_0 = 15;
    public static final int KEY_BUTTON_1 = 16;
    public static final int KEY_BUTTON_2 = 17;
    public static final int KEY_BUTTON_3 = 18;
    public static final int KEY_BUTTON_4 = 19;
    public static final int KEY_BUTTON_5 = 20;
    public static final int KEY_BUTTON_6 = 21;
    public static final int KEY_BUTTON_7 = 22; // reserve space for 8 buttons, although there are only 4 right now.
    public static final int KEY_ID = 23;

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERR = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_STOPPED = 3;

    public static final int ROOT_WINDOW_HANDLE = 0;
    public static final int ROOT_WINDOW_ID = 1;

    public static final int ENOMEM = 1;
    public static final int ENOWINDOW = 2;
    public static final int ENOLAYER = 3;
    public static final int EINVALID_OP = 4;
    public static final int EINVALID_TRANSACTION = 5;
    public static final int ENACK_RECEIVED = -1; // never sent.  Synthetic error.

    public static final int COLOR_BLACK = 0;
    public static final int COLOR_WHITE = 1;
    public static final int COLOR_CLEAR = -1;

    public static final int TEXT_ALIGNMENT_LEFT = 0;
    public static final int TEXT_ALIGNMENT_CENTER = 1;
    public static final int TEXT_ALIGNMENT_RIGHT = 2;

    public static final String ACTION_BUTTON_PRESS = "org.biro.pebble.Pebble.BUTTON_PRESS";
    public static final int BUTTON_BACK = 0;
    public static final int BUTTON_UP = 1;
    public static final int BUTTON_SELECT = 2;
    public static final int BUTTON_DOWN = 3;
    public static final int BUTTON_NUM_BUTTONS=4;

    public static final int BUTTON_WANT_SINGLE_CLICK = 1;
    public static final int BUTTON_WANT_REPEATED_MASK = 0xFFF << 8;
    public static final int BUTTON_WANT_MULTI_MASK = 0xF << 1;
    public static final int BUTTON_LONG_CLICK_MASK = 0xFFF << 20;
    public static final int LONG_CLICK_DOWN = 1 << 17;
    public static final int LONG_CLOCK_UP = 1 << 18;

    public static final String ACTION_RETRY = "org.biro.pebble.Pebble.ACTION_RETRY";

    private static final int RETRY_DELAY = 2000; // retry every 2 seconds

    public static final int buttonLongClickDelay(int ms) {
        return (ms & 0xfff) << 20;
    }

    public static final int buttonMultiMax(int count) {
        return (count & 0xf) << 1;
    }

    public static final int buttonRepeatSpeed(int ms) {
        return (ms & 0xfff) << 8;
    }

    //#define CLICK_DATA(r, c, b) (((r)?1:0) << 16 | (c << 8) | (b) )
    //#define LONG_CLICK_DATA(t, r,c,b) ((t) | ((r)?1:0) << 16 | (c << 8) | (b) )
    public static final int clickButton(int data) {
        return data & 0xff;
    }

    public static final int clickCount(int data) {
        return (data & 0xff) >> 8;
    }

    public static final boolean clickRepeating(int data) {
        return (data & 0x10000) == 1;
    }

    private Set<PebbleWindow> children = new HashSet<>();

    private Handler mUpdateHandler = new Handler();

    public interface PebbleFinishedCallback {
        public void processIncoming(Context ctx, int tid,
                                    PebbleDictionary resp, PebbleDictionary req);
    };

    private class PacketInfo {
        PebbleFinishedCallback w;
        PebbleDictionary data;
        long expires;

        PacketInfo(PebbleFinishedCallback w, PebbleDictionary data) {
            this.w = w;
            this.data = data;
            expires = System.currentTimeMillis() + 10000; // expire in 300ms.
            // XXXXX FIXME: That should not be hardcoded.
        }
    };

    private UUID mPebbleUUID;
    // Call this once immediately after create
    // to set the UUID and connect to the app.
    synchronized public void setPebbleAppUUID(String suuid) throws PebbleException {
        if (mPebbleUUID != null) {
            throw new PebbleException("Cannot change UUID");
        }
        mPebbleUUID = UUID.fromString(suuid);

        mPebbleDataReceiver = new PebbleKit.PebbleDataReceiver(mPebbleUUID) {
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
                            if (pebbleDictionary.contains(Pebble.KEY_CLICK)) {
                                int data = pebbleDictionary.getUnsignedIntegerAsLong(KEY_CLICK).intValue();
                                Intent i = new Intent(ACTION_BUTTON_PRESS);
                                i.putExtra("Button", Pebble.clickButton(data));
                                i.putExtra("Repeating", Pebble.clickRepeating(data));
                                i.putExtra("Count", Pebble.clickCount(data));
                                i.putExtra("TimeStamp", tid);
                                ctx.sendBroadcast(i);
                                tid = 0;
                                // tid = 0 is invalid, so we can fall through and
                                // we won't be processing anything.
                            }

                            PacketInfo info = getPebbleFinished(tid);
                            removeInflight(tid);
                            if (info != null && info.w != null) {
                                info.w.processIncoming(ctx, tid, pebbleDictionary, info.data);
                            }
                            return;

                        case STATUS_STARTED:
                            started = true;
                            ack(ctx, ptid);
                            resetWindows(ctx);
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

        mPebbleAckReceiver =
                new PebbleKit.PebbleAckReceiver(mPebbleUUID) {
                    @Override
                    public void receiveAck(Context context, int i) {
                        Log.d(TAG, "AckReceived");
                    }
                };

        mPebbleNackReceiver =
                new PebbleKit.PebbleNackReceiver(mPebbleUUID) {
                    @Override
                    public void receiveNack(Context context, int i) {
                        Log.d(TAG, "Nack Received.");
                        nackInflight(context);
                    }

                };


    }

    private boolean started = false;
    private boolean connected = false;

    private PebbleReceiver mPebbleReceiver = new PebbleReceiver(this);

    private PebbleKit.PebbleDataReceiver mPebbleDataReceiver;

    private void removeInflight(int tid) {
        synchronized (inflight) {
            inflight.remove(tid);
        }
    }

    private PebbleKit.PebbleAckReceiver mPebbleAckReceiver;

    private PebbleKit.PebbleNackReceiver mPebbleNackReceiver;

    private PebbleKit.PebbleDataLogReceiver mPebbleLogReceiver;

    // My transaction id, not to be confused with the Pebble TID.
    static int transaction_id;
    static {
        transaction_id = new Random().nextInt();
    }

    synchronized static int nextTransactionID() {
        int tid;
        do {
            tid = ++transaction_id;
        } while (tid == 0);
        return tid;
    }

    private void nackInflight(final Context ctx) {
        synchronized (inflight) {
            PebbleDictionary pd = new PebbleDictionary();
            pd.addUint32(KEY_STATUS, STATUS_ERR);
            pd.addUint32(KEY_ERROR_CODE, ENACK_RECEIVED);
            for (int i = 0; i < inflight.size(); ++i) {
                PacketInfo pi = inflight.valueAt(i);
                int tid = pi.data.getUnsignedIntegerAsLong(KEY_TRANSACTION_ID).intValue();
                if (pi.w != null) {
                    pi.w.processIncoming(ctx, tid, pd, pi.data);
                }
            }
            inflight.clear();
        }
        mUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent();
                i.setAction(ACTION_RETRY);
                ctx.sendBroadcast(i);
            }
        }, RETRY_DELAY);

    }

    private void nack(Context ctx, int transaction_id) {
        PebbleKit.sendNackToPebble(ctx, transaction_id);
    }

    private void ack(Context ctx, int transaction_id) {
        PebbleKit.sendAckToPebble(ctx, transaction_id);
    }

    private void resetWindows(Context ctx) {
        for (PebbleWindow pw: children) {
            pw.resetWindows(ctx);
        }
    }

    private final SparseArray<PacketInfo> inflight = new SparseArray<>();

    private PacketInfo getPebbleFinished(int tid) {
        synchronized (inflight) {
            PacketInfo pi = inflight.get(tid);
            return pi;
        }
    }

    void addChild(PebbleWindow pw) {
        children.add(pw);
    }

    void removeChild(PebbleWindow pw) {
        children.remove(pw);
    }

    public void sendMessage(Context ctx,  PebbleFinishedCallback w, PebbleDictionary data) {
        int tid = nextTransactionID();

        data.addUint32(KEY_TRANSACTION_ID, tid);
        synchronized (inflight) {
            inflight.put(tid, new PacketInfo(w, data));
        }

        PebbleKit.sendDataToPebbleWithTransactionId(ctx, mPebbleUUID,
                data, tid);
    }

    private void resendMessage(Context ctx, int tid) {
        synchronized (inflight) {
            PebbleKit.sendDataToPebbleWithTransactionId(ctx, mPebbleUUID,
                    inflight.get(tid).data, tid);
        }
    }

    public void pebbleConnected(Context ctx) {
        if (connected == false) {
            connected = true;
            for (PebbleWindow pw: children) {
                pw.update(ctx);

            }
        }
    }

    public void pebbleDisconnected(Context ctx) {
        connected = false;
    }

    public void registerHandlers(Context ctx) {
        PebbleKit.registerReceivedAckHandler(ctx, mPebbleAckReceiver);
        PebbleKit.registerReceivedDataHandler(ctx, mPebbleDataReceiver);
        PebbleKit.registerDataLogReceiver(ctx, mPebbleLogReceiver);
        PebbleKit.registerReceivedNackHandler(ctx, mPebbleNackReceiver);

        connected = PebbleKit.isWatchConnected(ctx);
    }

    public void registerReceivers(Context ctx) {
        PebbleKit.registerPebbleConnectedReceiver(ctx, mPebbleReceiver);
        PebbleKit.registerPebbleDisconnectedReceiver(ctx, mPebbleReceiver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RETRY);
        ctx.registerReceiver(mPebbleReceiver, filter);
    }

    public void unregisterReceivers(Context ctx) {
        ctx.unregisterReceiver(mPebbleReceiver);
    }

    public void start(Context ctx) {
        PebbleKit.startAppOnPebble(ctx, mPebbleUUID);
    }

    public void stop(Context ctx) {
        PebbleKit.closeAppOnPebble(ctx, mPebbleUUID);
    }

    public boolean isBusy() {
        synchronized (inflight) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < inflight.size(); ++i ) {
                if (now  - inflight.valueAt(i).expires >= 0) {
                    inflight.removeAt(i--);
                }
            }
            return (inflight.size() > 0);
        }
    }

}
