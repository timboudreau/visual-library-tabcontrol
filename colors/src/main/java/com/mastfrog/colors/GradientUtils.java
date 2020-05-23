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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;

/**
 *
 * @author Tim Boudreau
 */
final class GradientUtils {

    public static final AffineTransform NO_XFORM = AffineTransform.getTranslateInstance(0, 0);

    public static String transparencyToString(int xpar) {
        switch (xpar) {
            case Transparency.BITMASK:
                return "Bitmask";
            case Transparency.OPAQUE:
                return "Opaque";
            case Transparency.TRANSLUCENT:
                return "Translucent";
            default:
                return "Unknown-" + xpar;
        }
    }

    public static boolean isInvertableTransform(AffineTransform xform) {
        return xform.getDeterminant() != 0;
    }

    public static boolean isNoTransform(AffineTransform xform) {
        return xform == null || NO_XFORM.equals(xform);
    }

    public static String colorToString(Color c) {
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
    }

    public static String paintToString(Paint p) {
        if (p instanceof Color) {
            return "Color{" + GradientUtils.colorToString((Color) p) + "}";
        } else if (p instanceof RadialGradientPaint) {
            RadialGradientPaint rgp = (RadialGradientPaint) p;
            StringBuilder sb = new StringBuilder("RadialGradientPaint{");
            float[] fracs = rgp.getFractions();
            Color[] colors = rgp.getColors();
            sb.append(rgp.getRadius());
            sb.append(" @ ").append((int) rgp.getCenterPoint().getX())
                    .append(',').append((int) rgp.getCenterPoint().getY())
                    .append(":");
            for (int i = 0; i < fracs.length; i++) {
                sb.append(fracs[i]).append('=').append(GradientUtils.colorToString(colors[i]));
                if (i != fracs.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.append('}').toString();
        } else if (p instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) p;
            return "GradientPaint{" + (int) gp.getPoint1().getX() + "," + (int) gp.getPoint1().getY()
                    + ":" + GradientUtils.colorToString(gp.getColor1()) + " -> " + (int) gp.getPoint2().getX() + ","
                    + (int) gp.getPoint2().getY() + "=" + GradientUtils.colorToString(gp.getColor2()) + "}";
        }
        return p.toString();
    }

    public static void prepareGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private GradientUtils() {
    }
}
