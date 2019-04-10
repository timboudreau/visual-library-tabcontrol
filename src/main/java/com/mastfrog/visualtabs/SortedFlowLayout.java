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
import java.util.Comparator;
import java.util.List;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDataModel;

/**
 * Variant on FlowLayout which keeps the position order of tabs sorted
 * by their order in the model, so that <code>bringToFront()</code>
 * affects what <i>paints</i> on top of what, but not position.
 *
 * @author Tim Boudreau
 */
final class SortedFlowLayout implements Layout, Comparator<Widget> {

    private final TabDataModel model;
    private final boolean verticalOrientation;
    private final LayoutFactory.SerialAlignment alignment;
    private final int gap;
    private final int edgeGap;

    public SortedFlowLayout(TabDataModel model, boolean verticalOrientation, LayoutFactory.SerialAlignment alignment, int gap, int edgeGap) {
        this.model = model;
        this.verticalOrientation = verticalOrientation;
        this.alignment = alignment;
        this.gap = gap;
        this.edgeGap = edgeGap;
    }

    @Override
    public int compare(Widget o1, Widget o2) {
        if (o1 instanceof TabWidget && o2 instanceof TabWidget) {
            TabWidget ta = (TabWidget) o1;
            TabWidget tb = (TabWidget) o2;
            TabData da = ta.get();
            TabData db = tb.get();
            int ixa = model.indexOf(da);
            int ixb = model.indexOf(db);
            return ixa > ixb ? 1 : ixa == ixb ? 0 : -1;
        } else if (o1 instanceof TabWidget && !(o2 instanceof TabWidget)) {
            return -1;
        } else if (o2 instanceof TabWidget && !(o1 instanceof TabWidget)) {
            return 1;
        } else {
            return 0;
        }
    }

    List<Widget> widgetsSorted(Widget w) {
        List<Widget> result = new ArrayList<>(w.getChildren());
        Collections.sort(result, this);
        return result;
    }

    @Override
    public void layout(Widget widget) {
        int max = 0;
        List<Widget> children = widgetsSorted(widget);
        if (verticalOrientation) {
            for (Widget child : children) {
                if (!child.isVisible()) {
                    continue;
                }
                Rectangle preferredBounds = child.getPreferredBounds();
                int i = preferredBounds.width;
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
                int lx = -x;
                int ly = pos - y;
                switch (alignment) {
                    case CENTER:
                        lx += (max - width) / 2;
                        break;
                    case JUSTIFY:
                        width = max;
                        break;
                    case LEFT_TOP:
                        break;
                    case RIGHT_BOTTOM:
                        lx += max - width;
                        break;
                }
                if (child.isVisible()) {
                    child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, width, height));
                    pos += height + gap;
                } else {
                    child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, 0, 0));
                }
            }
        } else {
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
            int pos = edgeGap;
            for (Widget child : children) {
                Rectangle preferredBounds = child.getPreferredBounds();
                int x = preferredBounds.x;
                int y = preferredBounds.y;
                int width = preferredBounds.width;
                int height = preferredBounds.height;
                int lx = pos - x;
                int ly = -y;
                switch (alignment) {
                    case CENTER:
                        ly += (max - height) / 2;
                        break;
                    case JUSTIFY:
                        height = max;
                        break;
                    case LEFT_TOP:
                        break;
                    case RIGHT_BOTTOM:
                        ly += max - height;
                        break;
                }
                if (child.isVisible()) {
                    child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, width, height));
                    pos += width + gap;
                } else {
                    child.resolveBounds(new Point(lx, ly), new Rectangle(x, y, 0, 0));
                }
            }
        }
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
