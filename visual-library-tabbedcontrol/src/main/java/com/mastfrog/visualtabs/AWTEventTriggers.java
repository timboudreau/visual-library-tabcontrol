/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.mastfrog.visualtabs;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * One-shot awt event detection - we need this to detect mouse-release
 * when not over the scene component or we cannot remove popups if the
 * mouse is dragged off-scene.
 *
 * @author Tim Boudreau
 */
public final class AWTEventTriggers {
    private static final AWTEventTriggers INSTANCE = new AWTEventTriggers();

    public static void detectMouseReleased(Runnable run) {
        INSTANCE.subscribe(MouseEvent.MOUSE_RELEASED, run);
    }

    private final Map<Integer, Runnable> listeners = new HashMap<>();
    private int lastMask;
    void subscribe(int eventType, Runnable run) {
        boolean wasEmpty = listeners.isEmpty();
        Runnable r = wasEmpty ? null : listeners.remove(eventType);
        if (r != null) {
            Runnable nue = () -> {
                r.run();
                run.run();
            };
            listeners.put(eventType, nue);
        } else {
            listeners.put(eventType, run);
        }
        int mask = eventType;
        if (listeners.size() > 1) {
            for (Integer i : listeners.keySet()) {
                mask |= i;
            }
        }
        if (wasEmpty) {
            Toolkit.getDefaultToolkit().addAWTEventListener(l, mask);
        } else if (mask != lastMask) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(l);
            Toolkit.getDefaultToolkit().addAWTEventListener(l, mask);
        }

        lastMask = mask;
    }

    void dispatch(AWTEvent event) {
        Runnable r = listeners.remove(event.getID());
        if (r != null) {
            r.run();
        }
        if (listeners.isEmpty()) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(l);
        }
    }

    private final L l = new L();
    class L implements AWTEventListener {

        @Override
        public void eventDispatched(AWTEvent event) {
            dispatch(event);
        }

    }
}
