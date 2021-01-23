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

import com.mastfrog.visualtabs.buttons.ButtonPainter;
import com.mastfrog.visualtabs.buttons.ButtonAction;
import com.mastfrog.colors.Colors;
import com.mastfrog.colors.Gradients;
import com.mastfrog.colors.GradientPainter;
import com.mastfrog.colors.RectangularGlow;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.swing.UIManager;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.model.ObjectState;
import com.mastfrog.colors.Colors.ColorSupplier;

/**
 *
 * @author Tim Boudreau
 */
public class TabsAppearance {

    private static final String TAB_UNSELECTED_FILL_DARK = "tab_unsel_fill_dark";
    private static final String TAB_UNSEL_FILL_BRIGHT_UPPER = "tab_unsel_fill_bright_upper";
    private static final String TAB_UNSEL_FILL_DARK_UPPER = "tab_unsel_fill_dark_upper";

    private static final Color dkBlue = new Color(80, 80, 138);
    private static final Color ltBlue = new Color(70, 70, 200);
    private static final Color ltBlue2 = new Color(200, 200, 255);
    private static final Color base = new Color(220, 220, 255);
    private static final Color baseDk = new Color(80, 80, 70);
    private static final Color hl = Color.ORANGE;
    private static final Color hlDark = new Color(20, 10, 80);

    static BooleanSupplier isDark = (() -> {
        // Dark theme checker misses newer versions of Nimbus
        // which leave the UIManager key value pairs it checks
        // unset
        boolean res = Colors.darkTheme().getAsBoolean();
        if (!res) {
            return res;
        }
        Color c = UIManager.getColor("controlText");
        if (c == null) {
            return true;
        }
        if (c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 0) {
            return false;
        }
        return true;
    });

    static Font defaultFont;
    private static final Gradients gradients = new Gradients();
    static Set<String> logged = new HashSet<>(50);
    private static final Supplier<Color> tabBorderLow
            = Colors.fromUIManager(new Color(92, 92, 92, 128), "controlShadow");
    private static final Supplier<Color> tabBorderHigh
            = Colors.brightestOf(new Color(192, 255, 92, 255),
                    "Table.dropLineShortColor",
                    "Tree.selectionBackground")
                    .unless(isDark, Colors.fixed(new Color(192, 255, 92, 255))
                            .darkenBy(0.5f))
                    .cache();

    static final ColorSupplier selectedBase
            = Colors.choiceOf(ltBlue2, isDark, dkBlue,
                    "Table.dropLineShortColor",
                    //                    "TabbedPane.focus",
                    "TabbedPane.borderHightlightColor",
                    "TabRenderer.selectedActivatedBackground",
                    "nb.explorer.unfocusedSelBg"
            ).darkenOrLighten(0.325f, isDark)
                    .withSaturationNoGreaterThan(0.425F)
                    //                    .withSaturationNoGreaterThan(0.275f)
                    .withBrightnessFrom(Colors.fromUIManager(Color.GRAY, "control"))
                    .unless(isDark, Colors.fromUIManager(ltBlue)).cache();

    static final ColorSupplier selectedHl
            = selectedBase
                    .withBrightnessFrom(Colors.fromUIManager(Color.GRAY, "control"))
                    .withSaturationNoGreaterThan(0.075f)
                    .darkenOrLighten(-0.325f, isDark)
                    .cache();

    static final ColorSupplier hoveredHl
            = Colors.mostSaturatedOf(hl,
                    "TabRenderer.selectedActivatedBackground",
                    "TabbedPane.highlight"
            ).withSaturationNoGreaterThan(0.3f)
                    .unless(isDark, Colors.fromUIManager(hlDark,
                            "TabbedPane.highlight").darkenBy(-0.15f))
                    .cache();

    static final ColorSupplier hoveredBase
            = Colors.fromUIManager(hlDark, isDark, hl,
                    "TabRenderer.selectedActivatedBackground",
                    "TabbedPane.focus")
                    .withBrightnessFrom(Colors.fromUIManager(Color.GRAY, "control"))
                    .darkenOrLighten(-0.225f, isDark)
                    .adjustSaturation(-0.35f, isDark)
                    .cache();

    static ColorSupplier baseColor
            = Colors.fromUIManager(hl, TAB_UNSEL_FILL_BRIGHT_UPPER,
                    TAB_UNSEL_FILL_DARK_UPPER, TAB_UNSELECTED_FILL_DARK, "control")
                    .brightenBy(0.125f).cache();

    static ColorSupplier baseHl = Colors.fromUIManager(base, "control").darkenBy(0.25f);

    private static final ColorSupplier miniHighlight
            = selectedBase.withSaturation(0.75f)
                    .rotatingHueBy(-0.0375f)
                    .withBrightnessNoGreaterThanThatOf(selectedHl).cache();

//            Colors.fromUIManager(Color.BLUE.brighter(), "TabbedPane.focus");
    private static final ColorSupplier miniHighlightEnd = miniHighlight.withAlpha(0);

    static ColorSupplier selectedForeground
            = miniHighlight.darkerOf(selectedBase).contrasting()
                    .withSaturationNoGreaterThan(0.25f);

    private Supplier<Color> background = Colors.fromUIManager(Color.GRAY, "control");

    private static ColorSupplier defaultGlowDark
            = selectedHl.withSaturation(0.475f)
                    .withBrightness(0.98f)
                    .withAlpha(167)
                    .cache()
                    .unless(isDark, Colors.fixed(new Color(120, 120, 225, 172)));

    private Supplier<Color> glowDark = defaultGlowDark;

    public RectangularGlow glow() {
        return new RectangularGlow(glowDark(), glowLight(), gradients(), glowWidth());
    }

    private static final Color WHITE_FULL_ALPHA = new Color(255, 255, 255, 0);

    private Supplier<Color> glowLight
            = Colors.fixed(WHITE_FULL_ALPHA).unless(isDark, Colors.toUIColorSupplier(background).withAlpha(0));
    private static final ColorSupplier directionFallback
            = Colors.toUIColorSupplier(defaultGlowDark.withSaturation(0.9f)).withAlpha(0).cache();

    private BackgroundPainter unselectedHoveredTabPainter = TabsAppearance::defaultPaintUnselectedHovered;
    private BackgroundPainter selectedTabPainter = TabsAppearance::defaultPaintSelected;
    private BackgroundPainter unselectedTabPainter = TabsAppearance::defaultPaintUnselected;

    private static ColorSupplier textFallback = Colors.fixed(Color.BLACK).unless(isDark, Colors.fixed(Color.WHITE));
    private static ColorSupplier defaultUnselectedForeground
            = Colors.fromUIManager(textFallback,
                    "controlText", "textText", "textInactiveText")
                    .unless(isDark, baseHl.contrasting().withBrightnessNoGreaterThan(0.875f)
                    );
//
    private Function<ObjectState, Paint> tabForeground = state -> {
        return state.isSelected() ? selectedForeground.get() : defaultUnselectedForeground.get();
    };

    private Supplier<Font> font = TabsAppearance::defaultFont;
    private int fontSize = -1;
    private int tabInnerLeftMargin = 5;
    private int tabsMargin = 5;
    private int tabInnerRightMargin = 5;
    private int closeIconSize = 9;
    private int glowWidth = 9;
    private int tabInternalPadding = 6;
    private int panTrayRightInset = 24;
    private int tabsInnerSpacing = 4;
    private int panTrayLeftInset = 24;
    boolean dragDropEnabled = true;
    private IntSupplier dragThreasholdDistance;
    private ButtonPainter buttonPainter = TabsAppearance::defaultPaintSideButton;
    private final EnumMap<TabKind, Border> borderForKind = new EnumMap(TabKind.class);
    private final Map<ButtonState, TabIcon> closeIcons = new EnumMap<>(ButtonState.class);

    private Supplier<Color> selectionDirectionIndicatorColor = directionFallback;

    private BiFunction<Graphics2D, Rectangle, GradientPainter> tabBorderColorBottom = (g, r) -> {
        return gradients().linear(g, r.x, r.y,
                tabBorderHigh.get(), r.x, r.y + r.height, tabBorderLow.get());
    };
    private BiFunction<Graphics2D, Rectangle, GradientPainter> tabBorderColor = (g, r) -> {
        return gradients().linear(g, r.x, r.y,
                tabBorderLow.get(), r.x, r.y + r.height, tabBorderHigh.get());
    };

    private static Color base(ObjectState state) {
        if (state.isSelected()) {
            return selectedBase.get();
        }
        if (state.isWidgetHovered() || state.isHovered() || state.isWidgetAimed()) {
            return hoveredBase.get();
        }
        return baseColor.get();
    }

    private static Color hl(ObjectState state) {
        if (state.isSelected()) {
            return selectedHl.get();
        }
        if (state.isWidgetHovered() || state.isHovered() || state.isWidgetAimed()) {
            return hoveredHl.get();
        }
        return baseHl.get();
    }

    static int bottom(int y, int height) {
        return y + height - (height / 3);
    }

    static void defaultPaintUnselected(Graphics2D g, ObjectState state, TabKind kind, Rectangle r, int animTick, int of) {
        if (r.width <= 1 || r.height <= 1) {
            return;
        }

        gradients.linear(g, r.x, r.y, base(state), r.x, bottom(r.y, r.height), hl(state))
                .fill(g, r);

    }

    static void defaultPaintUnselectedHovered(Graphics2D g, ObjectState state, TabKind kind, Rectangle r, int animTick, int of) {
        if (r.width <= 1 || r.height <= 1) {
            return;
        }
        Color base = base(state);
        Color highlight = hl(state);
        if (of != 0 && animTick < of) {
            float halfOf = (float) of / 2f;
            float pct = (float) animTick / halfOf;

            if (pct <= 1f) {
                Color unBase = base(ObjectState.createNormal());
                Color unHighlight = hl(ObjectState.createNormal());
                base = Colors.between(unBase, base, pct);
                highlight = Colors.between(unHighlight, highlight, pct);
            }
        }
        gradients.linear(g, r.x, r.y, base, r.x, r.y + r.height / 2, highlight)
                .fill(g, r);
    }

    static void defaultPaintSelected(Graphics2D g, ObjectState state, TabKind kind, Rectangle r, int animTick, int of) {
        if (r.width <= 1 || r.height <= 1) {
            return;
        }
        if (kind == TabKind.DRAG_PROXY) {
            state = state.deriveSelected(true);
        }
        Color base = base(state);
        Color highlight = hl(state);
        float halfOf = of; //(float) of / 2f;
        float pct = (float) animTick / halfOf;
        if (of != 0 && animTick < of) {
            if (pct <= 1f) {
                Color unBase = base(ObjectState.createNormal());
                Color unHighlight = hl(ObjectState.createNormal());
                base = Colors.between(unBase, base, pct);
                highlight = Colors.between(unHighlight, highlight, pct);
            }
        }
        gradients.linear(g, r.x, r.y, base, r.x, bottom(r.y, r.height), highlight)
                .fill(g, r);

        // for centering layout
//        g.setColor(Color.black);
//        g.drawLine(r.x, r.y + (r.height/2), r.width, r.y + (r.height / 2));
//        if (animTick == of) {
        int ybase = (r.height / 6);
        int hbase = r.height / 2;
        int alph = Math.max(0, Math.min(255, (int) (pct * 255)));
        Color start = miniHighlight.withAlpha(alph / 2).get();
        Color end = miniHighlightEnd.withAlpha(0).get();

        gradients.linear(g, r.x, r.y + ybase, end, r.x, r.y + ybase + hbase, start)
                .fill(g, r.x, r.y + ybase, r.width, hbase);

        gradients.linear(g, r.x, r.y + hbase + ybase, start, r.x, r.y + ybase + hbase + hbase,
                end)
                .fill(g, r.x, r.y + ybase + hbase, r.width, hbase);
//        }
    }

    BackgroundPainter hoverDecoration = TabsAppearance::defaultPaintHoverDecoration;

    public BackgroundPainter hoverDecorationPainter() {
        return hoverDecoration;
    }

    public TabsAppearance setHoverDecoration(BackgroundPainter hoverDecoration) {
        this.hoverDecoration = hoverDecoration;
        return this;
    }

    static void defaultPaintSelectDecoration(Graphics2D g, ObjectState state, TabKind kind, Rectangle r, int animTick, int of) {
        double pctDone = of <= 0 ? 1 : (double) animTick / (double) of;
        Color top = selectedBase.rotatingHueBy((float) pctDone)
                .withAlpha(128)
                .get();
        int h = (int) (pctDone * (r.height));
        r.y = r.y + r.height - h;
        r.height = h;
        Arc2D.Double arc = new Arc2D.Double(r, 0, 180, Arc2D.PIE);
        g.setColor(top);
        g.fill(arc);
    }

    BackgroundPainter selectDecoration = TabsAppearance::defaultPaintSelectDecoration;

    public BackgroundPainter selectDecorationPainter() {
        return selectDecoration;
    }

    public TabsAppearance setSelectDecoration(BackgroundPainter selectDecoration) {
        this.selectDecoration = selectDecoration;
        return this;
    }

    static void defaultPaintHoverDecoration(Graphics2D g, ObjectState state, TabKind kind, Rectangle r, int animTick, int of) {
        float pctDone = of <= 0 ? 1 : (float) animTick / (float) of;
        Color top = selectedBase.rotatingHueBy((float) pctDone)
                .withAlpha(isDark.getAsBoolean() ? 128 : 220)
                .get();
        int w = Math.max(10, (int) ((pctDone) * r.width));
        int x = r.x + w;
        int h = (int) (pctDone * (r.height));
        int y = (r.height - h) / 2;
        g.setColor(top);
        g.fillOval(x, y, w, h);
    }

    public static Color alpha(int alpha, Color c) {
        if (c.getAlpha() != alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
        return c;
    }

    static Font defaultFont() {
        if (defaultFont != null) {
            return defaultFont;
        }
        Font f = UIManager.getFont("controlFont");
        if (f == null) {
            f = UIManager.getFont("windowTitleFont");
        }
        if (f == null) {
            String nm = System.getProperty("uiFontName", "sans-serif");
            f = new Font(nm, Font.PLAIN, 14);
        }
        String val = System.getProperty("uiFontSize");
        if (val != null && Pattern.compile("\\d+").matcher(val).matches()) {
            int size = Integer.parseInt(val);
            f = f.deriveFont((float) size);
        }
        return defaultFont = f;
    }

    public TabsAppearance() {
        borderForKind.put(TabKind.BETWEEN, new DefaultMiddleBorder());
        borderForKind.put(TabKind.LEFT_EDGE, new DefaultMiddleBorder());
        borderForKind.put(TabKind.RIGHT_EDGE, BorderFactory.createEmptyBorder());
        borderForKind.put(TabKind.SINGLE, BorderFactory.createEmptyBorder());
        borderForKind.put(TabKind.DEFUNCT, new DefaultMiddleBorder());
        borderForKind.put(TabKind.DRAG_PROXY, BorderFactory.createRoundedBorder(5, 5, 1, 1, Color.gray, Color.gray));
        for (ButtonState b : ButtonState.values()) {
            closeIcons.put(b, new CloseIcon(b));
        }
    }

    public ButtonPainter buttonPainter() {
        return buttonPainter;
    }

    public TabsAppearance setButtonPainter(ButtonPainter painter) {
        this.buttonPainter = painter;
        return this;
    }

    private static final ColorSupplier buttonOutline = Colors.fromUIManager(Color.DARK_GRAY, "controlDkShadow");
    private static final ColorSupplier buttonOuterNormal
            = defaultGlowDark
                    .brightenBy(0.3725f).cache();
    private static final ColorSupplier buttonCenterNormal = buttonOuterNormal.darkenBy(0.125f).cache();
    private static final ColorSupplier buttonCenterHovered
            = Colors.fromUIManager(ltBlue, "TabRenderer.selectedActivatedBackground",
                    "TabbedPane.focus").cache();
    private static final ColorSupplier buttonOuterHovered = buttonCenterHovered.darkenBy(0.1f).cache();
    private static final ColorSupplier buttonOuterPressed
            = Colors.fromUIManager(ltBlue, "TabRenderer.selectedActivatedBackground", "TabbedPane.focus").cache();
    private static final ColorSupplier buttonCenterPressed = buttonOuterPressed.darkenBy(0.1f).cache();
    private static final ColorSupplier buttonOuterDisabled
            = Colors.fromUIManager(Color.GRAY, "control").brightenBy(0.1f).cache();
    private static final ColorSupplier buttonCenterDisabled
            = buttonOuterPressed.darkenBy(0.15f).cache();
    private static final float defaultSideButtonStrokeWidth = 0.625f;

    static void defaultPaintSideButton(Graphics2D into, Rectangle bounds, Shape buttonShape, ButtonAction type, ObjectState state, boolean enabled) {
        Supplier<Color> center, outer;
        if (!enabled) {
            center = buttonCenterDisabled;
            outer = buttonOuterDisabled;
        } else if (state.isSelected()) {
            center = buttonCenterPressed;
            outer = buttonOuterPressed;
        } else if (state.isHovered()) {
            center = buttonCenterHovered;
            outer = buttonOuterHovered;
        } else {
            center = buttonCenterNormal;
            outer = buttonOuterNormal;
        }

        Rectangle bsBounds = buttonShape.getBounds();
        gradients.radial(into, bsBounds.x, bsBounds.y, center.get(), outer.get(), bsBounds.width)
                .fillShape(into, buttonShape);

        into.setColor(buttonOutline.get());
        Stroke old = into.getStroke();
        into.setStroke(new BasicStroke(defaultSideButtonStrokeWidth,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        into.draw(buttonShape);
        into.setStroke(old);
    }

    public Gradients gradients() {
        return gradients;
    }

    public int panTrayLeftInset() {
        return panTrayLeftInset;
    }

    public int panTrayRightInset() {
        return panTrayRightInset;
    }

    public TabsAppearance setPanTrayLeftInset(int val) {
        assert val >= 0;
        panTrayLeftInset = val;
        return this;
    }

    public TabsAppearance setPanTrayRightInset(int val) {
        assert val >= 0;
        panTrayRightInset = val;
        return this;
    }

    public TabsAppearance setPanTrayLeftAndRightInsets(int val) {
        assert val >= 0;
        panTrayRightInset = val;
        panTrayLeftInset = val;
        return this;
    }

    public TabsAppearance setBackground(Supplier<Color> bg) {
        background = bg;
        return this;
    }

    public Color getBackground() {
        return background.get();
    }

    public Color glowDark() {
        return glowDark.get();
    }

    public Color glowLight() {
        return glowLight.get();
    }

    public TabsAppearance setGlowDark(Supplier<Color> c) {
        glowDark = c;
        return this;
    }

    public TabsAppearance setGlowLight(Supplier<Color> c) {
        glowLight = c;
        return this;
    }

    public TabsAppearance setGlowWidth(int val) {
        glowWidth = val;
        return this;
    }

    public int glowWidth() {
        return glowWidth;
    }

    public int dragThresholdDistance() {
        return dragThreasholdDistance == null ? defaultDragThresholdDistance()
                : dragThreasholdDistance.getAsInt();
    }

    public TabsAppearance setDragThresholdDistance(IntSupplier supp) {
        this.dragThreasholdDistance = supp;
        return this;
    }

    int defaultDragThresholdDistance() {
        return 24;
    }

    public int tabIconLeftMargin() {
        return tabInnerLeftMargin;
    }

    public int tabIconRightMargin() {
        return tabInnerRightMargin;
    }

    public TabsAppearance disableInternalDragDropSupport() {
        dragDropEnabled = false;
        return this;
    }

    public boolean isInternalDragDropSupportEnabled() {
        return dragDropEnabled;
    }

    public TabsAppearance setInnerRightMargin(int val) {
        this.tabInnerRightMargin = val;
        return this;
    }

    public TabsAppearance setInnerLeftMargin(int val) {
        this.tabInnerLeftMargin = val;
        return this;
    }

    public TabsAppearance setTabsInnerSpacing(int val) {
        this.tabsInnerSpacing = val;
        return this;
    }

    public int tabsInnerSpacing() {
        return tabsInnerSpacing;
    }

    public Color selectionDirectionIndicatorColor() {
        return selectionDirectionIndicatorColor.get();
    }

    public TabsAppearance setSelectionDirectionIndicatorColor(Supplier<Color> c) {
        selectionDirectionIndicatorColor = c;
        return this;
    }

    public Font tabFont() {
        Font result = this.font.get();
        if (fontSize > 0) {
            result = result.deriveFont((float) fontSize);
        }
        return result;
    }

    public TabsAppearance setFont(Supplier<Font> supp) {
        this.font = supp;
        return this;
    }

    public TabsAppearance forceFontSize(int amt) {
        fontSize = amt;
        return this;
    }

    public BackgroundPainter tabBackgroundPainterForState(ObjectState state, TabKind kind) {
        if (state.isSelected() || kind == TabKind.DRAG_PROXY) {
            return selectedTabPainter;
        } else {
            if (state.isHovered()) {
                return unselectedHoveredTabPainter;
            } else {
                return unselectedTabPainter;
            }
        }
    }

    public TabsAppearance setUnselectedTabPainter(BackgroundPainter p) {
        unselectedTabPainter = p;
        return this;
    }

    public TabsAppearance setUnselectedHoveredTabPainter(BackgroundPainter p) {
        unselectedHoveredTabPainter = p;
        return this;
    }

    public TabsAppearance setSelectedHoveredTabPainter(BackgroundPainter p) {
        selectedTabPainter = p;
        return this;
    }

    public int tabInternalPadding() {
        return tabInternalPadding;
    }

    public TabsAppearance setTabInternalPadding(int val) {
        if (val < 0) {
            throw new IllegalArgumentException("< 0: " + val);
        }
        tabInternalPadding = val;
        return this;
    }

    public Border borderForKind(TabKind kind) {
        return borderForKind.get(kind);
    }

    public GradientPainter tabBorderColor(Graphics2D g, Rectangle rect) {
        return tabBorderColor.apply(g, rect);
    }

    public TabsAppearance setTabBorderColor(BiFunction<Graphics2D, Rectangle, GradientPainter> color) {
        this.tabBorderColor = color;
        return this;
    }

    public GradientPainter tabBorderColorBottom(Graphics2D g, Rectangle rect) {
        return tabBorderColorBottom.apply(g, rect);
    }

    public TabsAppearance setTabBorderColorBottom(BiFunction<Graphics2D, Rectangle, GradientPainter> color) {
        this.tabBorderColorBottom = color;
        return this;
    }

    public int tabsMargin() {
        return tabsMargin;
    }

    public TabsAppearance setTabsMargin(int val) {
        this.tabsMargin = val;
        return this;
    }

    public TabsAppearance setCloseIcon(ButtonState state, TabIcon icon) {
        closeIcons.put(state, icon);
        return this;
    }

    public TabIcon getCloseIcon(ObjectState state) {
        return closeIcons.get(ButtonState.forObjectState(state));
    }

    public int closeIconSize() {
        return closeIconSize;
    }

    public TabsAppearance setCloseIconSize(int val) {
        closeIconSize = val;
        return this;
    }

    public Paint tabForeground(ObjectState state) {
        return tabForeground.apply(state);
    }

    class DefaultMiddleBorder implements Border {

        @Override
        public Insets getInsets() {
            return new Insets(0, 0, 0, 1);
        }

        @Override
        public void paint(Graphics2D gr, Rectangle bounds) {
            int h = (bounds.height - 4) / 2;
            Rectangle topBounds = new Rectangle(bounds.x + bounds.width - 1, bounds.y + 2, 1, h);
            tabBorderColor(gr, topBounds).fill(gr, topBounds);
            Rectangle bottomBounds = new Rectangle(bounds.x + bounds.width - 1, bounds.y + 2 + h, 1, h);
            tabBorderColorBottom(gr,
                    bottomBounds).fill(gr, bottomBounds);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    enum ButtonState {
        NORMAL,
        HOVERED,
        PRESSED;

        static ButtonState forObjectState(ObjectState state) {
            if (state.isSelected()) {
                return PRESSED;
            }
            if (state.isHovered()) {
                return HOVERED;
            }
            return NORMAL;
        }
    }

    static final Supplier<Color> closeIconNormalForeground
            = Colors.fromUIManager(Color.GRAY, "controlDkShadow")
                    .unless(isDark, Colors.fromUIManager(Color.GRAY,
                            "control").brightenBy(0.2f)).cache();

    public interface TabIcon {

        public int getIconWidth();

        public int getIconHeight();

        public void paintIcon(ObjectState tabState, Graphics2D gr, int x, int y);
    }

    final class CloseIcon implements TabIcon {

        private final ButtonState state;

        public CloseIcon(ButtonState state) {
            this.state = state;
        }

        @Override
        public void paintIcon(ObjectState tabState, Graphics2D gr, int x, int y) {
            int endX = x + getIconWidth();
            int endY = y + getIconHeight();
            Paint fillColor = null;
            Paint outlineColor = null;
            Paint xColor = null;
            if (tabState.isSelected()) {
                switch (state) {
                    case NORMAL:
                        xColor = TabsAppearance.selectedHl.darkenOrLighten(-0.375f, isDark).get();
                        break;
                    case HOVERED:
                        xColor = TabsAppearance.selectedForeground.darkenOrLighten(0.375f, isDark).get();
                        break;
                    case PRESSED:
                        xColor = TabsAppearance.selectedHl.darkenOrLighten(0.375f, isDark).get();
                        break;
                }
            } else if (tabState.isHovered()) {
                switch (state) {
                    case NORMAL:
                        xColor = TabsAppearance.selectedHl.darkenOrLighten(-0.375f, isDark).get();
                        break;
                    case HOVERED:
                        xColor = TabsAppearance.selectedForeground.darkenOrLighten(0.375f, isDark).get();
                        break;
                    case PRESSED:
                        xColor = TabsAppearance.selectedHl.darkenOrLighten(0.375f, isDark).get();
                        break;
                }
            } else {
                switch (state) {
                    case NORMAL:
                        xColor = closeIconNormalForeground.get();
//                    xColor = TabsAppearance.this.glowDark().brighter();
                        break;
                    case HOVERED:
                        xColor = glowDark();
//                    fillColor =
                        break;
                    case PRESSED:
                        xColor = closeIconNormalForeground.get();
                        fillColor = Color.BLACK;
                        break;
                }
            }
            if (fillColor != null) {
                gr.setPaint(fillColor);
                gr.fillRect(x, y, endX - x, endY - y);
            }
            if (outlineColor != null) {
                gr.setPaint(outlineColor);
                gr.drawRect(x, y, endX - x, endY - y);
            }
            if (xColor != null) {
                gr.setPaint(xColor);
                gr.setStroke(new BasicStroke(1.625f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gr.drawLine(x + 1, y + 1, endX - 1, endY - 1);
                gr.drawLine(x + 1, endY - 1, endX - 1, y + 1);
            }
        }

        @Override
        public int getIconWidth() {
            return closeIconSize();
        }

        @Override
        public int getIconHeight() {
            return closeIconSize();
        }

    }

}
