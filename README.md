Visual Library Tabbed Control
=============================

A replacement tab control for NetBeans editor tabs, with animation and bling, that
uses [Visual Library](https://netbeans.apache.org/tutorials/nbm-visual_library.html) under
the hood.

I wrote the original (and still in use) tab control for NetBeans 3.6 in 2003 or so;  this
replaces the _tab displaying_ portion of the editor tab controls (it is divided into
two pieces - a "displayer" which shows tabs and maps events to specific tabs, and a
tabbed control which owns it and updates what component is shown in its content area.

It derives its colors from those of the look and feel using the
[Colors library here](https://github.com/timboudreau/desktop), which can do complex,
contingent transforms of RGB and HSB values, so there is no specific, configuration needed,
and uses its high-performance gradient-paint caching.

In general, it's pretty and _just works_.


Internals
---------

  * `TabScene` is the Visual Library scene which comprises the tabs area, and contains
    the listener code to respond to selection changes (internally or due to clicks on
    the tab control), with some optimizations to avoid triggering cascades of events
    when many tabs are opened or closed (such as during startup or when switching
    project groups)
  * `TabWidgetMapper` does the heavy lifting of keeping the scene's model of what tabs
    are visible in sync with the TabDataModel that gets manipulated by the windowing
    system when users open files, etc.
  * `PanTray` is the inner widget the tabs live in, and works similarly to the
    `Viewport` of a `JScrollPane` - it is responsible for the sliding window you can
    see over all the tabs which are in the scene; it is responsible for handling mouse
    wheel events, and for ensuring space is optimally used and that there are always
    some tabs, including the selected tab, visible, after model changes
  * `SortedFlowLayout` positions the tabs within the `PanTray` and is similar to
    the standard Visual Library flow layout, except that it keeps tabs ordered by
    their position in the `TabDataModel` we are listening to, rather than the order
    they happened to be added to the container
  * `GlowWidget` sits in a layer above the tabs and is responsible for the glow around
    the selected tab - Visual Library works based on a concept of _widget dependencies_
    rather than simply container and contained, and routinely uses layers in which
    components overlap, rather like Swing's glass pane - so the glow widget is given a
    dependency on whatever the currently selected `TabWidget` is, and while it has the
    dependency, it repositions itself around it and is revalidated whenever the tab
    widget is.
  * `ButtonsPanel` handles the buttons that appear to the right of tabs in the UI
  * `TabsAppearance` is where all of the background gradient and animation painting
    code lives.  Colors are derived from UIManager key/color pairs (with fallbacks
    for badly behaved look and feels), and dark look and feels are detected by a
    combination of known look-and-feel ids and comparing color pairs from UIManager.
    The API from the Colors library is built around `ColorSupplier`, which is,
    unsurprisingly, a `Supplier<Color>` with a fluent API for transforms and caching
    so you don't recompute every time (unless you need to), so you can do things like

```java
selectedHl.invertRGB()
    .withSaturationNoGreaterThan(0.25f)
    .withBrightnessFrom(Colors.fromUIManager(textFallback, 
        "controlText", "textText")) // fallbacks
    .unless(isDark, baseHl.contrasting().withBrightnessNoGreaterThan(0.875f)
```



