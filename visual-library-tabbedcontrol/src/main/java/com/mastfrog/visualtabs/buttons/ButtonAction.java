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
package com.mastfrog.visualtabs.buttons;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 *
 * @author Tim Boudreau
 */
public enum ButtonAction {
    LEFT, RIGHT, POPUP, MAXIMIZE;

    Shape shape(Rectangle r) {
        switch (this) {
            case LEFT:
                return left(r);
            case RIGHT:
                return right(r);
            case POPUP:
                return down(r);
            case MAXIMIZE:
                return maximize(r);
            default:
                throw new AssertionError(this);
        }
    }

    private Shape left(Rectangle r) {
        int[] x = {r.x + 1, r.x + r.width - 2, r.x + r.width - 2};
        int[] y = {r.y + (r.height / 2) - 1, r.y + 1, r.y + r.height - 2};
        return new LPolygon(x, y, 3);
    }

    private Shape right(Rectangle r) {
        int[] x = {r.x + 1, r.x + 1, r.x + r.width - 2};
        int[] y = {r.y + 1, r.y + (r.height - 2), r.y + 1 + (r.height / 2)};
        return new LPolygon(x, y, 3);
    }

    private Shape down(Rectangle r) {
        int[] x = {r.x + 1, r.x + r.width - 2, r.x + (r.width / 2)};
        int[] y = {r.y + 1, r.y + 1, r.y + r.height - 2};
        return new LPolygon(x, y, 3);
    }
    private Rectangle lastMaxRect;
    private Shape lastMax;

    private Shape maximize(Rectangle r) {
        if (r.equals(lastMaxRect)) {
            return lastMax;
        }
        r.y += 2;
        r.height -= 4;
        r.width-=3;
        int headSize = (r.width / 2) + (r.width / 6);

        int bottom = r.y + r.height;
        int right = r.x + r.width;
        int edge = headSize / 2;

        int centerX = r.x + (r.width / 2);
        int centerY = r.y + (r.height / 2);

        int shaftThickness = Math.max(1, r.height / 9);

        int[] xpoints = new int[]{r.x, r.x, r.x + headSize, r.x + edge + shaftThickness,
            centerX,
            right - (edge - shaftThickness),
            right,
            right,
            right - headSize,
            right - (edge + shaftThickness),
            centerX,
            r.x + edge - shaftThickness
        };
        int[] ypoints = new int[]{bottom - headSize, bottom, bottom, bottom - (edge - shaftThickness),
            centerY,
            r.y + edge + shaftThickness,
            r.y + headSize,
            r.y,
            r.y,
            r.y + edge - shaftThickness,
            centerY,
            bottom - (edge + shaftThickness)
        };
        return lastMax = new LPolygon(xpoints, ypoints, xpoints.length);
    }

    static final class LPolygon extends Polygon {

        public LPolygon(int[] xpoints, int[] ypoints, int npoints) {
            super(xpoints, ypoints, npoints);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Poly[");
            for (int i = 0; i < npoints; i++) {
                sb.append('<').append(xpoints[i]).append(',').append(ypoints[i]).append('>');
            }
            return sb.append(']').toString();
        }
    }
}
