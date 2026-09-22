- Folder Packs are now discoverable recursively
- Folder Packs can now be locked/unlocked
    - Can be toggled from the folder view or from the metadata under the `module` flag.
    - Locked folders behave exactly the same as regular folder packs.
        - Nested folders within locked folder packs will be force-locked.
        - Cannot be sorted
    - An unlocked folder's children can be transferred outside the folder.
      ([#47](https://github.com/fishstiz/packed_packs/issues/47))
        - Cannot itself be enabled, will instead be flattened.
        - Can be sorted
        - Folders cannot be unlocked from the enabled list.
    - Folders are unlocked by default. Can be changed from the options menu.
    - Existing folder packs prior to updating will be locked to preserve behavior.
- Folder pack metadata files are no longer watched and are loaded immediately with the folder pack upon entering the
  screen.
- Renaming a pack from the Packed Packs screen now also updates all profiles and folders that contain that pack.
- Fixed search not working for folders.
- Fixed being unable to drag non-required fixed position packs from the enabled list.

**API Changes**

- Folders no longer trigger pack-related events (they are not `Pack`s anymore).
- `PackContext` are now considered as `PackSelectionModel.Entry`
- `ScreenContext#getAvailablePacks` and `ScreenContext#getSelectedPacks` now includes all descendants (flattened).

This is an unstable version, thing may break or change. Please report any
issues [here](https://github.com/fishstiz/packed_packs/issues).