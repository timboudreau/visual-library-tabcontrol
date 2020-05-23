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
package com.mastfrog.colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author Tim Boudreau
 */
final class RadialGradientPainter implements GradientPainter {

    final BufferedImage img;
    final int x;
    final int y;
    final Color fillColor;
    final AffineTransform invertTransform;

    RadialGradientPainter(BufferedImage img, int x, int y, Color fillColor, AffineTransform invertTransform) {
        this.img = img;
        this.x = x;
        this.y = y;
        this.fillColor = fillColor;
        this.invertTransform = invertTransform;
    }

    @Override
    public String toString() {
        return "Radial{" + x + "," + y + " fill=" + fillColor + ", imgSize=" + img.getWidth() + "," + img.getHeight() + "}";
    }

    @Override
    public void fill(Graphics2D g, Rectangle bounds) {
        AffineTransform xform = AffineTransform.getTranslateInstance(bounds.x, bounds.y);
        if (Gradients.SCALING_SUPPORT) {
            if (invertTransform != GradientUtils.NO_XFORM) {
                xform.concatenate(invertTransform);
                bounds = invertTransform.createTransformedShape(bounds).getBounds();
            }
        }
        BufferedImage bi = img;
        int imageX = bounds.x - x;
        int imageY = bounds.y - y;
        int imageW = Math.min(bounds.width, bi.getWidth() - imageX);
        int imageH = Math.min(bounds.height, bi.getHeight() - imageY);
        if (imageW > 0 && imageH > 0 && imageX < bi.getWidth() && imageY < bi.getHeight() && imageY >= 0 && imageX >= 0) {
            if (imageX != 0 || imageY != 0 || imageW != bi.getWidth() || imageH != bi.getHeight()) {
                // Get just the subset of the image we can use
                bi = img.getSubimage(imageX, imageY, imageW, imageH);
            }
            g.drawRenderedImage(bi, xform);
        }
        int xRemainder = bounds.width - bi.getWidth();
        int yRemainder = bounds.height - bi.getHeight();
        if (xRemainder > 0) {
            g.setColor(fillColor);
            g.fillRect(bounds.x + bi.getWidth(), bounds.y, xRemainder, bounds.height);
        }
        if (yRemainder > 0) {
            g.setColor(fillColor);
            g.fillRect(bounds.x, bounds.y + bi.getHeight(), bounds.width, yRemainder);
        }
    }
}
