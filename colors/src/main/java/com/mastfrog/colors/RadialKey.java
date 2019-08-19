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
package com.mastfrog.colors;

import java.awt.Color;
import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
final class RadialKey {

    private final int[] values = new int[4];

    public RadialKey(int radius, Color topColor, Color bottomColor) {
        values[0] = radius;
        values[1] = topColor.getRGB();
        values[2] = bottomColor.getRGB();
        values[3] = 11 * Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "RadialKey{" + "radius=" + values[0] + ", topColor=" + GradientUtils.colorToString(new Color(values[1], true)) + ", bottomColor=" + GradientUtils.colorToString(new Color(values[2], true)) + '}';
    }

    @Override
    public int hashCode() {
        return values[3];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof RadialKey) {
            return Arrays.equals(values, ((RadialKey) obj).values);
        }
        return false;
    }

}
