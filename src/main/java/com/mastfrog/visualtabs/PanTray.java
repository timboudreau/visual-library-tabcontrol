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

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.animator.Animator;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.animator.SceneAnimator;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class PanTray extends Widget {

    private int panPosition = 0;
    private Point lastPoint;
    private long eventTime;
    private final int rightInset;
    private final HoverNotifier hoverConsumer;
    private final int leftInset;
    private final PartiallyVisibleWidgetConsumer pvConsumer;

    PanTray(Scene scene, int inset, HoverNotifier hoverConsumer, PartiallyVisibleWidgetConsumer pvConsumer) {
        this(scene, inset, inset, hoverConsumer, pvConsumer);
    }

    PanTray(Scene scene, int leftInset, int rightInset, HoverNotifier hoverConsumer, PartiallyVisibleWidgetConsumer pvConsumer) {
        super(scene);
        getActions().addAction(new WidgetAction.Adapter() {
            @Override
            public WidgetAction.State mouseWheelMoved(Widget widget, WidgetAction.WidgetMouseWheelEvent event) {
                eventTime = event.getWhen();
                lastPoint = convertLocalToScene(event.getPoint());
                PanTray.this.mouseWheelMoved(event);
                return WidgetAction.State.CONSUMED;
            }
        });
        this.rightInset = rightInset;
        this.hoverConsumer = hoverConsumer;
        this.leftInset = leftInset;
        this.pvConsumer = pvConsumer;
    }

    @Override
    protected Rectangle calculateClientArea() {
        Rectangle result = new Rectangle(0, 0, 0, 0);
        for (Widget w : getChildren()) {
            Rectangle r = w.getBounds();
            result.width += r.width;
            result.height = Math.max(result.height, r.height);
        }
        result.x -= panPosition;
        return result;
    }

    long lastScrollEvent = 0;
    int rapidCount;

    private static final long RAPID = 25;
    private static final int TRIGGER_RAPID_COUNT = 3;
    private static final int RAPID_MULTIPLIER = 17;

    private void mouseWheelMoved(WidgetAction.WidgetMouseWheelEvent event) {
        long now = System.currentTimeMillis();
        // Wheel numbers are low, so to not have deathly slow scrolling
        // we need a multiplier
        int multiplier = 5;
        // We will accelerate if we see more than some number of events
        // in a short timeframe
        long interval = now - lastScrollEvent;
        boolean lastWasRapid = rapidCount > 0;
        // store for next time
        lastScrollEvent = now;
        if (interval <= RAPID) { // Got a fast one
            rapidCount++;
            // Several fast ones in a row? Multiply the multiplier
            if (rapidCount > TRIGGER_RAPID_COUNT) {
                multiplier *= RAPID_MULTIPLIER;
            }
        } else {
            // Longer delay, reset the counter
            rapidCount = 0;
        }
        // If we got a more delayed event after rapid ones (i.e. scramming
        // the mouse wheel twice in a row), multiply even more
        if (lastWasRapid && rapidCount == 0
                && getScene().getSceneAnimator().isAnimatingPreferredLocation(getChildren().get(0))) {
            multiplier *= RAPID_MULTIPLIER * 2;
        } else {
            // If we're in the middle of a scroll animation, don't keep
            // firing more
            if (rapidCount > TRIGGER_RAPID_COUNT + 1) {
                return;
            }
        }

        int distance = -event.getUnitsToScroll() * multiplier;
        int newPos = panPosition + (distance);
        // get the limits on how far left or right we can move
        int[] minMax = minMax();
        boolean usable = newPos >= minMax[0] && newPos <= minMax[1];
        if (!usable) {
            if (newPos > minMax[1]) {
                newPos = minMax[1];
            } else if (newPos < minMax[0]) {
                newPos = minMax[0];
            }
            usable = true;
        }
        if (usable) {
            setPanPosition(newPos);
        }
    }

    int width() {
        int result = 0;
        for (Widget w : getChildren()) {
            result += w.getPreferredBounds().width;
        }
        return result;
    }

    int[] minMax() {
        int needed = width();
        JComponent comp = getScene().getView();
        Rectangle r = getBounds();
        if (r == null) {
            getScene().validate();
            r = getBounds();
            if (r == null) {
                return new int[]{0, 0};
            }
        }
        if (comp != null) {
            int w = r.width - leftInset;
            if (w < needed) {
                int min = w - needed;
                int[] result = new int[]{min, 0};
                return result;
            }
        }
        return new int[]{0, 0};
    }

    boolean scrollFor(Widget dragged) {
        int dir = offEdgeDirection(dragged);
        if (dir != 0) {
            makeNextChildVisible(dir);
        }
        return dir != 0;
    }

    DragScrollTimer dragTimer(Widget widget) {
        return new DragScrollTimer(this, widget);
    }

    static class DragScrollTimer implements ActionListener, Widget.Dependency {

        private final PanTray tray;
        private final Widget widget;
        private static final int INITIAL_DELAY = 500;
        private static final int SHORTER_DELAY = 250;
        private final Timer timer = new Timer(INITIAL_DELAY, this);
        private boolean everStarted;
        private int ticks;

        DragScrollTimer(PanTray tray, Widget widget) {
            this.tray = tray;
            this.widget = widget;
            timer.setCoalesce(true);
            widget.addDependency(new WeakDependency(widget, this));
        }

        public void stop() {
            timer.stop();
        }

        public void start() {
            if (!timer.isRunning()) {
                ticks = 0;
                timer.setDelay(INITIAL_DELAY);
                timer.start();
            }
            everStarted = true;
        }

        void speedUp() {
            ticks = 0;
            timer.setDelay(SHORTER_DELAY);
        }

        private static final int SPEEDUP_THRESHOLD = 5;

        void tick() {
            ticks++;
            if (timer.getDelay() == INITIAL_DELAY && ticks >= SPEEDUP_THRESHOLD) {
                speedUp();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean result = tray.scrollFor(widget);
            if (!result) {
                stop();
            }
            tick();
        }
        int ct = 0;

        @Override
        public void revalidateDependency() {
            if (everStarted && !timer.isRunning()) {
                int dir = tray.offEdgeDirection(widget);
                if (dir != 0) {
                    start();
                }
            }
        }

    }

    void scrollLeft() {
        makeNextChildVisible(-1);
    }

    void scrollRight() {
        makeNextChildVisible(1);
    }

    void makeNextChildVisible(int direction) {
        assert direction == 1 || direction == -1;
        int val = panPosition + (75 * -direction); // XXX base on font size or actual child widths
        int[] minMax = minMax();
        if (val < minMax[0]) {
            val = minMax[0];
        }
        if (val > minMax[1]) {
            val = minMax[1];
        }
        if (val >= minMax[0] && val <= minMax[1]) {
            setPanPosition(val);
        }
    }

    public int directionOf(Widget w) {
        if (w.getParentWidget() == null) {
            return 0;
        }
        Rectangle bds = w.getBounds();
        if (bds == null) {
            w.getScene().validate();
            bds = w.getBounds();
            if (bds == null) {
                return 0;
            }
        }
        Point loc = w.getLocation();
        if (loc != null) {
            bds.x += loc.x;
            bds.y += loc.y;
        }
        bds = w.getParentWidget().convertLocalToScene(bds);
        bds = getChildren().get(0).convertSceneToLocal(bds);
        Rectangle r = getVisibleRectangle();
        r = convertLocalToScene(r);
        r = getChildren().get(0).convertSceneToLocal(r);

        if (bds.x + bds.width >= r.x && bds.x <= r.x + r.width) {
            return 0;
        }
        boolean isScrollRight = bds.x + bds.width > r.x;
        if (isScrollRight) {
            return 1;
        } else {
            return -1;
        }
    }

    void ensureChildVisible(Widget w) {
        if (w.getParentWidget() == null) {
            return;
        }
        Rectangle bds = w.getBounds();
        if (bds == null) {
            w.getScene().validate();
            bds = w.getBounds();
            if (bds == null) {
                return;
            }
        }
        Point loc = w.getLocation();
        if (loc != null) {
            bds.x += loc.x;
            bds.y += loc.y;
        }
        bds = w.getParentWidget().convertLocalToScene(bds);
        bds = getChildren().get(0).convertSceneToLocal(bds);
        Rectangle r = getVisibleRectangle();
        r = convertLocalToScene(r);
        r = getChildren().get(0).convertSceneToLocal(r);

        if (bds.x >= r.x && bds.x + bds.width <= r.x + r.width) {
            return;
        }
        boolean isScrollRight = bds.x > r.x;
        // Include the left/right insets so we never have a partially
        // faded tab if we intentionally scrolled it into view,
        // and when it is not the left/right-most tab, it is visually
        // indicated that there are additional tabs beyond the
        // one scrolled into view
        if (isScrollRight) {
            // Avoid scrolling everything but the last tab
            // off screen
            bds.x -= (r.width - bds.width) - rightInset;
        } else {
            bds.x -= leftInset;
        }
        setPanPosition(-bds.x);
    }

    public Rectangle getVisibleRectangle() {
        Rectangle result = getBounds();
        if (result == null) {
            getScene().validate();
            result = getBounds();
        }
        if (result.x < 0) {
            int negative = result.x;
            result.x = 0;
            result.width += negative;
        } else if (result.x > 0) {
            result.width -= result.x;
        }
        Rectangle sceneBounds = getScene().getBounds();
        result = convertLocalToScene(result);
        return result.intersection(sceneBounds);
    }

    public int panPosition() {
        return this.panPosition;
    }

    HoverUpdateNotifier hun;

    private void updatePartiallyVisibleWidget() {
        Graphics2D g = getGraphics();
        if (g == null) {
            getScene().validate();
            g = getGraphics();
            if (g == null) {
                return;
            }
        }
        FontMetrics fm = g.getFontMetrics(getFont());
        int charsWidth = fm.stringWidth("xxxxxxx");
        if (partiallyVisibleLeftWidget != null) {
            Rectangle bds = partiallyVisibleLeftWidget.getBounds();
            if (partiallyVisibleLeftWidget.getBounds() == null) {
                getScene().validate();
                bds = partiallyVisibleLeftWidget.getBounds();
            }
            if (bds != null) {
                int right = bds.x + bds.width;
                if (bds.x < 0 && right > 0) {
                    boolean veryPartial = right < charsWidth;
                    if (veryPartial != wasVeryPartial) {
                        setPartiallyVisibleLeftWidget(partiallyVisibleLeftWidget, veryPartial);
                    } else {
                        return;
                    }
                }
            }
        }

        boolean found = false;
        for (Widget w : getChildren().get(0).getChildren()) {
            Rectangle bds = w.getBounds();
            if (w.getBounds() == null) {
                getScene().validate();
                bds = w.getBounds();
                if (bds == null) {
                    continue;
                }
            }
            bds = convertSceneToLocal(w.convertLocalToScene(bds));
            int right = bds.x + bds.width;
            if (bds.x < 0 && right > 0) {
                setPartiallyVisibleLeftWidget(w, right < charsWidth);
                found = true;
                break;
            }
        }
        if (!found) {
            setPartiallyVisibleLeftWidget(null, false);
        }
    }

    private Widget partiallyVisibleLeftWidget;
    private boolean wasVeryPartial;

    private void setPartiallyVisibleLeftWidget(Widget widget, boolean veryPartial) {
        if (partiallyVisibleLeftWidget != widget || (partiallyVisibleLeftWidget == widget && veryPartial != wasVeryPartial)) {
            pvConsumer.setPartiallyVisibleWidget(partiallyVisibleLeftWidget, widget, veryPartial);
            wasVeryPartial = veryPartial;
            partiallyVisibleLeftWidget = widget;
        }
    }

    interface PartiallyVisibleWidgetConsumer {

        void setPartiallyVisibleWidget(Widget prev, Widget w, boolean veryPartial);
    }

    public boolean canScrollLeft() {
        int[] minMax = minMax();
        return panPosition < minMax[1];
    }

    public boolean canScrollRight() {
        int[] minMax = minMax();
        return panPosition > minMax[0];
    }

    void setPanPosition(int pos) {
        if (pos != this.panPosition) {
            this.panPosition = pos;
            SceneAnimator anim = getScene().getSceneAnimator();
            Animator locAnim = anim.getPreferredLocationAnimator();
            if (hun != null) {
                locAnim.removeAnimatorListener(hun);
            }
            hun = new HoverUpdateNotifier();
            locAnim.addAnimatorListener(hun);
            anim.animatePreferredLocation(getChildren().get(0), new Point(pos, 0));
            updatePartiallyVisibleWidget();
            this.revalidate(false);
        }
    }

    public void ensureSomethingIsVisible() {
        int[] minMax = minMax();
        if (panPosition < minMax[0]) {
            setPanPosition(minMax[0]);
        }
    }

    class HoverUpdateNotifier implements AnimatorListener {

        boolean triggered;

        @Override
        public void animatorStarted(AnimatorEvent event) {
        }

        @Override
        public void animatorReset(AnimatorEvent event) {
//            event.getAnimator().removeAnimatorListener(this);
        }

        @Override
        public void animatorFinished(AnimatorEvent event) {
            event.getAnimator().removeAnimatorListener(this);
            if (!triggered && lastPoint != null) {
                triggered = true;
                Widget container = getChildren().get(0);
                for (Widget w : container.getChildren()) {
                    Point pp = w.convertSceneToLocal(lastPoint);
                    if (w.isHitAt(pp)) {
                        hoverConsumer.hoverMayBeChanged(eventTime, w, lastPoint);
                        break;
                    }
                }
                updatePartiallyVisibleWidget();
            }
        }

        @Override
        public void animatorPreTick(AnimatorEvent event) {
        }

        @Override
        public void animatorPostTick(AnimatorEvent event) {

        }
    }

    public int offEdgeDirection(Widget dragged) {
        Rectangle bds = dragged.getBounds();
        if (bds == null) {
            return 0;
        }
        Rectangle r = dragged.convertLocalToScene(bds);
        r = convertSceneToLocal(r);
        int res = offEdgeDirection(r);
        return res;
    }

    public int offEdgeDirection(Rectangle r) {
        if (r.x < 0) {
            return -1;
        } else {
            Rectangle bds = getVisibleRectangle();
            if (r.x + r.width > bds.width) {
                return 1;
            }
        }
        return 0;
    }
}
