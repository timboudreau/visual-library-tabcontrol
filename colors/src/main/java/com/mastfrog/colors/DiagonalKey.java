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
final class DiagonalKey implements Comparable<DiagonalKey> {

    private final int[] values;
    private final boolean cyclic;
    private long touched = System.nanoTime();

    DiagonalKey(int x1, int x2, int y1, int y2, Color top, Color bottom, boolean cyclic) {
        this(x1, x2, y1, y2, top.getRGB(), bottom.getRGB(), cyclic);
    }

    DiagonalKey(int x1, int x2, int y1, int y2, int top, int bottom, boolean cyclic) {
        values = new int[]{x1, x2, y1, y2, top, bottom};
        this.cyclic = cyclic;
    }

    void touch() {
        touched = System.nanoTime();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(Integer.toHexString(values[i]));
            if (i != values.length - 1) {
                sb.append('-');
            }
            sb.append(cyclic ? 'c' : 'n');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Arrays.hashCode(this.values) + (cyclic ? 73 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DiagonalKey)) {
            return false;
        }
        final DiagonalKey other = (DiagonalKey) obj;
        return Arrays.equals(this.values, other.values);
    }

    @Override
    public int compareTo(DiagonalKey o) {
        return Long.compare(touched, o.touched);
    }

}
