/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.colors;

import static com.mastfrog.colors.ImageTestUtils.assertImages;
import static com.mastfrog.colors.ImageTestUtils.newImage;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TransformedGradientPainterTest {

    private final Color COLOR_A = new Color(80, 80, 255, 255);
    private final Color COLOR_B = new Color(255, 180, 22, 255);
    Gradients gradients;

    @BeforeEach
    public void setup() {
        gradients = new Gradients();
//        gradients.onImageCreate = showImage();
    }

    @Test
    public void testTransform() throws Throwable {
        if (true) {
            return;
        }
        int gx1 = 10;
        int gy1 = 10;
        int gx2 = 50;
        int gy2 = 50;
//        enableVisualAssert();
        BufferedImage expected = newImage(80, 80, g -> {
//            g.clearRect(0, 0, 80, 80);
            GradientPaint gp = new GradientPaint(gx1, gy1, COLOR_A, gx2, gy2, COLOR_B, true);
            g.setPaint(gp);
            g.fillRect(10, 10, 70, 50);
        });
        BufferedImage img = newImage(80, 80, g -> {
//            g.clearRect(0, 0, 80, 80);
            double len = Point2D.distance(gx1, gy1, gx2, gy2);
//            GradientPainter p = gradients.linear(g, gx1, gy1, COLOR_A, gx1,
//                    (int) len, COLOR_B);
            GradientPainter p = gradients.linear(g, gx1, gy1, COLOR_A, gx1,
                    gx2, COLOR_B);

            TransformedGradientPainter ptr = new TransformedGradientPainter(
                    gx1, gy1, gx2, gy2, p);

            ptr.fill(g, new Rectangle(10, 10, 70, 50));
        });
        assertImages(expected, img);
    }

    private static AffineTransform rotation(double x1, double y1, double x2, double y2) {
        double[] center = center(x1, y1, x2, y2);
        double theta = Math.atan2(x2 - center[0], y2 - center[1]);
        double deg = Math.toDegrees(theta);
        System.out.println("DEG " + deg);
        theta = Math.toRadians(deg - 90);
        return AffineTransform.getRotateInstance(theta, center[0], center[1]);
    }

    private static double[] center(double x1, double y1, double x2, double y2) {
        Rectangle2D.Double dbl = new Rectangle2D.Double();
        dbl.setFrameFromDiagonal(x1, y1, x2, y2);
        return new double[]{dbl.getCenterX(), dbl.getCenterY()};
    }
}
