# 📦 Packed Packs

Pack resource and data packs into profiles with multiple selection, drag and drop, and extended mouse and keyboard
controls.

![packed_packs_demo_compressed](https://github.com/user-attachments/assets/6f7995cc-665d-4989-976c-d07ee2ca1ecf)

## ✨ Features

- Save and load custom profiles.
- Select multiple packs at once.
- Drag and drop selection between columns.
- Context Menus.
- [Additional Folders](#additional-folders).
- [Folder Packs](#folder-packs).
- Search by title.
- Filter out incompatible packs.
- Sort alphabetically or by last updated.
- [Mouse](#mouse-controls) and [keyboard](#keyboard-controls) controls.
- [Configuration](#configuration).
- [Developer Mode](#developer-mode).
- [Java API](#java-api).
- Explicit [compatibility](#compatibility) with certain mods.
- History (undo and redo).

<a id="additional-folders"></a>
<details>
<summary><b>📂 Additional Folders</b></summary>

- Add extra folders for pack discovery.
- Configure in `config/packed_packs/config.json` by adding paths under the `additionalFolders` array inside
  `resourcepacks` or
  `datapacks`.
- If the array doesn’t exist, create it manually or open and close the Packed Packs screen to update the config.
- Paths can be absolute or relative to the game directory.
- Correctly added folders appear as a context menu option under **Open Pack Folder**.
- **Requires game restart to apply.**

</details>

<a id="folder-packs"></a>
<details>
<summary><b>📂📦 Folder Packs</b></summary>

- Any folder in the root pack directory containing packs, without a `pack.mcmeta`, will be treated as a folder pack.
- Folder packs behave like regular packs and can be moved between rows and columns to toggle multiple packs at once.
- They can be opened to view and reorder their contents. The order is saved to `packed_packs.folderpack.json` in the
  folder root.
- Add a `pack.png` at the folder root to set a custom icon.

</details>

<a id="mouse-controls"></a>
<details>
<summary><b>🖱️ Mouse Controls</b></summary>

- Open Context Menu — right click
- Select range — hold <kbd>Shift</kbd> and click
- Add/remove from selection — hold <kbd>Ctrl</kbd> and click
- Transfer single entry quickly — double click
- Undo — click backwards side button
- Redo — click forwards side button

</details>

<a id="keyboard-controls"></a>
<details>
<summary><b>⌨️ Keyboard Controls</b></summary>

- Navigate entries — <kbd>↑</kbd> | <kbd>↓</kbd>
- Navigate out of entries — <kbd>Tab</kbd>
- Transfer selection — <kbd>Space</kbd> | <kbd>Enter</kbd>
- Select range — <kbd>Shift</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd>
- Select all — <kbd>Ctrl</kbd> + <kbd>A</kbd>
- Move selection — <kbd>Ctrl</kbd> / <kbd>Alt</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd>
- Undo — <kbd>Ctrl</kbd> + <kbd>Z</kbd>
- Redo — <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Z</kbd> | <kbd>Ctrl</kbd> + <kbd>Y</kbd>
- Open folder pack — <kbd>Enter</kbd>
- Close folder pack — <kbd>Escape</kbd>
- Delete file — <kbd>Delete</kbd>
- Rename file — <kbd>Ctrl</kbd> + <kbd>R</kbd> | <kbd>F2</kbd> (if not bound to screenshot)
- Open file — <kbd>Ctrl</kbd> + <kbd>Enter</kbd>
- Show in file manager — <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>R</kbd>
- Toggle profiles sidebar — <kbd>Ctrl</kbd> + <kbd>`</kbd>
- Switch between default and no profile — <kbd>F1</kbd>
- Refresh packs — <kbd>F5</kbd>
- Focus search bar — type any character

</details>

<a id="configuration"></a>
<details>
<summary><b>⚙️ Configuration</b></summary>

- Apply resource packs automatically on close.
- Replace the default resourcepack & datapack screens.
- Remove the red background on incompatible packs.
- Remember the last viewed profile when reopening the screen.

</details>

<a id="developer-mode"></a>
<details>
<summary><b>🚀 Developer Mode</b></summary>

- Toggle developer mode — <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>I</kbd> | <kbd>F12</kbd>
- Additional options will appear in the context menu when in developer mode.
- **Preferences**: Toggle the visibility of certain widgets.
- **Lock Profiles**: Locked profiles cannot be deleted, renamed, or modified.
- **Override Pack Properties**:
    - Overrides are configured per profile and apply only to that profile unless set as default.
    - Override the *required* property of packs. Also allows disabling required packs.
    - Override the *position* property of packs. Also allows moving fixed packs.
    - Hide packs.
- **Default Profiles**:
    - Overrides from the default profile are enabled **globally** for all profiles and even without one, including the
      original screen.
    - Enabled packs under the default profile are automatically marked as compatible.
    - Default profiles load automatically in the following cases:
        - **Resource Packs**: when the `options.txt` file is missing.
        - **Data Packs**: when creating a new world.
- **Copy to Clipboard**: Copy the select pack's ID.
- **Pack Aliases**: Add aliases to Pack IDs
    - Designed to help modpack devs migrate resource/data packs when updates are needed without affecting the user's
      configured pack selection and order.
    - Only used if the pack is selected but doesn't exist. Matching starts from the top of the alias map
      and resolves to the first match, see the `config.meta.json` file.
    - Supports regex (must be prefixed with `regex:`), the text color will change in the _Edit Aliases_ dialog if done
      correctly.
    - Exact matches always take priority over regex matches, regardless of their position in the alias map (e.g., a pack
      ID of `file/test-v1.2` will always match `"file/test-v1.2": "file/test-v1.4"` over
      `"regex:file\\/test-v\\d*": "file/test-v2"`).
    - Aliases are never cleared automatically, even if it points to a pack that no longer exists. They are only removed
      manually from the in-game GUI or the config file.
    - It is generally not recommended to use on data packs as it could break worlds. Test thoroughly.

</details>

<a id="java-api"></a>
<details>
<summary><b>♨️ Java API</b></summary>

The Java API, designed as an optional dependency, allows mods to extend functionality or add compatibility with 
Packed Packs. This includes subscribing to various events and registering custom preferences
to add configurable widgets at certain positions of the screen.

You can find example implementations in the [**testmod**](https://github.com/fishstiz/packed_packs/tree/master/mod/common/src/testmod), 
and in the [**compat**](https://github.com/fishstiz/packed_packs/tree/mc/1.21.11/mod/common/src/main/java/io/github/fishstiz/packed_packs/compat) 
package of the **main** source set. 

You may also view the [**javadocs**](https://github.com/fishstiz/packed_packs/tree/mc/1.21.11/api/common/src/main/java/io/github/fishstiz/packed_packs/api) from the source code.

Visit the [wiki page](https://fishstiz.github.io/packed_packs-wiki/java-api/getting-started) for more details.

</details>

<a id="compatibility"></a>
<details>
<summary><b>🔗 Compatibility</b></summary>

Explicit compatibility is added for:

- Resourcify
- Respackopts
- VTDownloader
- Entity Texture Features
- Polytone

[Submit an issue](https://github.com/fishstiz/packed_packs/issues) if the above mods have become incompatible. Make sure
to verify that the correct mod version is used for the target minecraft version, and if the issue only occurs with
Packed Packs installed.

<b>Note:</b> Compatibility for the above mods were added before the Java API was made.
It would be better for other mods to make use of the Java API instead for better long
term compatibility. If the above mods make enough breaking changes, it may have to be dropped.
</details>