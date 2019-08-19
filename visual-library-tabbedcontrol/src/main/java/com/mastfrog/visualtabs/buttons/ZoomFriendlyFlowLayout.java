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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class ZoomFriendlyFlowLayout implements Layout {
    // A patched / simplified version of FlowLayout from visual library -
    // in particular, that reuses the location from the preceding pass,
    // causing, when a zoom factor is present, widgets to shift progressively
    // further left
    @Override
    public void layout(Widget widget) {
        int max = 0;
        int gap = 0;
        List<Widget> children = widget.getChildren();
        for (Widget child : children) {
            if (!child.isVisible()) {
                continue;
            }
            Rectangle preferredBounds = child.getPreferredBounds();
            int i = preferredBounds.height;
            if (i > max) {
                max = i;
            }
        }
        int pos = 0;
        for (Widget child : children) {
            Rectangle preferredBounds = child.getPreferredBounds();
            int x = preferredBounds.x;
            int y = preferredBounds.y;
            int width = preferredBounds.width;
            int height = preferredBounds.height;
            int lx = pos - x;
            int ly = -y;
            ly += (max - height) / 2;
            if (child.isVisible()) {
                child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, width, height));
                pos += width + gap;
            } else {
                child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, 0, 0));
            }
        }
    }

    @Override
    public boolean requiresJustification(Widget widget) {
        return true;
    }

    @Override
    public void justify(Widget widget) {
        Rectangle parentBounds = widget.getClientArea();
        int parentX1 = parentBounds.x;
        int parentY1 = parentBounds.y;
        int parentY2 = parentY1 + parentBounds.height;
        int pos = 0;
        for (Widget child : widget.getChildren()) {
            if (!child.isVisible()) {
                continue;
            }
            Point childLocation = child.getLocation();
            childLocation.x = pos;
            Rectangle childBounds = child.getBounds();
            childLocation.y = (parentY1 + parentY2 - childBounds.height) / 2;
            childLocation.y -= childBounds.y;
            childLocation.x += parentX1;
            child.resolveBounds(childLocation, childBounds);
            pos += childBounds.width;
        }
    }

}
