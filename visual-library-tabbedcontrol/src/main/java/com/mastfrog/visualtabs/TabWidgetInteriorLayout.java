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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class TabWidgetInteriorLayout implements Layout {

    private final int pad;

    public TabWidgetInteriorLayout(int pad) {
        this.pad = pad;
    }

    @Override
    public void layout(Widget widget) {
        int maxY = 0;
        int maxH = 0;
        for (Widget w : widget.getChildren()) {
            Rectangle r = w.getPreferredBounds();
            if (r == null) {
                continue;
            }
            maxY = Math.max(maxY, r.y + r.height);
            maxH = Math.max(maxH, r.height);
        }
        int pos = widget.getBorder().getInsets().left;
        int top = widget.getBorder().getInsets().top;
        int totalWidth = Integer.MAX_VALUE;
        if (widget.isPreferredBoundsSet()) {
            totalWidth = widget.getPreferredBounds().width;
        }
        List<Widget> kids = new ArrayList<>(widget.getChildren());
        if (!((TabWidget) widget).closeButton.isEnabled()) {
            Collections.reverse(kids);
        }
        for (Iterator<Widget> it = kids.iterator(); it.hasNext();) {
            Widget w = it.next();
            Rectangle preferredBounds = w.getPreferredBounds();
            if (preferredBounds == null) {
                continue;
            }
            int x = preferredBounds.x;
            int y = preferredBounds.y;
            int lx = pos - x;
            int ly = -y;
            ly += top;
            preferredBounds.height = maxH;
            if (!it.hasNext()) {
                preferredBounds.width += pad;
            }
            if (pos + preferredBounds.width > totalWidth) {
                int rem = totalWidth - pos;
                preferredBounds.width = Math.max(0, rem);
            }
            w.resolveBounds(new Point(lx, ly), preferredBounds);
            pos += preferredBounds.width;
        }
    }

    @Override
    public boolean requiresJustification(Widget widget) {
        return true;
    }

    @Override
    public void justify(Widget widget) {
        layout(widget);
    }
}
