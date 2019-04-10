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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 *
 * @author Tim Boudreau
 */
final class AnimationTimer implements ActionListener {

    private final int ticks;
    private final boolean reversable;
    private final Timer timer;
    private boolean started;
    private int currentTick;
    private int direction = 1;
    private final boolean oneShot;
    private final TimerConsumer onTick;

    public AnimationTimer(int ticks, boolean reversable, int fps, boolean oneShot, TimerConsumer onTick) {
        this.ticks = ticks;
        this.reversable = reversable;
        this.oneShot = oneShot;
        timer = new Timer(1000 / fps, this);
        timer.setRepeats(true);
        timer.setCoalesce(true);
        this.onTick = onTick;
    }

    public int ticks() {
        return ticks;
    }

    public void start() {
        if (!started) {
            direction = 1;
            currentTick = 0;
            started = true;
            timer.start();
        }
    }

    public void stop() {
        if (started) {
            timer.stop();
            started = false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int tick = tick();
        onTick.tick(tick, ticks);
    }

    private int tick() {
        currentTick += direction;
        if (reversable) {
            if (currentTick < 0) {
                currentTick = 0;
                direction *= -1;
                if (oneShot) {
                    stop();
                }
            } else if (currentTick > ticks) {
                currentTick = ticks;
                direction *= -1;
            }
        } else if (currentTick > ticks) {
            if (oneShot) {
                currentTick = ticks;
                stop();
            } else {
                currentTick = 0;
            }
        }
        return currentTick;
    }

    public interface TimerConsumer {

        void tick(int tick, int of);
    }

}
