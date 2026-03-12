- Added a formal Java API mainly to support adding simple widgets. Refer to the new Wiki page for more details.
- Added Polytone button ([#43](https://github.com/fishstiz/packed_packs/issues/43)) to the Packed Packs screen.
    - **Note**: Mod developers can use the new API for custom implementations to ensure better long-term compatibility.
- Added Simplified Chinese Translation ([#46](https://github.com/fishstiz/packed_packs/pull/46)
  by [qznfbnj](https://github.com/qznfbnj))
- Added incompatible title and description when hovering or selecting incompatible packs to match vanilla
  behavior. Can be disabled with the "Hide incompatible Warnings" option.
- Improved keyboard navigation.
    - Focus is now correctly redirected when opening or closing most dialogs.
    - Arrow key navigation in the profiles list now prioritizes the profile button over the delete button.
    - Arrow key navigation in pack lists is now constrained within the lists. To move focus away from the pack list, use
      `Tab` or `Shift+Tab`.
    - Added pack list shortcuts for `Home`, `End`, `PgUp` and `PgDown`.
- Removed config migration from v1 to v2. If updating directly from v1, your old config file will remain on disk but will no longer be used.
- Removed the automatic hash suffix from new profile IDs.
- Fixed crash caused by illegal characters in profile names ([#42](https://github.com/fishstiz/packed_packs/issues/42)).
- Fixed crash with VTDownloader on 1.21.11.
- Fixed 'Remember Last Viewed Profile' option always being on.
- Made significant internal changes to the Packed Packs screen which may result in a different user experience or introduce new
  bugs. Please submit feedback or report any issues [here](https://github.com/fishstiz/packed_packs/issues).
- Changed background texture of dialogs to `popup/background.png` sprite as `demo_background.png` will be removed in
  the Minecraft 26.1 update.
- Fixed being able to disable mod resources in NeoForge via overrides.
- Bumped Fabric Loader version.
