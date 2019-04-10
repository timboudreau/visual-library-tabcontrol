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

import java.awt.Point;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.MoveStrategy;
import org.netbeans.api.visual.widget.Widget;

/**
 * Wrappers a MoveProvider/MoveStrategy to avoid invoking it unless the
 * drag is greater than a minimum distance and a timeout has elapsed,
 * to avoid interpreting clicks with minimal motions as moves.
 *
 * @author Tim Boudreau
 */
class DelayedMoveProvider<T extends MoveProvider & MoveStrategy> implements MoveProvider, MoveStrategy {

    private final T delegate;
    private final long delayMillis;
    private long startTimestamp = 0;
    private Widget targetWidget;
    private boolean delegating;
    private Point origLoc;
    private Point lastNewLocation;
    private final Consumer<Widget> alternateAction;
    private final BiPredicate<Point, Point> trigger;

    /**
     * Create a new delayed move provider that delegates to the original
     * if its conditions are met.
     *
     * @param delegate The real MoveProvider/MoveStrategy
     * @param delayMillis The minimum elapsed millisconds between calls to movementStarted()
     * and locationSuggested() before which the delegate should not be invoked
     * @param trigger A test to determine if the suggested and original points are
     * sufficiently distant from each other to go into delegating mode
     * @param alternateAction A consumer to call with the target widget in the
     * event that movementFinished() is called without ever having communicated
     * with the delegate - usually a click handler
     */
    public DelayedMoveProvider(T delegate, long delayMillis,
            BiPredicate<Point, Point> trigger, Consumer<Widget> alternateAction) {
        this.delegate = delegate;
        this.delayMillis = delayMillis;
        this.trigger = trigger;
        this.alternateAction = alternateAction;
    }

    void done() {
        startTimestamp = 0;
        targetWidget = null;
        delegating = false;
        origLoc = null;
        lastNewLocation = null;
    }

    @Override
    public void movementStarted(Widget widget) {
        origLoc = widget.getLocation();
        startTimestamp = System.currentTimeMillis();
        targetWidget = widget;
    }

    @Override
    public void movementFinished(Widget widget) {
        try {
            if (delegating) {
                delegate.movementFinished(widget);
            } else {
                alternateAction.accept(widget);
            }
        } finally {
            done();
        }
    }

    @Override
    public Point getOriginalLocation(Widget widget) {
        if (delegating) {
            return delegate.getOriginalLocation(widget);
        }
        return origLoc;
    }

    @Override
    public void setNewLocation(Widget widget, Point location) {
        if (delegating) {
            delegate.setNewLocation(widget, location);
        } else {
            lastNewLocation = location;
        }
    }

    @Override
    public Point locationSuggested(Widget widget, Point originalLocation, Point suggestedLocation) {
        if (delegating) {
            return delegate.locationSuggested(widget, originalLocation, suggestedLocation);
        }
        long elapsed = System.currentTimeMillis() - startTimestamp;
        if (elapsed > delayMillis && trigger.test(originalLocation, suggestedLocation)) {
            delegating = true;
            delegate.movementStarted(targetWidget);
            if (lastNewLocation != null) {
                delegate.setNewLocation(widget, lastNewLocation);
            }
            return delegate.locationSuggested(widget, originalLocation, suggestedLocation);
        }
        return originalLocation;
    }

}
