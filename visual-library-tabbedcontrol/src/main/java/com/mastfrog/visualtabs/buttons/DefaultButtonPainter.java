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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import org.netbeans.api.visual.model.ObjectState;

/**
 *
 * @author Tim Boudreau
 */
final class DefaultButtonPainter implements ButtonPainter {

    final ButtonColors colors;

    public DefaultButtonPainter() {
        this(new DefaultButtonColors());
    }

    public DefaultButtonPainter(ButtonColors colors) {
        this.colors = colors;
    }

    @Override
    public void paint(Graphics2D g, Rectangle into, Shape shape, ButtonAction type, ObjectState state, boolean enabled) {
        g.setStroke(new BasicStroke(1.125f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Paint bg = colors.background(state);
        if (bg != null) {
            g.setPaint(bg);
            g.fillRect(into.x, into.y, into.width, into.height);
        }
        Paint foreground = colors.foreground(state);
        if (foreground != null) {
            g.setPaint(foreground);
            g.fill(shape);
        }
        Paint outline = colors.outline(state);
        if (outline != null) {
            g.setPaint(outline);
            g.draw(shape);
        }
    }

    interface ButtonColors {

        Paint background(ObjectState state);

        Paint foreground(ObjectState state);

        Paint outline(ObjectState state);
    }

    static final class DefaultButtonColors implements ButtonColors {

        @Override
        public Paint background(ObjectState state) {
            return null;
        }

        @Override
        public Paint foreground(ObjectState state) {
            if (state.isSelected()) {
                return Color.ORANGE;
            }
            if (state.isWidgetHovered()) {
                return Color.BLUE;
            }
            return Color.LIGHT_GRAY;
        }

        @Override
        public Paint outline(ObjectState state) {
            if (state.isSelected()) {
                return Color.BLUE;
            }
            if (state.isWidgetHovered()) {
                return Color.RED;
            }
            return Color.DARK_GRAY;
        }
    }

}
