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
package com.mastfrog.visualtabs.buttons;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class RightButtonWidget extends Widget {

    private final ButtonIcon icon;

    RightButtonWidget(Scene scene, ButtonIcon icon) {
        super(scene);
        setOpaque(true);
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "rb:" + actionType();
    }

    public ButtonAction actionType() {
        return icon.actionType();
    }

    @Override
    protected Rectangle calculateClientArea() {
        Graphics2D g = getGraphics();
        if (g == null) {
            return new Rectangle();
        }
        FontMetrics fm = g.getFontMetrics(RightButtonWidget.this.getFont());
        int w = fm.stringWidth("X") + 4;
        int h = fm.getHeight() + 4;
        int size = Math.max(w, h);
        return new Rectangle(0, 0, size, size);
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (icon.actionType() == ButtonAction.MAXIMIZE) {
            // otherwise maximize will only test if the point
            // is in a very thin rectangular edge
            return iconBounds().contains(localLocation);
        }
        return icon.actionType().shape(iconBounds()).contains(localLocation);
    }
    
    private Rectangle iconBounds() {
        Rectangle bds = getBounds();
        Rectangle r = calculateClientArea();
        if (bds.height > r.height) {
            r.y += (bds.height - r.height) / 2;
        }
        if (bds.width > r.width) {
            r.x += (bds.width - r.width) / 2;
        }
        r.width++;
        r.height++;
        return r;
    }

    @Override
    protected void paintWidget() {
        Rectangle bds = getBounds();
        if (bds == null || bds.width < 1 || bds.height < 1) {
            return;
        }
        Graphics2D g = getGraphics();
        Rectangle into = iconBounds();
        icon.paint(into, g, getState(), isEnabled());
    }

    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        repaint();
    }

}
