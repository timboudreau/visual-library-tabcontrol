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
package com.mastfrog.visualtabs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.function.IntSupplier;
import org.netbeans.api.visual.border.Border;

/**
 *
 * @author Tim Boudreau
 */
final class RaggedBorder implements Border {

    private final TabsAppearance appearance;
    private final int depthLeft;
    private final int depthRight;
    private final IntSupplier selectionDirection;
    private boolean leftVisible = true;
    private boolean rightVisible = true;

    public RaggedBorder(int depthLeft, int depthRight, TabsAppearance appearance, IntSupplier selectionDirection) {
        this.depthLeft = depthLeft;
        this.depthRight = depthRight;
        this.appearance = appearance;
        this.selectionDirection = selectionDirection;
    }

    RaggedBorder setLeftVisible(boolean vis) {
        leftVisible = vis;
        return this;
    }

    RaggedBorder setRightVisible(boolean vis) {
        rightVisible = vis;
        return this;
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, leftVisible ? depthLeft : 0, 0, rightVisible ? depthRight : 0);
    }

    @Override
    public void paint(Graphics2D gr, Rectangle bounds) {
        int selDir = selectionDirection.getAsInt();
        Color col = (Color) appearance.getBackground();
        Color end = TabsAppearance.alpha(0, col);
        Color selEnd = appearance.selectionDirectionIndicatorColor();

        if (leftVisible) {
            appearance.gradients().linear(gr, bounds.x, bounds.y, col, bounds.x + depthLeft, bounds.y,
                    selDir == -1 ? selEnd : end).fill(gr, bounds.x, bounds.y, depthLeft, bounds.height);
        }

        if (rightVisible) {
            appearance.gradients().linear(gr, bounds.x + bounds.width - depthRight, bounds.y,
                    selDir == 1 ? selEnd : end, bounds.x + bounds.width, bounds.y, col)
                    .fill(gr, bounds.x + bounds.width - depthRight, bounds.y, depthRight, bounds.height);
        }
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
