- Allowed built-in packs in folder packs ([#61](https://github.com/fishstiz/packed_packs/issues/61))
    - The built-in pack ID must be in the folder's `packed_packs.folderpack.json` `packIds`,
        - **Note**: Pack IDs can be copied to clipboard by entering dev mode (`F12`), right-clicking a pack, and
          selecting the copy
          option.
        - Example:
          ```json
          {
            "module": false,
            "packIds": [
              "continuity:default",
              "examplenamespace:examplepath"
            ] 
          }
          ```
    - This must be done outside the Packed Packs screen since the folder metadata is not watched and loaded once only.
        - **Note**: It's loaded once only to avoid overwriting unsaved changes as the folder metadata is practically
          mutable. There is probably a better solution to this but this is the current limitation.
- Allowed empty folders to load as folder packs.
- Added `sort_none` icon for none sort.
- Updated icons for `trash`, `lock` and `unlock`.
- Widgets on top of pack entries are now navigable by keyboard.
- Fixed crash when saving state while a folder nested more than two levels deep is opened.
- Fixed sort button not working consistently for nested folders.
- Fixed folders nested more than three levels deep not being watched.
- Fixed folders not showing up first on `OLDEST` and `Z_A` sort options.
- Fixed pack rename incorrectly updating configs.

**Packed Packs API**

- Fixed `ScreenContext#rebuild` not rebuilding pack list entries (affects `v2.3.0-beta.2` only)

This is an unstable version, things may break or change.

These are the last new changes for **v2.3.0** so please report any issues and provide
feedback [here](https://github.com/fishstiz/packed_packs/issues) before I take another break on developing new
features, it would be greatly appreciated. If no more issues are found in around a week then a stable version may be
released.