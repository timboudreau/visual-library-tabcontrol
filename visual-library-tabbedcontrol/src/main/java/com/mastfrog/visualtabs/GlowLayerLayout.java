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

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Supplier;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Widget;

/**
 * Layout that syncs the position of a widget with the on-screen position
 * of the selected widget.
 *
 * @author Tim Boudreau
 */
class GlowLayerLayout implements Layout {

    private final int glowWidth;
    private final Supplier<Widget> target;

    GlowLayerLayout(int glowWidth, Supplier<Widget> target) {
        this.glowWidth = glowWidth;
        this.target = target;
    }

    Rectangle rect(Widget w) {
        Widget widget = target.get();
        if (widget != null) {
            Rectangle r = widget.getBounds();
            if (r != null) {
                Insets ins = widget.getBorder().getInsets();
                r.x += ins.left;
                r.width -= ins.left + ins.right;
                r.y += ins.top;
                r.width -= ins.top + ins.bottom;
                r = widget.convertLocalToScene(r);
                r = w.convertSceneToLocal(r);
                r.x -= glowWidth;
                r.y -= glowWidth;
                r.width += glowWidth*2;
                r.height += glowWidth*2;
                return r;
            }
        }
        return new Rectangle(0, 0, 0, 0);
    }

    @Override
    public void layout(Widget widget) {
        Widget glow = widget.getChildren().get(0);
        Rectangle r = rect(widget);
        glow.resolveBounds(new Point(0, 0), r);
    }

    @Override
    public boolean requiresJustification(Widget widget) {
        return false;
    }

    @Override
    public void justify(Widget widget) {
        // do nothing
    }

}
