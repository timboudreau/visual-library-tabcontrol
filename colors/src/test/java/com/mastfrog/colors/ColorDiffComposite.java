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

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
final class ColorDiffComposite implements Composite, CompositeContext {

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return this;
    }

    @Override
    public void dispose() {
        // do nothing
    }
    int[] WHITE = new int[]{0, 255, 0, 255};

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        int w = Math.min(dstIn.getWidth(), src.getWidth());
        int h = Math.min(dstIn.getHeight(), src.getHeight());
        int[] scratchA = new int[4];
        int[] scratchB = new int[4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src.getPixel(x, y, scratchA);
                dstIn.getPixel(x, y, scratchB);
                if (!Arrays.equals(scratchA, scratchB)) {
                    for (int i = 0; i < scratchA.length - 1; i++) {
                        int diff = scratchA[i] - scratchB[i];
                        int val = Math.max(0, Math.min(255, 128 + diff));
                        scratchA[i] = val;
                    }
                    scratchA[3] = 255;
                    dstOut.setPixel(x, y, scratchA);
                } else {
                    dstOut.setPixel(x, y, WHITE);
                }
            }
        }
    }

}
