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

import com.mastfrog.visualtabs.buttons.ButtonAction;
import java.util.EnumSet;
import java.util.Set;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.netbeans.swing.tabcontrol.WinsysInfoForTabbedContainer;

/**
 *
 * @author Tim Boudreau
 */
public enum TabFeatures {

    MAXIMIZE,
    TABS_POPUP,
    CLOSE_BUTTONS;

    public static boolean isAvailable(ButtonAction action, Set<TabFeatures> enabled) {
        switch (action) {
            case MAXIMIZE:
                return enabled.contains(MAXIMIZE);
            case POPUP:
                return enabled.contains(TABS_POPUP);
        }
        return true;
    }

    public static Set<TabFeatures> fromTabDisplayer(TabDisplayer disp) {
        EnumSet result = EnumSet.allOf(TabFeatures.class);
        if (disp == null) {
            result.remove(MAXIMIZE);
            result.remove(TABS_POPUP);
        } else {
            WinsysInfoForTabbedContainer info = disp.getContainerWinsysInfo();
            if (!info.isTopComponentClosingEnabled()) {
                result.remove(CLOSE_BUTTONS);
            }
        }
        return result;
    }
}
