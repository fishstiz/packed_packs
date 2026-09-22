TODO

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
- IDs of nested regular (non-folder) packs no longer inherit the parent's ID. This means you can no longer have multiple
  packs with the same names under different folders.
    - Nested folders will still inherit parent IDs
    - This change was made so moving/renaming folders externally no longer changes the order of nested packs, 
      and when flattened manually, will no longer be disabled when Packed Packs is uninstalled.
    - **This will break existing configs that use nested packs**. Nested resource and data packs will be disabled and
      the order within folder packs will revert to natural order.
    - Auto migration exists and will automatically be enabled for users updating from **v2.2.3 and above**,
      specifically those that have the `__version.json` file with the `version` set to `1`.
        - Migration of resource packs occurs on the first load only upon updating.
        - Migration of data packs only occurs upon entering a world.
            - Folder metadata and profiles are updated once on discovery, but users still need to enter the world to
              update the data packs in the `level.dat`.
        - The `migrations` directory will be created which tracks the migrated packs.
          Do not include this in modpacks.
        - Migrations can be fully disabled from `__version.json` by setting `migrateLegacyFolders` to `false`. You do
          not need to do this for fresh installations.
    - If you do not agree with this change then you can provide your feedback on the github issues page.
- Folder pack metadata files are no longer watched and are loaded immediately with the folder pack upon entering the
  screen.
- Fixed search not working for folders.
- Fixed being unable to drag non-required fixed position packs from the enabled list.

**API Changes**

- Folders no longer trigger pack-related events.
- `PackContext` are now considered as `PackSelectionModel.Entry`
- `ScreenContext#getAvailablePacks` and `ScreenContext#getSelectedPacks` now includes all descendants (flattened).
- No actual code breaking changes.

This is an unstable version, thing may break or change. Please report issues or provide
feedback [here](https://github.com/fishstiz/packed_packs/issues).