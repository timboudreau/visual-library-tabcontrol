/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mastfrog.colors;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SynthPaintTest {
    Gradients grads = new Gradients();
    LinearGradientPainter lin;

    @BeforeEach
    public void setup() {
        ImageTestUtils.newImage(80, 80, g -> {
            lin = (LinearGradientPainter) grads.horizontal(g, 5, 5, new Color(180, 180, 255), 80, Color.ORANGE);
        });
    }

    @Test
    public void test() throws InterruptedException, Throwable {
        Ellipse2D.Double ell = new Ellipse2D.Double(5, 5, 40, 40);
        Rectangle bds = ell.getBounds();
        BufferedImage img = ImageTestUtils.newImage(50, 50, g -> {
            // In this case, the antialiased pixels will not match,
            // so turn them off
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.clearRect(0, 0, 50, 50);
//            g.setClip(ell);
            g.setClip(null);
            BufferedImagePaint pt = new BufferedImagePaint(lin.img, false);
            g.setPaint(pt);
            g.fill(ell);
        });
        BufferedImage img2 = ImageTestUtils.newImage(50, 50, g -> {
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.clearRect(0, 0, 50, 50);
            g.setClip(ell);
            int filled = 0;
            while (filled < bds.height) {
                g.drawRenderedImage(lin.img, AffineTransform.getTranslateInstance(0, filled));
                filled += lin.img.getHeight();
            }
        });

//        ImageTestUtils.enableVisualAssert();
        ImageTestUtils.assertImages(img, img2);
    }
}
