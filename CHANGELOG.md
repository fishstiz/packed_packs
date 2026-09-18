TODO

- Folder Packs are now discoverable recursively
- Folder Packs can now be locked/unlocked
    - Locked folder packs behave exactly the same as regular folder packs.
    - An unlocked folder packs' children can be transferred outside the folder.
      ([#47](https://github.com/fishstiz/packed_packs/issues/47))
        - Folder packs cannot be unlocked when it's a descendant of a locked folder pack.
        - Cannot itself be enabled, instead it will be flattened.
        - Can be sorted
    - TODO fix action validations
- IDs of nested non-folder packs no longer inherits the parent's ID. This means you can no longer have multiple
  packs with the same names under different folders.
    - Nested folder packs will still inherit parent IDs
    - **Note**: This change was made so nested non-folder packs do not suddenly change orders or be disabled when
      folders are moved/renamed or when Packed Packs is uninstalled.
    - TODO add auto migration
- Fixed search not working for folder packs

**API Changes**
- Folder packs no longer trigger pack-related events (they are not `Pack`s anymore).
- `PackContext` are now considered as `PackSelectionModel.Entry`

This is an unstable version, thing may break or change. Please report any
issues [here](https://github.com/fishstiz/packed_packs/issues).