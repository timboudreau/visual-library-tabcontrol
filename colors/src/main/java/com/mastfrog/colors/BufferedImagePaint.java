package com.mastfrog.colors;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 * @author Tim Boudreau
 */
final class BufferedImagePaint implements Paint {

    private final BufferedImage img;
    private final boolean vertical;

    public BufferedImagePaint(BufferedImage img, boolean vertical) {
        this.img = img;
        this.vertical = vertical;
    }

    @Override
    public int getTransparency() {
        return Transparency.TRANSLUCENT;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return new PC(cm, deviceBounds, userBounds, xform, hints);
    }

    class PC implements PaintContext {

        private final ColorModel cm;
        private final Rectangle deviceBounds;
        private final Rectangle2D userBounds;
        private final AffineTransform xform;
        private final RenderingHints hints;

        private PC(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
            this.cm = cm;
            this.deviceBounds = deviceBounds;
            this.userBounds = userBounds;
            this.xform = xform;
            this.hints = hints;
        }

        @Override
        public void dispose() {
        }

        @Override
        public ColorModel getColorModel() {
            return img.getColorModel();
        }

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            // This works, but for the wrong reason - we
            // are being called once for every scan line.
            // Why???
            Raster orig = img.getRaster();
            WritableRaster r = getColorModel()
                    .createCompatibleWritableRaster(w, h);
            int filled = 0;
            orig = orig.createTranslatedChild(-x, -y);
            while (filled < (!vertical ? h : w)) {
                int fx = !vertical ? 0 : x + filled;
                int fy = !vertical ? y + filled : 0;
                r.setRect(fx, fy, orig);
                filled += !vertical ? orig.getHeight() : orig.getWidth();
            }
            return r;
        }
    }
}
