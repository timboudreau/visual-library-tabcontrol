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

import com.mastfrog.visualtabs.TabsAppearance.TabIcon;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.swing.tabcontrol.TabData;

/**
 *
 * @author Tim Boudreau
 */
final class TabWidget extends Widget implements Supplier<TabData> {

    final DynamicLabelWidget label;
    private final TabsAppearance appearance;
    private TabKind lastKind;
    private final Function<TabWidget, TabKind> kindFinder;
    final CloseButton closeButton;
    private final Function<TabWidget, TabData> func;
    private final TabIconBorder labelBorder = new TabIconBorder();
    private final AnimationTimer hoverDecorationTimer
            = new AnimationTimer(16, false, 24, true, this::onHoverAnimTick);
    private final AnimationTimer selectDecorationTimer
            = new AnimationTimer(16, false, 24, true, this::onSelectAnimTick);

    public TabWidget(Scene scene, TabData data, TabsAppearance appearance, Function<TabWidget, TabKind> kindFinder, BiConsumer<TabData, WidgetMouseEvent> onClose, Function<TabWidget, TabData> func) {
        super(scene);
        this.appearance = appearance;
        this.kindFinder = kindFinder;
        this.func = func;
        setLayout(new TabWidgetInteriorLayout(appearance.tabInternalPadding()));
        label = new DynamicLabelWidget(scene, this, appearance);
        label.setOpaque(false);
        label.setBorder(labelBorder);
        setOpaque(false);
        addChild(label);
        label.setFont(appearance.tabFont());
        label.syncText();
        closeButton = new CloseButton(scene, onClose);
        addChild(closeButton);
        lastKind = kindFinder.apply(this);
        updateBorder();
    }

    boolean isActive() {
        return ((TabScene) getScene()).isActive();
    }

    private void updateBorder() {
        setBorder(appearance.borderForKind(lastKind));
    }

    @Override
    public TabData get() {
        return func.apply(this);
    }

    static String stateString(ObjectState state) {
        StringBuilder sb = new StringBuilder("[");
        if (state.isWidgetAimed()) {
            sb.append("aimed,");
        }
        if (state.isSelected()) {
            sb.append("selected,");
        }
        if (state.isFocused()) {
            sb.append("focused,");
        }
        if (state.isHovered()) {
            sb.append("hovered,");
        }
        if (state.isObjectFocused()) {
            sb.append("objectFocused,");
        }
        if (state.isHighlighted()) {
            sb.append("highlighted,");
        }
        if (state.isObjectHovered()) {
            sb.append("objectHighlighted,");
        }
        if (state.isWidgetFocused()) {
            sb.append("widgetFocused");
        }
        if (state.isObjectHovered()) {
            sb.append("objectHovered");
        }
        return sb.append(']').toString();
    }

    boolean isInCloseButtonBounds(Point p) {
        Point scenePoint = super.convertLocalToScene(p);
        Rectangle r = closeButton.getBounds();
        if (r == null) { // closing
            return true;
        }
        r = closeButton.convertLocalToScene(r);
        return r.contains(scenePoint);
    }

    TabsAppearance appearance() {
        return appearance;
    }

    int lastIconWidth = -1;
    int lastIconHeight = -1;

    boolean sync() {
        boolean changed = label.syncText();
        if (changed) {
            label.revalidate(false);
        }
        TabData data = get();
        setToolTipText(data.getTooltip());
        TabKind newKind = kindFinder.apply(this);
        if (newKind != lastKind) {
            changed = true;
            lastKind = newKind;
            updateBorder();
        }
        closeButton.setVisible(newKind != TabKind.DRAG_PROXY && newKind != TabKind.DEFUNCT);
        Icon icon = data.getIcon();
        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();

        changed |= iconWidth != lastIconWidth || iconHeight != lastIconHeight;
        lastIconWidth = iconWidth;
        lastIconHeight = iconHeight;
        if (changed) {
            setPreferredBounds(null);
            label.setPreferredBounds(null);
            revalidate();
            label.revalidate();
        }
        return changed;
    }

    @Override
    public String toString() {
        return get().toString();
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        label.setState(state);
        if (state.isSelected()) {
            stopHoverAnimationTimer();
            if (!previousState.isSelected()) {
                lastSelectTick = 0;
                lastSelectOf = selectDecorationTimer.ticks();
                selectDecorationTimer.start();
            }
        } else if (state.isHovered()) {
            if (!previousState.isHovered() && !previousState.isSelected()) {
                lastHoverTick = 0;
                lastHoverOf = hoverDecorationTimer.ticks();
                hoverDecorationTimer.start();
            }
        } else {
            stopHoverAnimationTimer();
            stopSelectAnimationTimer();
        }
        repaint();
    }

    private void stopHoverAnimationTimer() {
        hoverDecorationTimer.stop();
        lastHoverTick = hoverDecorationTimer.ticks();
    }

    private void stopSelectAnimationTimer() {
        selectDecorationTimer.stop();
        lastSelectTick = selectDecorationTimer.ticks();
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    private Graphics2D imageGraphics;

    void withTemporaryGraphics(Graphics2D graphics, Runnable r) {
        imageGraphics = graphics;
        try {
            r.run();
        } finally {
            imageGraphics = null;
        }
    }

    @Override
    protected Graphics2D getGraphics() {
        Graphics2D result = imageGraphics == null ? super.getGraphics() : imageGraphics;
        if (imageGraphics == null && lastKind == TabKind.DRAG_PROXY) {
            result.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        }
        Object desktopHints
                = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map<?, ?>) {
            result.addRenderingHints((Map<?, ?>) desktopHints);
        }
        return result;
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        super.paintBorder();
        paintBackground(g);
        super.paintWidget();
    }

    void paintBackground(Graphics2D g) {
        Rectangle r = getClientArea();
        if (r == null || r.width == 0 || r.height == 0) {
            return;
        }
        ObjectState state = getState();
        BackgroundPainter p = appearance.tabBackgroundPainterForState(state, lastKind);

        int lastTick = state.isSelected() ? lastSelectTick : lastHoverTick;
        int lastOf = state.isSelected() ? lastSelectOf : lastHoverOf;
        p.paint(g, state, lastKind, r, lastTick, lastOf);

        if (getState().isSelected()) {
            p = appearance.selectDecorationPainter();
            Shape oldClip = g.getClip();
            g.setClip(r);
            p.paint(g, state, lastKind, r, lastSelectTick, lastSelectOf);
            g.setClip(oldClip);
        } else if (getState().isHovered()) {
            p = appearance.hoverDecorationPainter();
            Shape oldClip = g.getClip();
            g.setClip(r);
            p.paint(g, state, lastKind, r, lastHoverTick, lastHoverOf);
            g.setClip(oldClip);
        }
    }

    private int lastHoverTick;
    private int lastHoverOf;

    private void onHoverAnimTick(int tick, int of) {
        lastHoverTick = tick;
        lastHoverOf = of;
        repaintForTick();
    }

    private int lastSelectTick;
    private int lastSelectOf;

    private void onSelectAnimTick(int tick, int of) {
        lastSelectTick = tick;
        lastSelectOf = of;
        repaintForTick();
    }

    private void repaintForTick() {
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
    protected Rectangle calculateClientArea() {
        Rectangle rect = super.calculateClientArea();
        Icon icon = get().getIcon();
        rect.height = Math.max(rect.height, icon.getIconHeight());
        return rect;
    }

    private final class TabIconBorder implements Border {

        private int targetHeight() {
            Rectangle bds = getBounds();
            if (bds == null) {
                Graphics2D g = getGraphics();
                if (g == null) {
                    return 0;
                }
                if (label.getFont() != null) {
                    FontMetrics mx = g.getFontMetrics(label.getFont());
                    return mx.getHeight();
                } else {
                    return 24;
                }
            }
            return bds.height;
        }

        private int scaleFactor(int iconWidth, int iconHeight) {
            int height = targetHeight();
            if (height / 2 > iconHeight && height / 2 > iconWidth) {
                return Math.max(1, Math.max(height / iconHeight, height / iconWidth) - 1);
            }
            return 1;
        }

        private Dimension getIconDimensions() {
            Icon icon = get().getIcon();
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            int scale = scaleFactor(width, height);
            return new Dimension(width * scale, height * scale);
        }

        @Override
        public Insets getInsets() {
            Icon icon = get().getIcon();
            int width = icon.getIconWidth();
            int leftMargin = appearance.tabIconLeftMargin();
            if (width == 0) {
                return new Insets(0, leftMargin, 0, 0);
            }
            Dimension dim = getIconDimensions();
            int rightMargin = appearance.tabIconRightMargin();
            return new Insets(0, dim.width + (leftMargin * 2), 0, rightMargin);
        }

        @Override
        public void paint(Graphics2D gr, Rectangle bounds) {
            // XXX scale icon if smaller than some percentage of
            // width / height
            Icon icon = get().getIcon();
            int width = icon.getIconWidth();
            if (icon.getIconWidth() <= 0) {
                return;
            }
            double scale = scaleFactor(width, icon.getIconHeight());

            double x = bounds.x + appearance.tabIconLeftMargin();
            double y = bounds.y;
            double h = icon.getIconHeight() * scale;
            if (h < bounds.height) {
                y += (bounds.height - h) / 2;
            }
            gr.translate(x, y);
            if (scale != 1) {
                gr.scale(scale, scale);
            }
            icon.paintIcon(null, gr, 0, 0);
            if (scale != 1) {
                gr.scale(1d / scale, 1d / scale);
            }
            gr.translate(-x, -y);
        }

        @Override
        public boolean isOpaque() {
            return true;
        }
    }

    final class CloseButton extends Widget {

        CloseButton(Scene scene, BiConsumer<TabData, WidgetMouseEvent> onClick) {
            super(scene);
            getActions().addAction(new WidgetAction.Adapter() {
                @Override
                public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
                    if (!event.isPopupTrigger() && event.getClickCount() == 1) {
                        onClick.accept(get(), event);
                        return WidgetAction.State.CONSUMED;
                    }
                    return WidgetAction.State.REJECTED;
                }
            });
        }

        @Override
        protected void paintWidget() {
            if (!isEnabled()) {
                return;
            }
            Rectangle r = getClientArea();
            if (r.width == 0) {
                return;
            }
            Graphics2D gr = getGraphics();
            TabIcon icon = appearance.getCloseIcon(getState());
            if (icon != null) {
                int y = ((r.y + r.height - 1) / 2) - (icon.getIconHeight() / 2);
                icon.paintIcon(TabWidget.this.getState(), gr, 0, y);
            }
        }

        @Override
        protected Rectangle calculateClientArea() {
            if (isVisible()) {
                TabIcon icon = appearance.getCloseIcon(getState());
                return new Rectangle(0, 0, icon.getIconWidth(), icon.getIconHeight());
            } else {
                return new Rectangle();
            }
        }
    }
}
