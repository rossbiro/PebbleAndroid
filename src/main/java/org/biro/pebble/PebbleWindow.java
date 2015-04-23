package org.biro.pebble;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
public class PebbleWindow {

    private static final String TAG = "PebbleWindow: ";

    private final int STATE_NONE = 0;
    private final int STATE_UPDATING = 1;
    private final int STATE_PUSH = 2;
    private final int STATE_REQUEST_CLICKS = 3;

    private boolean needReset=false;
    private boolean needClear=false;
    private boolean wantClicks=false;

    private final Stack<Integer> stateStack = new Stack<>();

    private int wh = -1;
    private Pebble parent;
    private List<PebbleLayer> layers = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id = 0;

    // Not all button actions
    // can be used at once.
    // e.g. single click and
    // repeated click.
    private class ButtonInfo {
        boolean wantSingleClick = false;
        short repeated_rate = 0; // in ms.
        short long_click_time = 0; //in ms
        byte multi_click_count = 0;
    }

    private ButtonInfo[] clicks;

    {
        clicks = new ButtonInfo[Pebble.BUTTON_NUM_BUTTONS];
        for (int i = 0; i < Pebble.BUTTON_NUM_BUTTONS; ++i) {
            clicks[i] = new ButtonInfo();
        }
    }

    // get's a window handle.
    private void connect(Context ctx) {
        if (wh >= 0) {
            return;
        }

        if (id != 0) {
            PebbleDictionary data = new PebbleDictionary();
            data.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_GET_DICTIONARY_BY_ID);
            parent.sendMessage(ctx, new Pebble.PebbleFinishedCallback() {
                @Override
                public void processIncoming(Context ctx, int tid, PebbleDictionary resp, PebbleDictionary req) {
                    int status = resp.getUnsignedIntegerAsLong(Pebble.KEY_STATUS).intValue();
                    if (status == Pebble.STATUS_ERR) {
                        Log.e(TAG, "Call Failed" + resp.getUnsignedIntegerAsLong(Pebble.KEY_ERROR_CODE));
                        handleError(ctx, tid, resp, req);
                    } else {
                        wh = resp.getUnsignedIntegerAsLong(Pebble.KEY_RETURN_VALUE).intValue();
                        updateStatus(ctx);
                    }
                }
            }, data);
            return;
        }

        PebbleDictionary data = new PebbleDictionary();
        data.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_NEW_WINDOW);
        parent.sendMessage(ctx, new Pebble.PebbleFinishedCallback() {
                    @Override
                    public void processIncoming(Context ctx, int tid,
                                                PebbleDictionary res, PebbleDictionary req) {
                        int status = res.getUnsignedIntegerAsLong(Pebble.KEY_STATUS).intValue();
                        if (status == Pebble.STATUS_ERR) {
                            Log.e(TAG, "Call Failed" + res.getUnsignedIntegerAsLong(Pebble.KEY_ERROR_CODE));
                            handleError(ctx, tid, res, req);
                        } else {
                            wh = res.getUnsignedIntegerAsLong(Pebble.KEY_RETURN_VALUE).intValue();
                            updateStatus(ctx);
                        }
                    }
                }, data);

    }

    private void clearState() {
        synchronized (stateStack) {
            stateStack.clear();
        }
    }

    private int popState() {
        synchronized (stateStack) {
            if (stateStack.isEmpty()) {
                return STATE_NONE;
            }
            return stateStack.pop();
        }
    }

    private void pushState(int state) {
        synchronized (stateStack) {
            stateStack.push(state);
        }
    }

    public void addState(int state) {
        synchronized (stateStack) {
            stateStack.add(state);
        }
    }

    private static PebbleWindow root = null;

    synchronized public static PebbleWindow getRootWindow() {
        if (root == null) {
            root = new PebbleWindow();
            root.wh = Pebble.ROOT_WINDOW_HANDLE;
            root.id = Pebble.ROOT_WINDOW_ID;
        }
        return root;
    }

    void handleError(Context ctx, int tid, PebbleDictionary req, PebbleDictionary resp) {
        if (resp.getUnsignedIntegerAsLong(Pebble.KEY_ERROR_CODE) == Pebble.ENOWINDOW) {
            needReset = true;
            resetWindows(ctx);
        } else {
            needClear = true;
            clearWindow(ctx);
        }


    }

    // continues processing status after
    // something interrupted it.
    public void updateStatus(Context ctx) {
        int cs = popState();
        switch (cs) {
            case STATE_NONE:
                //nothing to do.
                break;

            case STATE_UPDATING:
                update(ctx);
                break;

            case STATE_PUSH:
                push(ctx);
                break;

            case STATE_REQUEST_CLICKS:
                requestClicks(ctx);
                break;

            default:
                Log.e(TAG, "Unknown state");
                break;
        }

    }

    public void update(Context ctx) {
        if (parent.isBusy()) {
            addState(STATE_UPDATING);
            return;
        }

        if (wh < 0) {
            connect(ctx);
            addState(STATE_UPDATING);
            return;
        }

        for (PebbleLayer pl: layers) {
            if (pl.changed()) {
                if (pl.update(ctx, this)) {
                    addState(STATE_UPDATING);
                    return; // did something, have to wait for a result.
                }
            }
        }

        updateStatus(ctx);

    }

    public void push(Context ctx) {
        PebbleDictionary pd = new PebbleDictionary();

        if (parent.isBusy()) {
            addState(STATE_PUSH);
            return;
        }

        pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_PUSH_WINDOW);
        if (wh < 0) {
            addState(STATE_PUSH);
            connect(ctx);
            return;
        }

        send(ctx, pd, null);
    }

    public void addLayer(PebbleLayer pl) {
        layers.add(pl);
    }

    public void send(Context ctx, PebbleDictionary pd, Pebble.PebbleFinishedCallback pfc) {
        if (wh < 0) {
            connect(ctx);
            return;
        }
        pd.addUint32(Pebble.KEY_WINDOW_ID, wh);
        parent.sendMessage(ctx, pfc, pd);
    }

    public void setParent(Pebble p) {
        if (parent != null) {
            parent.removeChild(this);
        }
        parent = p;
        parent.addChild(this);
    }

    public void resetWindows(Context ctx) {
        needReset = false;
        PebbleDictionary pd = new PebbleDictionary();
        pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_RESET_WINDOWS);
        clearState();
        addState(STATE_UPDATING);
        if (wantClicks) {
            addState(STATE_REQUEST_CLICKS);
        }
        parent.sendMessage(ctx, new Pebble.PebbleFinishedCallback() {
            @Override
            public void processIncoming(Context ctx, int tid, PebbleDictionary resp, PebbleDictionary req) {
                if (resp.getUnsignedIntegerAsLong(Pebble.KEY_STATUS) == Pebble.STATUS_ERR) {
                    handleError(ctx, tid, resp, req);
                } else {
                    needReset = false;
                    updateStatus(ctx);
                }
            }
        }, pd);
        wh = -1;
        for (PebbleLayer pl: layers) {
            pl.clearHandle();
        }
    }

    public void clearWindow(Context ctx) {
        PebbleDictionary pd = new PebbleDictionary();
        pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_CLEAR_WINDOW);
        send(ctx, pd, new Pebble.PebbleFinishedCallback() {
            @Override
            public void processIncoming(Context ctx, int tid, PebbleDictionary resp, PebbleDictionary req) {
                if (resp.getUnsignedIntegerAsLong(Pebble.KEY_STATUS) == Pebble.STATUS_ERR) {
                    handleError(ctx, tid, resp, req);
                } else {
                    needClear = false;
                    updateStatus(ctx);
                }
            }
        });
    }

    public void setClickRequests(int button) {
        clicks[button].wantSingleClick = true;
    }

    private int clicksToInt(ButtonInfo bi) {
        int cr = 0;
        if (bi.wantSingleClick) {
            cr |= Pebble.BUTTON_WANT_SINGLE_CLICK;
        }
        cr |= Pebble.buttonMultiMax(bi.multi_click_count);
        cr |= Pebble.buttonLongClickDelay(bi.long_click_time);
        cr |= Pebble.buttonRepeatSpeed(bi.repeated_rate);
        return cr;
    }

    public void requestClicks(Context ctx) {
        wantClicks = true;
        if (parent.isBusy()) {
            addState(STATE_REQUEST_CLICKS);
            return;
        }

        if (wh < 0 ) {
            connect(ctx);
            addState(STATE_REQUEST_CLICKS);
            return;
        }

        PebbleDictionary pd = new PebbleDictionary();
        pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_REQUEST_CLICKS);
        for (int i = 0; i < Pebble.BUTTON_NUM_BUTTONS; ++i) {
            int cr = clicksToInt(clicks[i]);
            if (cr == 0) {
                continue;
            }
            pd.addUint32(Pebble.KEY_BUTTON_0 + i, cr);
        }
        send(ctx, pd, new Pebble.PebbleFinishedCallback() {
            @Override
            public void processIncoming(Context ctx, int tid, PebbleDictionary resp, PebbleDictionary req) {
                // XXXXX FIXME: handle error codes
                updateStatus(ctx);
            }
        });

    }
}
