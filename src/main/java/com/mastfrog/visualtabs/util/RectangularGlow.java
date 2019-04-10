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
package com.mastfrog.visualtabs.util;

import com.mastfrog.visualtabs.TabsAppearance;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 * @author Tim Boudreau
 */
public final class RectangularGlow implements GradientPainter {

    final Color dark;
    final Color light;
    final Gradients gradients;
    final int glowWidth;

    public RectangularGlow(TabsAppearance appearance) {
        this(appearance.glowDark(), appearance.glowLight(), appearance.gradients(), appearance.glowWidth());
    }

    public RectangularGlow(Color dark, Color light, Gradients gradients, int glowWidth) {
        this.dark = dark;
        this.light = light;
        this.gradients = gradients;
        this.glowWidth = glowWidth;
    }

    @Override
    public void fill(Graphics2D g, Rectangle r) {
        if (r == null || r.width == 0 || r.height == 0) {
            return;
        }
        Rectangle rect = new Rectangle(r.x + glowWidth, r.y, r.width - (glowWidth * 2), glowWidth);
        painter(Region.TOP, rect, g).fill(g, rect);
        rect.translate(0, r.height - (glowWidth));
        painter(Region.BOTTOM, rect, g).fill(g, rect);
        rect.translate(-glowWidth, -(r.height - glowWidth));
        rect.width += glowWidth * 2;
        rect.x = r.x;
        rect.y = r.y + glowWidth;
        rect.width = glowWidth;
        rect.height = r.height - (glowWidth * 2);
        painter(Region.LEFT, rect, g).fill(g, rect);
        rect.translate(r.width - glowWidth, 0);
        painter(Region.RIGHT, rect, g).fill(g, rect);
        rect.x = r.x;
        rect.y = r.y;
        rect.width = glowWidth;
        rect.height = glowWidth;
        painter(Region.TOP_LEFT, rect, g).fill(g, rect);
        rect.translate(r.width - glowWidth, 0);
        painter(Region.TOP_RIGHT, rect, g).fill(g, rect);
        rect.translate(-(r.width - glowWidth), r.height - glowWidth);
        painter(Region.BOTTOM_LEFT, rect, g).fill(g, rect);
        rect.translate(r.width - glowWidth, 0);
        painter(Region.BOTTOM_RIGHT, rect, g).fill(g, rect);
    }

    GradientPainter painter(Region side, Rectangle r, Graphics2D g) {
        int glowSize = Math.min(r.width, r.height) - 1;
        switch (side) {
            case TOP:
                return gradients.linear(g, r.x, r.y + glowSize, dark, r.x, r.y, light);
            case LEFT:
                return gradients.linear(g, r.x, r.y, light, r.x + glowSize, r.y, dark);
            case BOTTOM:
                return gradients.linear(g, r.x, r.y, dark, r.x, r.y + glowSize, light);
            case RIGHT:
                return gradients.linear(g, r.x, r.y, dark, r.x + glowSize, r.y, light);
            case TOP_LEFT:
                return gradients.radial(g, r.x, r.y, dark, light, glowSize);
            case TOP_RIGHT:
                return gradients.radial(g, r.x - glowSize, r.y, dark, light, glowSize);
            case BOTTOM_LEFT:
                return gradients.radial(g, r.x, r.y - glowSize, dark, light, glowSize);
            case BOTTOM_RIGHT:
                return gradients.radial(g, r.x - glowSize, r.y - glowSize, dark, light, glowSize);
            default:
                throw new AssertionError(side);
        }
    }

    private enum Region {
        LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_RIGHT
    }

}
