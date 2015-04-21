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


    private final Stack<Integer> stateStack = new Stack<>();

    private int wh = -1;
    private Pebble parent;
    private List<PebbleLayer> layers = new ArrayList<>();

    // get's a window handle.
    private void connect(Context ctx) {
        if (wh >= 0) {
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
                            handleError(res.getUnsignedIntegerAsLong(Pebble.KEY_ERROR_CODE));
                        } else {
                            wh = res.getUnsignedIntegerAsLong(Pebble.KEY_WINDOW_ID).intValue();
                            updateStatus(ctx);
                        }
                    }
                }, data);

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
        }
        return root;
    }

    private void handleError(long error) {
        // can't do anything yet.
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
        parent = p;
    }
}
