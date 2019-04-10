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

import static com.mastfrog.visualtabs.util.ImageTestUtils.assertImages;
import static com.mastfrog.visualtabs.util.ImageTestUtils.newImage;
import static com.mastfrog.visualtabs.util.ImageTestUtils.showImage;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ScalingTest {

    private final Color COLOR_A = new Color(0, 0, 255, 255);
    private final Color COLOR_B = new Color(255, 127, 22, 0);
    Gradients gradients;

    @BeforeEach
    public void setup() {
        gradients = new Gradients();
        gradients.onImageCreate = showImage();
    }

    @Test
    public void testScaling() throws Throwable {
        if (true) {
            return;
        }
        BufferedImage expected = scaledImage(80, 80, 2, g -> {
            g.clearRect(0, 0, 80, 80);
            GradientPaint gp = new GradientPaint(10, 10, COLOR_A, 10, 20, COLOR_B, false);
            g.setPaint(gp);
            g.fillRect(10, 10, 50, 50);
        });
        BufferedImage img = newImage(160, 160, g -> {
            g.scale(2, 2);
            g.clearRect(0, 0, 80, 80);
            GradientPainter p = gradients.linear(g, 10, 10, COLOR_A, 10, 20, COLOR_B);
            p.fill(g, new Rectangle(10, 10, 50, 50));
        });
        assertImages(expected, img);
    }

    private BufferedImage scaledImage(int w, int h, int scale, Consumer<Graphics2D> c) {
        return newImage(w * scale, h * scale, gr -> {
            gr.scale(scale, scale);
            c.accept(gr);
        });
    }

    private BufferedImage scaledGradient(int w, int h, int scale, Consumer<Graphics2D> c) {
        return newImage(w * scale, h * scale, gr -> {
            gr.scale(scale, scale);
            c.accept(gr);
        });
    }
}
