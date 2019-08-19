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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.netbeans.api.visual.model.ObjectState;

/**
 *
 * @author Tim Boudreau
 */
final class ButtonIcon {

    private final ButtonAction types;
    private final ButtonPainter painter;

    public ButtonIcon(ButtonAction types, ButtonPainter painter) {
        this.types = types;
        this.painter = painter == null ? new DefaultButtonPainter() : painter;
    }

    public ButtonAction actionType() {
        return types;
    }

    @Override
    public String toString() {
        return types.toString();
    }

    public void paint(Rectangle into, Graphics2D g, ObjectState state, boolean enabled) {
        int inset = 2;
        into.x += inset;
        into.y += inset;
        int doubleInset = inset * 2;
        into.width -= doubleInset;
        into.height -= doubleInset;
        switch (types) {
            case POPUP:
                into.y += 1;
                into.height -= 1;
                into.x -= 4;
                break;
            case MAXIMIZE:
                into.x -= 5;
                break;
        }
        painter.paint(g, into, types.shape(into), types, state, enabled);
    }
}
