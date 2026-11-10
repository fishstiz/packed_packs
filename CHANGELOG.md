- Added ability to reorder profiles from the profiles sidebar by dragging with the leftmost
  button [(#60)](https://github.com/fishstiz/packed_packs/issues/60).
- Added descriptions to profiles, which are shown as tooltips on hover or
  focus. [(#65)](https://github.com/fishstiz/packed_packs/issues/65)
    - Can be edited from the edit button on the profiles sidebar
- Added option to swap actions of the "Show Packs" button with the transfer sprite and quick transfer (double click)
  shortcut.
  [(#80)](https://github.com/fishstiz/packed_packs/issues/80)
    - When on, the "Show Packs" button will instead transfer all packs within the folder, and the Select/Deselect button
      will open the folder. Double-clicking will now also open the folder instead of transferring all packs.
    - Can be toggled on, off, or for unlocked folders only
- Added option to configure the max width of the Packed Packs screen.
- Allowed root level packs to be added virtually to folders, same as with built-in packs. Same limitations apply.
- Newly discovered sub folders will now be placed on top of the saved order.
    - **Note**: The order of packs (including subfolders) are saved within a folders config, allowing the order to be
      preserved when recursively transferring all packs of a folder. The sort option is also changed to "None" when
      entering a folder, which shows the preserved order instead of the previously selected sort option. This may
      explain some confusion regarding why sub folders may show below regular packs. I am open to feedback on this, feel
      free to leave them on the issues page.
- Opening dropdowns now change the focus to the dropdown container, which may fix some issues with mods that attempt to
  fix [MC-259387](https://bugs.mojang.com/browse/MC/issues/MC-259387) by resetting the focus of buttons on
  click [(#79)](https://github.com/fishstiz/packed_packs/issues/79).
    - **Note**: This does not fix all compat issues with mods that do this, but honestly I don't really agree with
      fixing it by just resetting the focus, just because Minecraft didn't work that way previously. It should be safe
      for me to assume that the focused widget is the active or last element interacted with (like after a click), which
      these mods break. The fix to the issue should be to make focus visible only when relevant. I left some comments on the
      github issue and on mojira. 