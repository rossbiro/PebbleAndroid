package org.biro.pebble;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;

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
 * Created by rossb on 4/19/15.
 */
public class PebbleTextLayer implements PebbleLayer {
    private static final String TAG = "PebbleLayer: ";
    private int tlh = -1;

    private int fg = Pebble.COLOR_BLACK;
    private boolean fg_changed = false;

    private int bg = Pebble.COLOR_WHITE;
    private boolean bg_changed = false;

    private String font = "Raster Gothic 14-point Boldface";
    private boolean font_changed = false;

    int alignment = Pebble.TEXT_ALIGNMENT_LEFT;
    boolean alignment_changed = false;

    String text="";
    boolean text_changed = false;

    private void updateChanged(PebbleDictionary pd) {
        if (pd.contains(Pebble.KEY_ATTRIBUTE_FG_COLOR)) {
            fg_changed = !(pd.getUnsignedIntegerAsLong(Pebble.KEY_ATTRIBUTE_FG_COLOR).intValue() == fg);
        }

        if (pd.contains(Pebble.KEY_ATTRIBUTE_BG_COLOR)) {
            bg_changed = !(pd.getUnsignedIntegerAsLong(Pebble.KEY_ATTRIBUTE_BG_COLOR).intValue() == bg);
        }

        if (pd.contains(Pebble.KEY_ATTRIBUTE_ALIGNMENT)) {
            alignment_changed = !(pd.getUnsignedIntegerAsLong(Pebble.KEY_ATTRIBUTE_ALIGNMENT).intValue() == alignment);
        }

        if (pd.contains(Pebble.KEY_ATTRIBUTE_FONT)) {
            font_changed = !pd.getString(Pebble.KEY_ATTRIBUTE_FONT).equals(font);
        }

        if (pd.contains(Pebble.KEY_ATTRIBUTE_TEXT)) {
            try {
                text_changed = !pd.getString(Pebble.KEY_ATTRIBUTE_TEXT).equals(text);
            } catch (Exception e) {
                Log.d(TAG, "Text comare exception: " + e.getMessage());
            }
            try {
                byte[] bytes = pd.getBytes(Pebble.KEY_ATTRIBUTE_TEXT);
                String s = new String(bytes, "UTF-8");
                text_changed = !s.equals(text);
            } catch (Exception e) {
                Log.d(TAG, "Text comare exception2: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean changed() {
        return fg_changed || bg_changed || font_changed || alignment_changed || text_changed;
    }

    // returns true when we started something and
    // have to wait.
    @Override
    public boolean update(Context ctx, final PebbleWindow pw) {
        PebbleDictionary pd;
        if (tlh < 0) {
            pd = new PebbleDictionary();
            pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_NEW_TEXT_LAYER);
            pw.send(ctx, pd, new Pebble.PebbleFinishedCallback() {
                @Override
                public void processIncoming(Context ctx, int tid, PebbleDictionary resp,
                                            PebbleDictionary req) {
                    tlh = resp.getUnsignedIntegerAsLong(Pebble.KEY_TEXT_LAYER_ID).intValue();
                    pw.updateStatus(ctx);
                }
            });
            return true;
        }

        if (!changed()) {
            return false;
        }

        pd = new PebbleDictionary();
        pd.addUint32(Pebble.KEY_TEXT_LAYER_ID, tlh);
        pd.addUint32(Pebble.KEY_METHOD_ID, Pebble.FUNC_APPLY_ATTRIBUTES);

        if (fg_changed) {
            pd.addUint32(Pebble.KEY_ATTRIBUTE_FG_COLOR, fg);
        }

        if (bg_changed) {
            pd.addUint32(Pebble.KEY_ATTRIBUTE_BG_COLOR, bg);
        }

        if (font_changed) {
            pd.addString(Pebble.KEY_ATTRIBUTE_FONT, font);
        }

        if (alignment_changed) {
            pd.addUint32(Pebble.KEY_ATTRIBUTE_ALIGNMENT, alignment);
        }

        if (text_changed) {
            try {
                byte[] b = text.getBytes("UTF-8");
                pd.addBytes(Pebble.KEY_ATTRIBUTE_TEXT, b);
            } catch (java.io.UnsupportedEncodingException e) {
                Log.d(TAG, "UnsupportedIOEncodingException: " + e.getMessage());
            }
        }

        pw.send(ctx, pd, new Pebble.PebbleFinishedCallback() {
            @Override
            public void processIncoming(Context ctx, int tid,
                                        PebbleDictionary resp, PebbleDictionary req) {
                updateChanged(req);
                pw.updateStatus(ctx);
            }
        });

        return true;
    }

    public void setText(String text) {
        if (this.text == text) {
            return;
        }

        this.text = text;
        this.text_changed = true;
    }
}
