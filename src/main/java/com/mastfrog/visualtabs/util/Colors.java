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
package com.mastfrog.visualtabs.util;

import static com.mastfrog.visualtabs.util.GradientUtils.colorToString;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 *
 * @author Tim Boudreau
 */
public class Colors {

    public static UIColorSupplier fixed(Color color) {
        return new FixedColorSupplier(color);
    }

    public static UIColorSupplier fromUIManager(Color fallback, String... names) {
        return new UIManagerColor(names, fallback).cache();
    }

    public static UIColorSupplier fromUIManager(Color ifTruefFallabck, BooleanSupplier supp, Color ifFalseFallback, String... names) {
        UIColorSupplier fallback = Colors.fixed(ifTruefFallabck).unless(supp, Colors.fixed(ifFalseFallback));
        return new UIManagerColor(names, fallback).cache();
    }

    public static UIColorSupplier fromUIManager(Supplier<Color> fallback, String... names) {
        return new UIManagerColor(names, fallback).cache();
    }

    public static UIColorSupplier mostSaturatedOf(Color ifTrue, BooleanSupplier test, Color ifFalse, String... names) {
        UIColorSupplier fallback = Colors.fixed(ifTrue).unless(test, Colors.fixed(ifFalse));
        return new MostSaturatedOf(fallback, names, 1);
    }

    public static UIColorSupplier mostSaturatedOf(Color fallback, String... names) {
        return new MostSaturatedOf(fallback, names, 1);
    }

    public static UIColorSupplier brightestOf(Color ifTrue, BooleanSupplier test, Color ifFalse, String... names) {
        UIColorSupplier fallback = Colors.fixed(ifTrue).unless(test, Colors.fixed(ifFalse));
        return new MostSaturatedOf(fallback, names, 0);
    }

    public static UIColorSupplier brightestOf(Color fallback, String... names) {
        return new MostSaturatedOf(fallback, names, 0);
    }

    public static UIColorSupplier brightestOrDarkestOf(Color fallback,
            BooleanSupplier darkLight, String... names) {
        return new MostSaturatedOf(new FixedColorSupplier(fallback), names, 0, darkLight);
    }

    public static UIColorSupplier toUIColorSupplier(Supplier<Color> supp) {
        return Colors.supplier(supp);
    }

    public static Color between(Color a, Color b, float tick, float of) {
        float pct = tick / of;
        return between(a, b, pct);
    }

    public static Color between(Color a, Color b, float percentage) {
        if (percentage <= 0F) {
            return a;
        } else if (percentage >= 1F) {
            return b;
        }
        int ar = a.getRed();
        int ag = a.getGreen();
        int ab = a.getBlue();
        int aa = a.getAlpha();
        int br = b.getRed();
        int bg = b.getGreen();
        int bb = b.getBlue();
        int ba = b.getAlpha();
        return new Color(spread(ar, br, percentage), spread(ag, bg, percentage),
                spread(ab, bb, percentage), spread(aa, ba, percentage));
    }

    private static int spread(int aVal, int bVal, float percentage) {
        if (aVal == bVal) {
            return aVal;
        }
        int dist = (int) ((bVal - aVal) * percentage);
        return Math.max(0, Math.min(255, aVal + dist));
    }

    public interface FloatSupplier {

        public float getAsFloat();
    }

    public static void listUIManager() {
        List<NC> ncs = new ArrayList<>();
        Set<Object> keys = new HashSet<>(UIManager.getDefaults().keySet());
        keys.addAll(UIManager.getLookAndFeelDefaults().keySet());
        for (Object o : keys) {
            Object o1 = UIManager.getColor(o);
            if (o1 instanceof Color) {
                NC nc = new NC(o.toString(), (Color) o1);
                ncs.add(nc);
            }
        }
        Collections.sort(ncs);
        for (NC nc : ncs) {
            if (nc.saturation() == 0f) {
                continue;
            }
            System.out.println(" - " + nc);
        }
    }

    static class NC extends Color implements Comparable<NC> {

        private final String nm;

        NC(String nm, Color c) {
            super(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
            this.nm = nm;
        }

        @Override
        public String toString() {
            return nm + ": \t" + GradientUtils.colorToString(this) + " sat " + saturation()
                    + " bri " + brightness();
        }

        private float saturation() {
            float[] f = new float[3];
            Color.RGBtoHSB(getRed(), getGreen(), getBlue(), f);
            return f[1];
        }

        private float brightness() {
            float[] f = new float[3];
            Color.RGBtoHSB(getRed(), getGreen(), getBlue(), f);
            return f[2];
        }

        @Override
        public int compareTo(NC o) {
            float a = saturation();
            float b = o.saturation();
            if (a == b) {
                return nm.compareToIgnoreCase(o.nm);
            }
            return a > b ? -1 : a < b ? 1 : 0;
        }
    }

    public static BooleanSupplier darkTheme() {
        return new DarkThemeDetector();
    }

    public static BooleanSupplier lightTheme() {
        BooleanSupplier supp = darkTheme();
        return () -> !supp.getAsBoolean();
    }

    static class DarkThemeDetector implements BooleanSupplier {

        int value;

        @Override
        public boolean getAsBoolean() {
            if (value != 0) {
                return value == 1;
            }

            if ("Darcula".equals(UIManager.getLookAndFeel().getID())) {
                value = 1;
                return true;
            }

            Color col = UIManager.getColor("textText");
            if (col != null && Color.BLACK.equals(col)) {
                value = -1;
                return false;
            } else if (col != null && Color.WHITE.equals(col)) {
                value = 1;
                return true;
            }
            UIColorSupplier a = Colors.fromUIManager(Color.RED, "textText", "Label.text", "activeCaption", "Menu.foreground");
            UIColorSupplier b = Colors.fromUIManager(Color.RED, "text", "textBackground", "Table.textForeground");

            Color aCol = a.get();
            Color bCol = b.get();
            if (!Color.RED.equals(aCol) && !Color.RED.equals(bCol) && !aCol.equals(bCol)) {
                if (isBrighter(aCol, bCol)) {
                    value = -1;
                } else {
                    value = 1;
                }
                return value == 1;
            }
            Color fallback = new JTextField().getBackground();
            if (fallback != null && !Color.GRAY.equals(fallback)) {
                value = Colors.isBrighter(fallback, Color.GRAY) ? 1 : -1;
                return value == 1;
            }
            return false;
        }

    }

    static final class MostSaturatedOf implements UIColorSupplier, Comparator<Color> {

        private final Supplier<Color> fallback;
        private final String[] names;
        private int component;
        private final BooleanSupplier supp;

        public MostSaturatedOf(Color fallback, String[] names, int component) {
            this(new FixedColorSupplier(fallback), names, component);
            this.component = component;
        }

        public MostSaturatedOf(Supplier<Color> fallback, String[] names, int component, BooleanSupplier supp) {
            this.fallback = fallback;
            this.names = names;
            this.component = component;
            this.supp = supp;

        }

        public MostSaturatedOf(Supplier<Color> fallback, String[] names, int component) {
            this(fallback, names, component, null);
        }

        @Override
        public Color get() {
            List<Color> all = new ArrayList<>(names.length);
            for (String nm : names) {
                Color c = UIManager.getColor(nm);
                if (c != null) {
                    all.add(c);
                }
            }
            if (all.isEmpty()) {
                return fallback.get();
            }
            Collections.sort(all, this);
            if (supp == null || !supp.getAsBoolean()) {
                return all.get(0);
            } else {
                return all.get(all.size() - 1);
            }
        }

        @Override
        public int compare(Color o1, Color o2) {
            float[] ahsb = new float[3];
            float[] bhsb = new float[3];
            Color.RGBtoHSB(o1.getRed(), o1.getGreen(), o1.getBlue(), ahsb);
            Color.RGBtoHSB(o2.getRed(), o2.getGreen(), o2.getBlue(), bhsb);
            if (Arrays.equals(ahsb, bhsb)) {
                return 0;
            }
            float[] best;
            if (ahsb[component] > bhsb[component]) {
                // a is best
                if (ahsb[0] < 0.25f && bhsb[0] >= 0.25f) {
                    if (bhsb[component] > 0.1f) {
                        best = bhsb;
                    } else {
                        best = ahsb;
                    }
                } else {
                    best = ahsb;
                }
            } else {
                if (bhsb[0] < 0.25f && ahsb[0] >= 0.25f) {
                    if (ahsb[component] > 0.1f) {
                        best = ahsb;
                    } else {
                        best = bhsb;
                    }
                } else {
                    best = ahsb;
                }
            }
            System.out.println("Best of " + o1 + " " + o2 + " is " + (best == ahsb ? o1 : o2));
            return best == ahsb ? -1 : 1;
        }
    }

    static final class HSBChanger implements UIColorSupplier {

        private final UIColorSupplier orig;
        private final FloatSupplier val;
        private final int component;

        HSBChanger(UIColorSupplier orig, FloatSupplier val, int component) {
            this.orig = orig;
            this.val = val;
            this.component = component;
        }

        HSBChanger(UIColorSupplier orig, float val, int component) {
            this(orig, () -> val, component);
        }

        @Override
        public Color get() {
            Color value = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), hsb);
            hsb[component] = this.val.getAsFloat();
            Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            if (value.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), value.getAlpha());
            }
            return result;
        }
    }

    static final class Tinter implements UIColorSupplier {

        private final UIColorSupplier orig;
        private final Supplier<Color> tintSupplier;
        private final FloatSupplier percentage;

        public Tinter(UIColorSupplier orig, Supplier<Color> tintSupplier, float pct) {
            this(orig, tintSupplier, () -> pct);
        }

        public Tinter(UIColorSupplier orig, Supplier<Color> tintSupplier, FloatSupplier percentage) {
            this.orig = orig;
            this.tintSupplier = tintSupplier;
            this.percentage = percentage;
        }

        @Override
        public Color get() {
            Color c = orig.get();
            Color tint = tintSupplier.get();
            float[] cHsb = new float[3];
            float[] tintHsb = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), cHsb);
            Color.RGBtoHSB(tint.getRed(), tint.getGreen(), tint.getBlue(), tintHsb);
            float pct = percentage.getAsFloat();
            cHsb[0] = tintHsb[0];
            float sat = tintHsb[1] * pct;
            if (cHsb[1] < sat) {
                cHsb[1] = sat;
            }
            Color result = new Color(Color.HSBtoRGB(cHsb[0], cHsb[1], cHsb[2]));
            if (c.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), c.getAlpha());
            }
            return result;
        }
    }

    static final class HSBAdjuster implements UIColorSupplier {

        private final UIColorSupplier orig;
        private final FloatSupplier val;
        private final int component;

        HSBAdjuster(UIColorSupplier orig, FloatSupplier val, int component) {
            this.orig = orig;
            this.val = val;
            this.component = component;
        }

        HSBAdjuster(UIColorSupplier orig, float val, int component) {
            this(orig, () -> val, component);
        }

        @Override
        public Color get() {
            Color value = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), hsb);
            hsb[component] = Math.max(0f, Math.min(1f, hsb[component] + this.val.getAsFloat()));
            Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            if (value.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), value.getAlpha());
            }
            return result;
        }
    }

    static final class HSBLimit implements UIColorSupplier {

        private final UIColorSupplier orig;
        private final FloatSupplier val;
        private final int component;
        private final boolean noLessThan;

        HSBLimit(UIColorSupplier orig, FloatSupplier val, int component, boolean noLessThan) {
            this.orig = orig;
            this.val = val;
            this.component = component;
            this.noLessThan = noLessThan;
        }

        HSBLimit(UIColorSupplier orig, float val, int component, boolean noLessThan) {
            this(orig, () -> val, component, noLessThan);
        }

        @Override
        public Color get() {
            Color value = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), hsb);
            float v = val.getAsFloat();
            boolean changed = false;
            if (noLessThan) {
                if (hsb[component] < v) {
                    hsb[component] = v;
                    changed = true;
                }
            } else {
                if (hsb[component] > v) {
                    hsb[component] = v;
                    changed = true;
                }
            }
            if (!changed) {
                return value;
            }
//            hsb[component] = Math.max(0f, Math.min(1f, hsb[component] + this.val.getAsFloat()));
            Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            if (value.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), value.getAlpha());
            }
            return result;
        }
    }

    static final class WithRGB implements UIColorSupplier {

        private final int component;
        private final Supplier<Color> orig;
        private final int value;

        public WithRGB(int component, Supplier<Color> orig, int value) {
            assert component >= 0 && component < 4;
            this.component = component;
            this.orig = orig;
            this.value = value;
        }

        @Override
        public Color get() {
            Color c = orig.get();
            switch (component) {
                case 0:
                    return new Color(value, c.getGreen(), c.getBlue(), c.getAlpha());
                case 1:
                    return new Color(c.getRed(), value, c.getBlue(), c.getAlpha());
                case 2:
                    return new Color(c.getRed(), c.getGreen(), value, c.getAlpha());
                case 3:
                    return new Color(c.getRed(), c.getGreen(), c.getBlue(), value);
                default:
                    throw new AssertionError(component);
            }
        }
    }

    static final class UIManagerColor implements UIColorSupplier {

        private final String[] names;
        private final Supplier<Color> fallback;

        public UIManagerColor(String[] names, Color fallback) {
            this(names, new FixedColorSupplier(fallback));
        }

        public UIManagerColor(String[] names, Supplier<Color> fallback) {
            this.names = names;
            this.fallback = fallback;
        }

        @Override
        public Color get() {
            for (String n : names) {
                Color result = UIManager.getColor(n);
                if (result != null) {
                    return result;
                }
            }
            return fallback.get();
        }
    }

    static final class CachingColorSupplier implements UIColorSupplier {

        private Supplier<Color> color;
        private Color value;

        public CachingColorSupplier(Supplier<Color> color) {
            this.color = color;
        }

        @Override
        public Color get() {
            if (value == null) {
                value = color.get();
                if (value != null) {
                    color = null;
                }
            }
            return value;
        }

        public String toString() {
            return colorToString(get()) + " sat=" + saturation() + " bri " + brightness();
        }
    }

    static final class HsbExtractor implements FloatSupplier {

        private final Supplier<Color> orig;
        private final int component;

        public HsbExtractor(Supplier<Color> orig, int component) {
            this.orig = orig;
            this.component = component;
        }

        @Override
        public float getAsFloat() {
            Color c = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            return hsb[component];
        }
    }

    static final class FixedColorSupplier implements UIColorSupplier {

        private final Color color;

        public FixedColorSupplier(Color color) {
            this.color = color;
        }

        @Override
        public Color get() {
            return color;
        }
    }

    static final class ContrastingColorSupplier implements UIColorSupplier {

        private final Supplier<Color> orig;

        public ContrastingColorSupplier(Supplier<Color> orig) {
            this.orig = orig;
        }

        @Override
        public Color get() {
            Color c = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            float bri = hsb[2];
            if (bri > 0.45f && bri < 0.575f) {
                if (bri > 0.45f) {
                    hsb[2] = 0.05f;
                } else {
                    hsb[2] = 0.95f;
                }
                hsb[0] = hsb[0] < 0.5f ? 1f - hsb[0] : 0.5f - hsb[0];
            } else {
                hsb[2] = hsb[2] < 0.5f ? 1f - hsb[2] : 0.5f - hsb[2];
            }
            Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            if (c.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), c.getAlpha());
            }
            return result;
        }

    }

    public static boolean isBrighter(Color toTest, Color than) {
        HsbExtractor ae = new HsbExtractor(new FixedColorSupplier(than), 2);
        HsbExtractor be = new HsbExtractor(new FixedColorSupplier(toTest), 2);
        return ae.getAsFloat() > be.getAsFloat();
    }

    public static boolean isLessSaturated(Color toTest, Color than) {
        return !isMoreSaturated(toTest, than);
    }

    public static boolean isMoreSaturated(Color toTest, Color than) {
        HsbExtractor ae = new HsbExtractor(new FixedColorSupplier(than), 1);
        HsbExtractor be = new HsbExtractor(new FixedColorSupplier(toTest), 1);
        return ae.getAsFloat() > be.getAsFloat();
    }

    public static boolean isDarker(Color toTest, Color than) {
        return !isBrighter(toTest, than);
    }

    static final class RotateHueSupplier implements UIColorSupplier {

        private final Supplier<Color> orig;
        private final float by;

        public RotateHueSupplier(Supplier<Color> orig, float by) {
            this.orig = orig;
            this.by = by;
        }

        @Override
        public Color get() {
            Color c = orig.get();
            float[] hsb = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            hsb[0] = (hsb[0] + by);
            if (hsb[0] > 1f) {
                hsb[0] -= 1f;
            }
            Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            if (c.getAlpha() != 255) {
                result = new Color(result.getRed(), result.getGreen(), result.getBlue(), c.getAlpha());
            }
            return result;
        }
    }

    static UIColorSupplier supplier(Supplier<Color> supp) {
        return supp instanceof UIColorSupplier ? (UIColorSupplier) supp : supp::get;
    }

    public interface UIColorSupplier extends Supplier<Color> {

        default UIColorSupplier withTintFrom(Supplier<Color> tintSupplier, float percentageOfSaturation) {
            return new Tinter(this, tintSupplier, percentageOfSaturation);
        }

        default UIColorSupplier withBrightnessNoLessThanThatOf(Supplier<Color> other) {
            // HSBLimit
            UIColorSupplier supp = supplier(other);
            return new HSBLimit(this, supp::brightness, 2, true);
        }

        default UIColorSupplier withBrightnessNoGreaterThanThatOf(Supplier<Color> other) {
            // HSBLimit
            UIColorSupplier supp = supplier(other);
            return new HSBLimit(this, supp::brightness, 2, false);
        }

        default UIColorSupplier withBrightnessNoGreaterThan(float val) {
            return new HSBLimit(this, val, 2, false);
        }

        default UIColorSupplier withBrightnesNoLessThan(float val) {
            return new HSBLimit(this, val, 2, true);
        }

        default UIColorSupplier withSaturationNoGreaterThan(float val) {
            return new HSBLimit(this, val, 1, false);
        }

        default UIColorSupplier withSaturationNoLessThan(float val) {
            return new HSBLimit(this, val, 1, true);
        }

        default float component(int which) {
            Color c = get();
            float[] vals = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), vals);
            return vals[which];
        }

        default float brightness() {
            return component(2);
        }

        default float hue() {
            return component(0);
        }

        default float saturation() {
            return component(1);
        }

        default UIColorSupplier darkenOrLighten(float amount, BooleanSupplier supp) {
            FloatSupplier fs = () -> {
                return supp.getAsBoolean() ? -amount : amount;
            };
            return new HSBAdjuster(this, fs, 2);
        }

        default UIColorSupplier adjustSaturation(float amount, BooleanSupplier supp) {
            FloatSupplier fs = () -> {
                return supp.getAsBoolean() ? -amount : amount;
            };
            return new HSBAdjuster(this, fs, 1);
        }

        default UIColorSupplier darkenBy(float amt) {
            return new HSBAdjuster(this, -amt, 2);
        }

        default UIColorSupplier brightenBy(float amt) {
            return new HSBAdjuster(this, amt, 2);
        }

        default UIColorSupplier increaseSaturationBy(float amt) {
            return new HSBAdjuster(this, -amt, 1);
        }

        default UIColorSupplier decreaseSaturationBy(float amt) {
            return new HSBAdjuster(this, amt, 1);
        }

        default UIColorSupplier unless(BooleanSupplier supp, Supplier<Color> other) {
            return () -> {
                boolean val = supp.getAsBoolean();
                if (val) {
                    return other.get();
                }
                return UIColorSupplier.this.get();
            };
        }

        default UIColorSupplier rotatingHueBy(float val) {
            if (val < -1 || val > 1) {
                throw new IllegalArgumentException("Rotate must be >=-1 and <=1");
            }
            return new RotateHueSupplier(this, val);
        }

        default UIColorSupplier contrasting() {
            return new ContrastingColorSupplier(this);
        }

        default BooleanSupplier isBrighter(Supplier<Color> than) {
            int[] result = new int[1];
            return () -> {
                if (result[0] != 0) {
                    return result[0] == 1;
                }
                boolean res = Colors.isBrighter(get(), than.get());
                result[0] = res ? 1 : -1;
                return res;
            };
        }

        default BooleanSupplier isDarker(Supplier<Color> than) {
            int[] result = new int[1];
            return () -> {
                if (result[0] != 0) {
                    return result[0] == 1;
                }
                boolean res = !Colors.isBrighter(get(), than.get());
                result[0] = res ? 1 : -1;
                return res;
            };
        }

        default UIColorSupplier darkerOf(Supplier<Color> other) {
            return () -> {
                Color a = get();
                Color b = other.get();
                if (Colors.isDarker(a, b)) {
                    return a;
                } else {
                    return b;
                }
            };
        }

        default UIColorSupplier brighterOf(Supplier<Color> other) {
            return () -> {
                Color a = get();
                Color b = other.get();
                if (Colors.isBrighter(a, b)) {
                    return a;
                } else {
                    return b;
                }
            };
        }

        default UIColorSupplier leastSaturatedOf(Supplier<Color> other) {
            return () -> {
                Color a = get();
                Color b = other.get();
                if (Colors.isLessSaturated(a, b)) {
                    return a;
                } else {
                    return b;
                }
            };
        }

        default UIColorSupplier mostSaturatedOf(Supplier<Color> other) {
            return () -> {
                Color a = get();
                Color b = other.get();
                if (Colors.isLessSaturated(a, b)) {
                    return a;
                } else {
                    return b;
                }
            };
        }

        default UIColorSupplier cache() {
            if (this instanceof CachingColorSupplier || this instanceof FixedColorSupplier) {
                return this;
            }
            return new CachingColorSupplier(this);
        }

        default UIColorSupplier withHueFrom(Supplier<Color> color) {
            HsbExtractor ext = new HsbExtractor(color, 0);
            return new HSBChanger(this, ext, 0);
        }

        default UIColorSupplier withSaturationFrom(Supplier<Color> color) {
            HsbExtractor ext = new HsbExtractor(color, 1);
            return new HSBChanger(this, ext, 1);
        }

        default UIColorSupplier withBrightnessFrom(Supplier<Color> color) {
            HsbExtractor ext = new HsbExtractor(color, 2);
            return new HSBChanger(this, ext, 2);
        }

        default UIColorSupplier withAlpha(int fixedAlpha) {
            return new WithRGB(3, this, fixedAlpha);
        }

        default UIColorSupplier withHue(float hue) {
            return new HSBChanger(this, hue, 0);
        }

        default UIColorSupplier withSaturation(float saturation) {
            return new HSBChanger(this, saturation, 1);
        }

        default UIColorSupplier withBrightness(float bri) {
            return new HSBChanger(this, bri, 2);
        }

        default UIColorSupplier withHue(int hue) {
            float f = hue;
            f /= 255f;
            return new HSBChanger(this, f, 0);
        }

        default UIColorSupplier withSaturation(int saturation) {
            float f = saturation;
            f /= 255f;
            return new HSBChanger(this, f, 1);
        }

        default UIColorSupplier withBrightness(int bri) {
            float f = bri;
            f /= 255f;
            return new HSBChanger(this, f, 2);
        }

        default UIColorSupplier withRed(int red) {
            return new WithRGB(0, this, red);
        }

        default UIColorSupplier withGreen(int red) {
            return new WithRGB(1, this, red);
        }

        default UIColorSupplier withBlue(int red) {
            return new WithRGB(2, this, red);
        }
    }
}
