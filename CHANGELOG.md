- Changed the **Sort** cycling button to a dropdown button.
- Added dropdown button next to the **Open Pack Folder** button to open additional folders if there is any.
- Added buttons to toggle **default** and **lock** of profiles next to the profile list when on dev mode. 
- Increased the max height of context menu.
- Fixed folder pack order not saving when the folder pack list is not closed.
- Fixed pack entries getting stuck with the incompatible title and description when navigating by keyboard.
- Fixed hidden override not immediately reflecting on the pack screen when toggling dev mode.
- Fixed profile name failing to resolve when the profiles directory has not yet created.
- Fixed crash when reading an invalid config file.

--- 
**Packed Packs API**
- Added methods to add `Renderable` and `Renderable & LayoutElement`s to `InitializePackEntryEvent`.
- Added `ScreenContext#minecraft()`