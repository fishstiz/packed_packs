TODO

- Folder Packs are now discoverable recursively
- Folder Packs can now be locked/unlocked
    - Locked folders behave exactly the same as regular folder packs.
        - Nested folders within locked folder packs will be force-locked.
        - Cannot be sorted
    - An unlocked folder's children can be transferred outside the folder.
      ([#47](https://github.com/fishstiz/packed_packs/issues/47))
        - Cannot itself be enabled, instead it will be flattened.
        - Can be sorted
    - TODO fix action validations
    - TODO add config option to change default folder state
- IDs of nested non-folder packs no longer inherits the parent's ID. This means you can no longer have multiple
  packs with the same names under different folders.
    - Nested folders will still inherit parent IDs
    - **Note**: This change was made so nested non-folder packs do not suddenly change orders or be disabled when
      folders are moved/renamed or when Packed Packs is uninstalled.
    - TODO add auto migration
- Fixed search not working for folders.
- Fixed being unable to drag non-required fixed position packs from the enabled list.

**API Changes**
- Folders no longer trigger pack-related events (they are not `Pack`s anymore).
- `PackContext` are now considered as `PackSelectionModel.Entry`
- `ScreenContext#getAvailablePacks` and `ScreenContext#getSelectedPacks` now includes all descendants (flattened).

This is an unstable version, thing may break or change. Please report any
issues [here](https://github.com/fishstiz/packed_packs/issues).