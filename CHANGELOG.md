This is the stable release of **v2.3.0**, to recap:

- Folder Packs can now be nested.
- Folder Packs can now be locked and unlocked.
    - Locked folders behave exactly the same as usual.
    - Nested folders within locked folder packs will be force-locked.
    - Children of unlocked folders can be enabled individually
      ([#47](https://github.com/fishstiz/packed_packs/issues/47)).
        - Unlocked folders that become locked will automatically recall its descendants.
    - Unlocked folders themselves cannot be enabled, they will be flattened instead.
    - Folder packs are now unlocked by default. This can be changed from the options menu.
        - Existing folder packs with saved configs (that has no `module` flag) will be locked to preserve behavior.
- Allowed built-in packs in folder packs ([#61](https://github.com/fishstiz/packed_packs/issues/61))
    - The built-in pack ID must be in the folder config's `packIds`,
    - This can only be done outside the Packed Packs screen due to the change below.
- Folder pack config files are no longer watched and are loaded immediately upon entering the screen. This is to prevent
  overwriting unsaved state on refreshes.
- Empty folders will now load as folder packs.
- Renaming a pack from the Packed Packs screen should now also update all profiles and its parent folder pack.
- Updated Russian Translations ([#75](https://github.com/fishstiz/packed_packs/pull/75)
  by [iceban](https://github.com/iceban))
- Updated icons for `trash`, `lock`, and `unlock`.
- Widgets on pack entries are now navigable by keyboard.
- Fixed search not working for folders.
- Fixed being unable to drag non-required fixed position packs from the enabled list.

Changes since v2.3.0-beta.3

- Removed restrictions on symlinks, however restrictions set by vanilla Minecraft may still apply (symlinks may need to
  be specified in `allowed_symlinks.txt`).
- Improved compatibility with Controlify.

Fixes with v2.3.0-beta.3:

- Fixed hidden folders being loaded as folder packs
- Fixed empty folders not showing up inside folders.
- Fixed folder order not saving on undo/redo
- Fixed remember last viewed profile option not working.
- Fixed incorrect warning being logged about opening non-module folder from enabled list.
- Fixed folders sometimes changing its lock state when the "Folders Locked by Default" option is changed.
- Built-in packs contained in folders will now log duplicates in other folders. The precedence of which folder wins is
  not known.

**API Changes**

- Folders no longer trigger pack-related events (they are not `Pack`s anymore).
- `PackContext` are now considered as `PackSelectionModel.Entry`
- `ScreenContext#getAvailablePacks` and `ScreenContext#getSelectedPacks` are now flattened.