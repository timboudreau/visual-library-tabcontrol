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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Paints a gradient or similar, which may be cached as a BufferedImage
 * for performance.
 *
 * @author Tim Boudreau
 */
public interface GradientPainter {

    void fill(Graphics2D g, Rectangle bounds);

    default void fill(Graphics2D g, int x, int y, int w, int h) {
        fill(g, new Rectangle(x, y, w, h));
    }

    default void fillShape(Graphics2D g, Shape shape) {
        Shape oldClip = g.getClip();
        g.setClip(shape);
        fill(g, shape.getBounds());
        g.setClip(oldClip);
    }

}
