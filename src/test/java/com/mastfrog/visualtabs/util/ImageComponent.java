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

import static com.mastfrog.visualtabs.util.GradientUtils.colorToString;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author Tim Boudreau
 */
final class ImageComponent extends JPanel {

    ImageComponent(BufferedImage img, String title, int zoom) {
        setLayout(new BorderLayout());
        JLabel ttl = new JLabel(title);
        ttl.setHorizontalAlignment(SwingConstants.CENTER);
        add(ttl, BorderLayout.NORTH);
        ImageView view = new ImageView(img, zoom);
        view.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        add(view, BorderLayout.CENTER);
    }

    static class ImageView extends JComponent {

        private final BufferedImage img;
        private final float zoom;

        public ImageView(BufferedImage img, float zoom) {
            this.img = img;
            this.zoom = zoom;
            setToolTipText("Image");
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            Insets ins = getInsets();
            int x = (int) ((event.getX() - ins.left) / zoom);
            int y = (int) ((event.getY() - ins.right) / zoom);
            if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                Color px = new Color(img.getRGB(x, y), true);
                return x + "," + y + ": " + colorToString(px);
            }
            return "(off image)";
        }

        public Dimension getPreferredSize() {
            int w = (int) Math.ceil(img.getWidth() * zoom);
            int h = (int) Math.ceil(img.getHeight() * zoom);
            Insets ins = getInsets();
            if (ins != null) {
                w += ins.left + ins.right;
                h += ins.top + ins.bottom;
            }
            return new Dimension(w, h);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        private AffineTransform insetsTransform() {
            Insets ins = getInsets();
            if (ins != null) {
                return AffineTransform.getTranslateInstance(ins.left, ins.top);
            }
            return AffineTransform.getTranslateInstance(0, 0);
        }

        private AffineTransform imageTransform() {
            AffineTransform xform = insetsTransform();
            xform.concatenate(AffineTransform.getScaleInstance(zoom, zoom));
            return xform;
        }

        public void paint(Graphics gr) {
            Graphics2D g = (Graphics2D) gr;
            //                prepareGraphics(g);
            g.drawRenderedImage(img, imageTransform());
            javax.swing.border.Border b = getBorder();
            if (b != null) {
                b.paintBorder(this, gr, 0, 0, getWidth(), getHeight());
            }
        }
    }

}
