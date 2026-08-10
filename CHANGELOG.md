- Added configurable load conditions in the context menu options for the default resource pack profile:
    - `NO_OPTIONS_OR_VERSION_FILE`: Triggers when the `options.txt` or `config/packed_packs/__version.json` is missing.
        - Fixes default profile not applying with Default Options
          ([#67](https://github.com/fishstiz/packed_packs/issues/67))
        - Used as the default condition when the `config.meta.json` is newly created.
        - **Note**: You should not ship the `__version.json` file in modpacks, and since it's new to this update,
          switching to this option will reset the resource pack configuration for existing users.
    - `NO_OPTIONS`: Triggers only when the `options.txt` is missing.
        - Only exists to preserve the resource pack configuration for existing users when updating to this or newer
          versions of the mod, otherwise use the above option.
        - Used as the default load condition when the `config.meta.json` file already exists.
- Updated Russian Translation ([#70](https://github.com/fishstiz/packed_packs/pull/70) by iceban)
- Improved compatibility with smooth scrolling ([#71](https://github.com/fishstiz/packed_packs/issues/71))
- Arrow key navigation in pack lists is no longer constrained.