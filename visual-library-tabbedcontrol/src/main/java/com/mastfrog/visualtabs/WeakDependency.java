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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.netbeans.api.visual.widget.Widget;

/**
 * A dependency with weak references in both directions, used by
 * animation timers to get notifications while active without
 * being held forever if something goes wrong.
 *
 * @author Tim Boudreau
 */
final class WeakDependency implements Widget.Dependency {

    private final Reference<Widget.Dependency> ref;
    private final Reference<Widget> widget;

    WeakDependency(Widget widget, Widget.Dependency orig) {
        ref = new WeakReference<>(orig);
        this.widget = new WeakReference<>(widget);
    }

    @Override
    public void revalidateDependency() {
        Widget.Dependency delegate = ref.get();
        if (delegate == null) {
            Widget widge = widget.get();
            if (widge != null) {
                widge.removeDependency(this);
            }
        } else {
            delegate.revalidateDependency();
        }
    }

}
