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

import com.mastfrog.visualtabs.util.RectangularGlow;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 * Paints a fading glow around the selected tab.
 *
 * @author Tim Boudreau
 */
class GlowWidget extends Widget {

    private final TabsAppearance appearance;

    public GlowWidget(Scene scene, TabsAppearance appearance) {
        super(scene);
        setOpaque(false);
        this.appearance = appearance;
        setCheckClipping(false);
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        Rectangle r = getBounds();
        if (r == null || r.width == 0 || r.height == 0 || !isVisible()) {
            return;
        }
        RectangularGlow glow = appearance.glow();
        glow.fill(g, r);
    }

}
