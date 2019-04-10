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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.function.Supplier;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.swing.tabcontrol.TabData;
import org.openide.awt.HtmlRenderer;

/**
 *
 * @author Tim Boudreau
 */
final class DynamicLabelWidget extends LabelWidget {

    private final Supplier<TabData> data;
    private final TabsAppearance appearance;
    private static final double M_PI_2 = Math.PI / 2;

    DynamicLabelWidget(Scene scene, Supplier<TabData> data, TabsAppearance appearance) {
        super(scene);
        this.data = data;
        this.appearance = appearance;
        setMinimumSize(new Dimension(1, 24));
        super.setCheckClipping(true);
    }

    int centerY() {
        return lastCenter;
    }

    int fontHeight() {
        return fontHeight;
    }

    int lastCenter = 12;
    int fontHeight = 18;

    protected Rectangle calculateClientArea() {
        String label = getLabel();
        Graphics2D gr = getGraphics();
        if (label == null || gr == null) {
            return super.calculateClientArea();
        }
        Rectangle rectangle;

        Font f = getFont();
        gr.setFont(f);
        FontMetrics fm = gr.getFontMetrics();
        fontHeight = fm.getHeight();
        int maxAscent = fm.getMaxAscent();
        lastCenter = maxAscent / 2;

        double w = HtmlRenderer.renderHTML(label, gr, 0, maxAscent,
                Integer.MAX_VALUE, fontHeight, f, Color.BLACK, HtmlRenderer.STYLE_CLIP, false);
        int width = (int) Math.ceil(w) + 4;

        rectangle = new Rectangle(0, 0, width, fontHeight);
        switch (getOrientation()) {
            case NORMAL:
                return rectangle;
            case ROTATE_90:
                return new Rectangle(rectangle.y, -rectangle.x - rectangle.width, rectangle.height, rectangle.width);
            default:
                throw new IllegalStateException();
        }
    }

    /*
    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        if (state.isHovered() && !state.isSelected()) {
            anim.start();
        } else {
            anim.stop();
        }
    }

    private final TransformAnimation anim = new TransformAnimation(36, 48, true,
            this::animTickRepaint);

    private void animTickRepaint() {
        Rectangle r = getBounds();
        if (r == null) {
            getScene().validate();
            r = getBounds();
            if (r == null) {
                return;
            }
        }
        r = convertLocalToScene(r);
        JComponent v = getScene().getView();
        if (v != null) {
            v.repaint(r);
        }
    }

    @Override
    protected void paintWidget() {
        Graphics2D gr = getGraphics();
        Rectangle r = getBounds();
        if (r == null) {
            return;
        }
        anim.withTransformAnimation(gr, r.width, r.height, () -> {
            doPaintWidget(gr);
        });
    }
     */
    @Override
    protected void paintWidget() {
        doPaintWidget(getGraphics());
    }

    void doPaintWidget(Graphics2D gr) {
        String label = getLabel();
        if (label == null) {
            return;
        }
        gr.setFont(getFont());
        FontMetrics fontMetrics = gr.getFontMetrics();
        Rectangle clientArea = getClientArea();
        Orientation orientation = getOrientation();
        Alignment alignment = getAlignment();
        VerticalAlignment verticalAlignment = getVerticalAlignment();
        int x;
        int y;
        int w;
        switch (orientation) {
            case NORMAL:
                w = clientArea.width;
                switch (alignment) {
                    case BASELINE:
                        x = 0;
                        break;
                    case LEFT:
                        x = clientArea.x;
                        break;
                    case CENTER:
                        x = clientArea.x + (clientArea.width - fontMetrics.stringWidth(label)) / 2;
                        break;
                    case RIGHT:
                        x = clientArea.x + clientArea.width - fontMetrics.stringWidth(label);
                        break;
                    default:
                        return;
                }
                switch (verticalAlignment) {
                    case BASELINE:
                        y = 0;
                        break;
                    case TOP:
                        y = clientArea.y + fontMetrics.getAscent();
                        break;
                    case CENTER:
                        y = clientArea.y + (clientArea.height + fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;
                        break;
                    case BOTTOM:
                        y = clientArea.y + clientArea.height - fontMetrics.getDescent();
                        break;
                    default:
                        return;
                }
                break;
            case ROTATE_90:
                w = clientArea.height;
                switch (alignment) {
                    case BASELINE:
                        x = 0;
                        break;
                    case LEFT:
                        x = clientArea.x + fontMetrics.getAscent();
                        break;
                    case CENTER:
                        x = clientArea.x + (clientArea.width + fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;
                        break;
                    case RIGHT:
                        x = clientArea.x + clientArea.width - fontMetrics.getDescent();
                        break;
                    default:
                        return;
                }
                switch (verticalAlignment) {
                    case BASELINE:
                        y = 0;
                        break;
                    case TOP:
                        y = clientArea.y + fontMetrics.stringWidth(label);
                        break;
                    case CENTER:
                        y = clientArea.y + (clientArea.height + fontMetrics.stringWidth(label)) / 2;
                        break;
                    case BOTTOM:
                        y = clientArea.y + clientArea.height;
                        break;
                    default:
                        return;
                }
                break;
            default:
                return;
        }
        switch (orientation) {
            case NORMAL:
                break;
            case ROTATE_90:
                gr.rotate(-M_PI_2);
                break;
            default:
                throw new IllegalStateException();
        }
        drawString(gr, x, y, w);
    }

    private void drawString(Graphics2D g, int x, int y, int w) {
        Rectangle pb = getParentWidget().getBounds();
        pb = convertSceneToLocal(getParentWidget().convertLocalToScene(pb));
        Paint background = getBackground();
        String label = getLabel();
        boolean paintAsDisabled = isPaintAsDisabled();
        Font f = getFont();
        FontMetrics fm = g.getFontMetrics(f);

        int height = fm.getHeight();
        int baseline = fm.getMaxAscent();

        int down = (pb.height - height) / 2;
        int top = pb.y + baseline + down;

        if (paintAsDisabled && background instanceof Color) {
            Color color = (Color) background;
            HtmlRenderer.renderHTML(label, g, x, top + 1, w, height, f, color.brighter(), 0, true);
            HtmlRenderer.renderHTML(label, g, x, top, w, height, f, color.darker(), 0, true);
        } else {
            Color color = (Color) appearance.tabForeground(getState());
            HtmlRenderer.renderHTML(label, g, x, top, w, height, f, color, 0, true);
        }
    }

    boolean syncText() {
        String txt = data.get().getText();
        if (!txt.equals(getLabel())) {
            setLabel(txt);
            revalidate(false);
            return true;
        }
        return false;
    }
}
