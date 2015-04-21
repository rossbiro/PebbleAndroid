package org.biro.pebble;

import android.content.Context;

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
 * Created by rossb on 4/18/15.
 */
public interface  PebbleLayer {
    abstract boolean changed();
    boolean update(Context ctx, PebbleWindow pw);
}
