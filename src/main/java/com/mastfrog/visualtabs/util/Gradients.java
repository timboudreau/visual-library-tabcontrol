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
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The problems with GradientPaint and friends are that they allocate a fairly
 * large raster every time they paint, and depend on pixel-pushing operations
 * that are suboptimal for modern graphics cards. And with radial gradient
 * paints and vertical or horizontal linear gradient paints that are going to be
 * used repeatedly, it is perfectly acceptable to allocate the gradient once and
 * use it like a rubber stamp. This class does that, writing the gradient into a
 * transparent BufferedImage, and normalizing the parameters so that the same
 * image can be used for any compatible gradient (i.e. if you want a radial
 * gradient, and use multiple quadrants of it at different times, only one image
 * is ever created and cached; same with vertical and horizontal gradients at
 * different positions with the same colors and dimension in the direction of
 * the gradient).
 * <p>
 * That sort of caching can also be done in a custom Paint implementation, and
 * when developing this that was tested too. In a non-micro benchmark which
 * counted how many gradients could be painted looping as fast as possible, in
 * two seconds, for many rounds, the BufferedImage approach was 10x faster than
 * the Paint implementation, and also far more predictable - forcing garbage
 * collection between loops resulted in the BufferedImage implementation being
 * 40x faster than the Paint implementation..
 * </p>
 * <p>
 * The gradient painters returned here can be used pretty much the same way as
 * the associated gradient paints; you need to pass a Graphics parameter when
 * creating one, since caching is done based on graphics device and transform
 * scale, so that different device scaling doesn't result in windows which paint
 * strangely when dragged to a different monitor.
 * </p>
 *
 * @author Tim Boudreau
 */
public class Gradients {

    private static final int DEFAULT_DIMENSION = 12;

    Consumer<BufferedImage> onImageCreate; // for tests

    private static final float[] ZERO_ONE = new float[]{0, 1};

    private final Map<String, Map<GradientKey, BufferedImage>> gradientImageForKeyForDeviceId
            = new HashMap<>(3);
    private final Map<String, Map<RadialKey, BufferedImage>> radialImageForKeyForDeviceId
            = new HashMap<>(3);

    private Map<GradientKey, BufferedImage> gradientMap(Graphics2D g) {
        String id = devAndTransformId(g);
        Map<GradientKey, BufferedImage> result = gradientImageForKeyForDeviceId.get(id);
        if (result == null) {
            result = new HashMap<>(40);
            gradientImageForKeyForDeviceId.put(id, result);
        }
        return result;
    }

    private String devAndTransformId(Graphics2D g) {
        AffineTransform xform = g.getTransform();
        if (xform != null && xform.getScaleX() != 1D || xform.getScaleY() != 1D) {
            double scaleX = xform.getScaleX();
            double scaleY = xform.getScaleY();
            return g.getDeviceConfiguration().getDevice().getIDstring() + ";" + scaleX + ":" + scaleY;
        } else {
            return g.getDeviceConfiguration().getDevice().getIDstring();
        }
    }

    private Map<RadialKey, BufferedImage> radialMap(Graphics2D g) {
        String id = devAndTransformId(g);
        Map<RadialKey, BufferedImage> result = radialImageForKeyForDeviceId.get(id);
        if (result == null) {
            result = new HashMap<>(40);
            radialImageForKeyForDeviceId.put(id, result);
        }
        return result;
    }

    private BufferedImage imageForKey(Graphics2D g, RadialKey rk, Supplier<BufferedImage> ifAbsent) {
        Map<RadialKey, BufferedImage> m = radialMap(g);
        BufferedImage result = m.get(rk);
        if (result == null) {
            result = ifAbsent.get();
            m.put(rk, result);
        }
        return result;
    }

    private BufferedImage imageForKey(Graphics2D g, GradientKey gk, Supplier<BufferedImage> ifAbsent) {
        Map<GradientKey, BufferedImage> m = gradientMap(g);
        BufferedImage result = m.get(gk);
        if (result == null) {
            result = ifAbsent.get();
            m.put(gk, result);
        }
        return result;
    }

    private AffineTransform invertTransform(Graphics2D g) {
        AffineTransform xform = g.getTransform();
        if (xform != null && xform.getScaleX() == 1D || xform.getScaleY() == 1D) {
            return GradientUtils.NO_XFORM;
        } else {
            return AffineTransform.getScaleInstance(1D / xform.getScaleX(), 1D / xform.getScaleY());
        }
    }

    /**
     * Create a radial gradient paint with two colors and a spread of 0.0F to
     * 1.0F.
     *
     * @param g The graphics context to paint into
     * @param x The center x coordinate
     * @param y The center y coordinate
     * @param a The color at the center
     * @param b The color at the radius
     * @param radius The radius
     * @return A painter, which uses a bitmap that will be cached for future use
     */
    public GradientPainter radial(Graphics2D g, int x, int y, Color a, Color b, int radius) {
        if (radius == 0 || a == b || a.equals(b)) {
            return new ColorPainter(b);
        }
        RadialKey rk = new RadialKey(radius, a, b);
        BufferedImage img = imageForKey(g, rk, () -> {
            return createRadialGradientImage(g, x, y, a, b, radius);
        });
        return new RadialGradientPainter(img, x, y, b, invertTransform(g));
    }

    /**
     * Create a linear gradient painter. If the gradient is neither horizontal
     * nor vertical, an uncached painter which simply creates and fills a
     * GradientPaint is used, in which case there is no benefit from caching.
     *
     * @param g The graphics context, to cache by graphics device
     * @param x1 The starting x coordinate
     * @param y1 The starting y coordinate
     * @param top The color at the starting coordinates
     * @param x2 The ending x coordinate
     * @param y2 The ending y coordinate
     * @param bottom The color at the end coordinates
     * @return A painter
     */
    public GradientPainter linear(Graphics2D g, int x1, int y1, Color top, int x2, int y2, Color bottom) {
        if (top == bottom || top.equals(bottom) || (x1 == x2 && y1 == y2)) {
            return new ColorPainter(top);
        }
        GradientKey key = GradientKey.forGradientSpec(x1, y1, top, x2, y2, bottom);
        if (key == null) {
            return new NonCacheableGradientPainter(x1, y1, top, x2, y2, bottom);
        }
        BufferedImage img = imageForKey(g, key, () -> {
            return createLinearGradientImage(g, x1, y1, top, x2, y2, bottom);
        });

        return new LinearGradientPainter(img, Math.min(x1, x2), Math.min(y1, y2), key.isVertical(),
                key.topColor(), key.bottomColor(), invertTransform(g));
    }

    private BufferedImage createRadialGradientImage(Graphics2D g, int x, int y, Color a, Color b, int radius) {
//        AffineTransform xform = g.getTransform();
//        if (xform.getScaleX() != 1 || xform.getScaleY() != 1) {
//            double max = Math.max(xform.getScaleX(), xform.getScaleY());
//            System.out.println("oldRadius " + radius);
//            radius *= max;
//            System.out.println("newradius " + radius);
//        }
        int xpar = transparencyMode(a, b);
        Color[] colors = new Color[]{a, b};
        BufferedImage img = g.getDeviceConfiguration().createCompatibleImage(radius * 2, radius * 2, xpar);
        Graphics2D gg = img.createGraphics();
        RadialGradientPaint rgp = new RadialGradientPaint(radius, radius, radius, ZERO_ONE, colors, CycleMethod.NO_CYCLE);
        GradientUtils.prepareGraphics(gg);
        gg.setPaint(rgp);
        gg.fillRect(0, 0, radius * 2, radius * 2);
        gg.dispose();
        if (onImageCreate != null) {
            onImageCreate.accept(img);
        }
        return img;
    }

    public GradientPainter vertical(Graphics2D g, int x, int y1, Color top, int height, Color bottom) {
        return linear(g, x, y1, top, x, y1 + height, bottom);
    }

    public GradientPainter horizontal(Graphics2D g, int x, int y, Color top, int width, Color bottom) {
        return linear(g, x, y, top, x + width, y, bottom);
    }

    private static int transparencyMode(Color a, Color b) {
        int ta = a.getTransparency();
        int tb = b.getTransparency();
        if (ta == Transparency.OPAQUE && tb == Transparency.OPAQUE) {
            return Transparency.OPAQUE;
        }
        return Transparency.TRANSLUCENT;
    }

    private static boolean SCALING_SUPPORT = false;

    private BufferedImage createLinearGradientImage(Graphics2D g, int x1, int y1, Color top, int x2, int y2, Color bottom) {
        return GradientKey.normalize(x1, y1, top, x2, y2, bottom, (nx1, ny1, nTop, nx2, ny2, nBottom, normed) -> {
            if (SCALING_SUPPORT) {
                AffineTransform xform = g.getTransform();
                boolean scaling = xform.getScaleX() != 1 || xform.getScaleY() != 1;
                if (scaling) {
                    double max = Math.max(xform.getScaleX(), xform.getScaleY());
//                get the distance
                    double xdist = max * (nx2 - nx1);
                    double ydist = max * (ny2 - ny1);
                    System.out.println("Scale width to " + xdist + " from " + (nx2 - nx1));
                    System.out.println("Scale height to " + ydist + " from " + (ny2 - ny1));
                    nx2 = (int) (nx1 + xdist);
                    ny2 = (int) (ny1 + ydist);
                }
            }

            int w, h;
            if (nx1 == nx2) {
                w = DEFAULT_DIMENSION;
                h = ny2 - ny1;
            } else {
                h = DEFAULT_DIMENSION;
                w = nx2 - nx1;
            }
            int xpar = transparencyMode(top, bottom);
            BufferedImage img = g.getDeviceConfiguration().createCompatibleImage(w, h, xpar);
            GradientPaint pt = new GradientPaint(nx1, ny1, nTop, nx2, ny2, nBottom);
            Graphics2D gg = img.createGraphics();
            GradientUtils.prepareGraphics(gg);
            gg.setPaint(pt);
            gg.fillRect(0, 0, w, h);
            gg.dispose();
            if (onImageCreate != null) {
                onImageCreate.accept(img);
            }
            return img;
        });
    }

    private static final class RadialGradientPainter implements GradientPainter {

        final BufferedImage img;
        final int x;
        final int y;
        final Color fillColor;
        final AffineTransform invertTransform;

        public RadialGradientPainter(BufferedImage img, int x, int y, Color fillColor, AffineTransform invertTransform) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.fillColor = fillColor;
            this.invertTransform = invertTransform;
        }

        public String toString() {
            return "Radial{" + x + "," + y + " fill=" + fillColor + ", imgSize=" + img.getWidth() + "," + img.getHeight() + "}";
        }

        @Override
        public void fill(Graphics2D g, Rectangle bounds) {
            AffineTransform xform = AffineTransform.getTranslateInstance(bounds.x, bounds.y);
            if (SCALING_SUPPORT) {
                if (invertTransform != GradientUtils.NO_XFORM) {
                    xform.concatenate(invertTransform);
                    System.out.println("oldbounds " + bounds);
                    bounds = invertTransform.createTransformedShape(bounds).getBounds();
                    System.out.println("newbounds " + bounds);
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

    private static final class LinearGradientPainter implements GradientPainter {

        final BufferedImage img;
        final int x;
        final int y;
        final boolean vertical;
        final Color before;
        final Color after;
        final AffineTransform inverseTransform;

        public LinearGradientPainter(BufferedImage img, int x, int y, boolean vertical, Color before, Color after, AffineTransform invertTransform) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.vertical = vertical;
            this.before = before;
            this.after = after;
            this.inverseTransform = invertTransform;
        }

        @Override
        public String toString() {
            return (vertical ? "Vert{" : "Horiz{")
                    + "{" + x + "," + y + " before=" + before
                    + " after=" + after
                    + ", imgSize=" + img.getWidth() + "," + img.getHeight() + "}";
        }

        @Override
        public void fill(Graphics2D g, Rectangle bounds) {
//            AffineTransform origTransform = g.getTransform();
            boolean verticalTiling = !this.vertical;
            bounds = new Rectangle(bounds);
            int x = this.x;
            int y = this.y;
            BufferedImage bi = img;
            int imageX = Math.max(0, bounds.x - x); // questionable
            int imageY = Math.max(0, bounds.y - y);
            int imageW = Math.min(bounds.width, bi.getWidth() - imageX);
            int imageH = Math.min(bounds.height, bi.getHeight() - imageY);

            double scaleX = 1;
            double scaleY = 1;
            if (SCALING_SUPPORT) {
                if (inverseTransform != GradientUtils.NO_XFORM) {
                    scaleX = inverseTransform.getScaleX();
                    scaleY = inverseTransform.getScaleY();
                    g.transform(inverseTransform);
                    g.translate(x, y);
//                AffineTransform b = inverseTransform;
//                System.out.println("orig bounds " + bounds);
//                bounds = inverseTransform.createTransformedShape(bounds).getBounds();
//                System.out.println("Invert transform to " + bounds);
                    x *= scaleX;
                    y *= scaleY;
//                imageX *= inverseTransform.getScaleX();
//                imageY *= inverseTransform.getScaleY();
//                imageW *= inverseTransform.getScaleX();
//                imageH *= inverseTransform.getScaleY();
//                System.out.println("Points from " + this.x + ", " + this.y + " to " + x + ", " + y);
                }
            }
            if (bounds.x < x) {
                if (verticalTiling) {
                    g.setColor(before);
                    g.fillRect(bounds.x, bounds.y, x, bounds.height);
                    bounds.width -= x - bounds.x;
                    bounds.x = x;
                }
            }
            if (bounds.y < y) {
                if (!verticalTiling) {
                    g.setColor(before);
                    g.fillRect(bounds.x, bounds.y, bounds.width, y);
                    bounds.height -= y;
                    bounds.y = y;
                }
            }

            if (bounds.width > bi.getWidth() * scaleX) {
                if (!verticalTiling) {
                    g.setColor(after);
                    int imageBottom = bounds.y + bi.getHeight();
                    int fillHeight = bounds.y + bounds.height - imageBottom;
                    g.fillRect(bounds.x, imageBottom, bounds.width, fillHeight);
                }
            }
            if (bounds.height > bi.getHeight() * scaleY) {
                if (verticalTiling) {
                    int imageRight = bounds.x + bi.getWidth();
                    int fillWidth = (bounds.x + bounds.width) - imageRight;
                    g.setColor(after);
                    g.fillRect(imageRight, bounds.y, fillWidth, bounds.height);
                }
            }

            if (imageX >= bi.getWidth() * scaleX || imageY >= bi.getHeight() * scaleY || imageX < 0 || imageY < 0 || imageH <= 0 || imageW <= 0) {
                // Fill was everything, we aren't painting within the bounds the
                // gradient would fill
                return;
            }

            if (imageX != 0 || imageY != 0 || imageW != img.getWidth() || imageH != img.getHeight()) {
                // Get just the subset of the image we can use
                try {
                    bi = bi.getSubimage(imageX, imageY, imageW, imageH);
                } catch (RasterFormatException rfe) {
                    throw new IllegalStateException("Creating subimage of image "
                            + bi.getWidth() + "x" + bi.getHeight() + " with bounds "
                            + imageX + "," + imageY + "," + imageW + "," + imageH, rfe);
                }
            }

            int position = verticalTiling ? bounds.y : bounds.x;
            int max = verticalTiling ? bounds.y + bounds.height : bounds.x + bounds.width;
            // Tile the image along the dimension in question
            while (position < max) {
                // Translate to the bounds position
                AffineTransform xform = verticalTiling
                        ? AffineTransform.getTranslateInstance(bounds.x, position)
                        : AffineTransform.getTranslateInstance(position, bounds.y);
                if (SCALING_SUPPORT) {
                    if (inverseTransform != GradientUtils.NO_XFORM) {
                        xform.concatenate(inverseTransform);
                    }
                }

                // See if we are going to tile past the rectangle bounds,
                // and trim the image if need be
                int imageDim = (int) (verticalTiling ? bi.getHeight() * scaleX : bi.getWidth() * scaleY);
                if (position + imageDim > max) {
                    // last round - snip off any of the image we don't want to
                    // paint, which would go outside the rectangle
                    int diff = (position + imageDim) - (max);
                    // Get a subimage of just the remainder
                    bi = verticalTiling ? bi.getSubimage(0, 0, bi.getWidth(), bi.getHeight() - diff)
                            : bi.getSubimage(0, 0, bi.getWidth() - diff, bi.getHeight());
                }
                g.drawRenderedImage(bi, xform);
                position += verticalTiling ? bi.getHeight() * scaleY : bi.getWidth() * scaleX;
            }
//            g.setTransform(origTransform);
        }
    }

    private static class NonCacheableGradientPainter implements GradientPainter {

        final int x1;
        final int y1;
        final Color top;
        final int x2;
        final int y2;
        final Color bottom;

        NonCacheableGradientPainter(int x1, int y1, Color top, int x2, int y2, Color bottom) {
            this.x1 = x1;
            this.y1 = y1;
            this.top = top;
            this.x2 = x2;
            this.y2 = y2;
            this.bottom = bottom;
        }

        @Override
        public String toString() {
            return "NonCacheableGradientPainter{" + "x1=" + x1 + ", y1=" + y1
                    + ", top=" + colorToString(top) + ", x2=" + x2 + ", y2="
                    + y2 + ", bottom=" + colorToString(bottom) + '}';
        }

        @Override
        public void fill(Graphics2D g, Rectangle bounds) {
            Paint old = g.getPaint();
            GradientPaint gp = new GradientPaint(x1, y1, top, x2, y2, bottom);
            g.setPaint(gp);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            if (old != null) {
                g.setPaint(old);
            }
        }
    }

    static class ColorPainter implements GradientPainter {

        private final Color color;

        public ColorPainter(Color color) {
            this.color = color;
        }

        @Override
        public void fill(Graphics2D g, Rectangle bounds) {
            g.setColor(color);
            g.fill(bounds);
        }

    }
}
