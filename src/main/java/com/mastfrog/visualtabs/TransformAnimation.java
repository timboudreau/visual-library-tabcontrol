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

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import javax.swing.Timer;

/**
 *
 * @author Tim Boudreau
 */
final class TransformAnimation implements ActionListener {

    private int tick;
    private int direction = -1;
    private final int limit;
    private final Timer timer;
    private final boolean oneShot;
    private final Runnable repainter;

    public TransformAnimation(int limit, int fps, boolean oneShot, Runnable repainter) {
        this.limit = limit;
        tick = limit;
        timer = new Timer(1000 / fps, this);
        timer.setRepeats(true);
        this.oneShot = oneShot;
        this.repainter = repainter;
    }

    private boolean resetPending;
    private boolean started;

    public void stop() {
        resetPending = true;
        started = false;
    }

    public void start() {
        if (!started) {
            started = true;
            resetPending = oneShot;
            reallyReset();
            timer.start();
        }
    }

    void reallyReset() {
        tick = limit - 1;
        direction = -1;
    }

    private int tick() {
        tick += direction;
        if (tick > limit) {
            tick -= 2;
            direction *= -1;
        } else if (tick < 0) {
            tick = 1;
            direction *= -1;
        }
        if (resetPending && tick == limit) {
            timer.stop();
        }
        return tick;
    }

    private static final int SCALE_TICK_DIVISOR = 4;
    private static final double MAX_SCALE_DELTA = 0.05;
    private static final double MAX_THETA_DELTA = 0.06;

    private AffineTransform transform(double w, double h) {

        int initialScaleTick = limit;
        int finalScaleTick = limit - (limit / SCALE_TICK_DIVISOR);
        int currentScaleTick;
        int currentRotateTick;
        if (tick > finalScaleTick && tick <= initialScaleTick) {
            // Scale the scale tick into the range of timer ticks that handle
            // scaling
            currentScaleTick = tick - finalScaleTick;
            // Use a fixed 1.0 value for the final scale tick
            currentRotateTick = finalScaleTick;
        } else {
            // Not scaling any further, but keep the scale we arrived at
            // when we were counting down through scale ticks before starting
            // rotation
            currentScaleTick = 1;
            // Scale the rotate tick value to within the range of ticks that are
            // rotate ticks.
//            currentRotateTick = (initialScaleTick - finalScaleTick) - (finalScaleTick - tick);
            currentRotateTick = finalScaleTick - (finalScaleTick - tick);
        }
        // Get the factor we will scale by
        double scaleTickFactor = (double) currentScaleTick / (double) (initialScaleTick - finalScaleTick);
        // Convert it so it is only a change in the range defined by max scale delta,
        // so we scale down only a little bit
        double scaleTickValue = (1.0 - MAX_SCALE_DELTA) + (MAX_SCALE_DELTA * scaleTickFactor);
        double ww = w / 2;
        double hh = h / 2;
        double offy = hh - (hh * scaleTickValue);
        double offx = ww - (ww * scaleTickValue);
        // Translate so that the scaled graphics will remain centered on the x/y midpoint
        AffineTransform result = AffineTransform.getTranslateInstance(offx, offy);
        if (currentRotateTick != finalScaleTick) {
            int invertedTick = finalScaleTick - currentRotateTick;
            // Get the factor we will rotate by
            double rotateFactor = (double) currentRotateTick / (double) finalScaleTick;
            // Convert that to theta within the range we want to rotate by - the
            // scaling allows us to (usually) rotate without the rotated graphic
            // going outside the clip region
            double rotateTheta = ((MAX_THETA_DELTA * rotateFactor)
                    + (MAX_THETA_DELTA / 2d)) - (MAX_THETA_DELTA);
//            System.out.println(tick + ". ScaleTick " + currentScaleTick + " ROTATE TICK " + currentRotateTick + " theta " + rotateTheta + " factor " + rotateFactor);

            result.concatenate(AffineTransform.getRotateInstance(rotateTheta, ww, hh));
        } else {
//            System.out.println(tick + ". ScaleTick " + currentScaleTick + " ROTATE TICK " + currentRotateTick + " scaleValue " + scaleTickValue + " factor " + scaleTickFactor);
        }
        result.concatenate(AffineTransform.getScaleInstance(scaleTickValue, scaleTickValue));
        return result;
    }

    public void withTransformAnimation(Graphics2D g, int w, int h, Runnable r) {
        if (!timer.isRunning()) {
            r.run();
            return;
        }
        AffineTransform old = g.getTransform();
        try {
//            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.transform(transform(w, h));
            r.run();
        } finally {
            g.setTransform(old);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tick();
        repainter.run();
    }
}
