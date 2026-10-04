# 📦 Packed Packs

Pack resource and data packs into profiles with multiple selection, drag and drop, and extended mouse and keyboard
controls.

![packed_packs_demo_compressed](https://github.com/user-attachments/assets/6f7995cc-665d-4989-976c-d07ee2ca1ecf)

## ✨ Features

- Save and load custom profiles.
- Select multiple packs at once.
- Drag and drop selection between columns.
- Right click context menu for file operations and other options.
- Search by pack title.
- Sort packs alphabetically or by date updated.
- Filter out incompatible packs.

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

- Any folder without a `pack.mcmeta` will be treated as a folder pack.
- Add a `pack.png` at the folder root to set a custom icon.
- Folders can be nested.
- They can be opened to view and reorder their contents. The order is saved to `packed_packs.folderpack.json` in the
  folder root.
- Folder packs can be locked to make them behave like regular packs which can be moved between rows and columns. 
  These are also known as `module`s.
  - Children cannot be moved outside locked folder packs. Locking a folder pack will automatically recall its children. 
  - Locked folder packs will also lock all descendants that are folders. 
  - Newly discovered folders can be made locked by default from the options menu. 
- Built-in packs can be added inside folder packs when specified in `packed_packs.folderpack.json`.
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
  - **Note**: This can only be done outside the Packed Packs screen as the `packed_packs.folderpack.json` 
    file is only loaded once on enter to prevent overwriting unsaved state.

</details>

<a id="mouse-controls"></a>
<details>
<summary><b>🖱️ Mouse Controls</b></summary>

<table>
  <tbody>
    <tr>
      <td>Open Context Menu</td>
      <td>right click</td>
    </tr>
    <tr>
      <td>Select range</td>
      <td>hold <kbd>Shift</kbd> and click</td>
    </tr>
    <tr>
      <td>Add/remove from selection</td>
      <td>hold <kbd>Ctrl</kbd> and click</td>
    </tr>
    <tr>
      <td>Quick transfer</td>
      <td>double click</td>
    </tr>
    <tr>
      <td>Undo</td>
      <td>click backwards side button</td>
    </tr>
    <tr>
      <td>Redo</td>
      <td>click forwards side button</td>
    </tr>
  </tbody>
</table>

</details>

<a id="keyboard-controls"></a>
<details>
<summary><b>⌨️ Keyboard Controls</b></summary>

<table>
  <tbody>
    <tr>
      <td>Navigate entries</td>
      <td><kbd>↑</kbd>, <kbd>↓</kbd></td>
    </tr>
    <tr>
      <td>Navigate out of entries</td>
      <td><kbd>Tab</kbd></td>
    </tr>
    <tr>
      <td>Transfer selection</td>
      <td><kbd>Space</kbd>, <kbd>Enter</kbd></td>
    </tr>
    <tr>
      <td>Select range</td>
      <td><kbd>Shift</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd></td>
    </tr>
    <tr>
      <td>Select all</td>
      <td><kbd>Ctrl</kbd> + <kbd>A</kbd></td>
    </tr>
    <tr>
      <td>Move selection</td>
      <td><kbd>Ctrl</kbd> / <kbd>Alt</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd></td>
    </tr>
    <tr>
      <td>Undo</td>
      <td><kbd>Ctrl</kbd> + <kbd>Z</kbd></td>
    </tr>
    <tr>
      <td>Redo</td>
      <td><kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Z</kbd>, <kbd>Ctrl</kbd> + <kbd>Y</kbd></td>
    </tr>
    <tr>
      <td>Open folder pack</td>
      <td><kbd>Enter</kbd></td>
    </tr>
    <tr>
      <td>Close folder pack</td>
      <td><kbd>Escape</kbd></td>
    </tr>
    <tr>
      <td>Delete file</td>
      <td><kbd>Delete</kbd></td>
    </tr>
    <tr>
      <td>Rename file</td>
      <td><kbd>Ctrl</kbd> + <kbd>R</kbd>, <kbd>F2</kbd> (if not bound to screenshot)</td>
    </tr>
    <tr>
      <td>Open file</td>
      <td><kbd>Ctrl</kbd> + <kbd>Enter</kbd></td>
    </tr>
    <tr>
      <td>Show in file manager</td>
      <td><kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>R</kbd></td>
    </tr>
    <tr>
      <td>Toggle profiles sidebar</td>
      <td><kbd>Ctrl</kbd> + <kbd>`</kbd></td>
    </tr>
    <tr>
      <td>Switch between default and no profile</td>
      <td><kbd>F1</kbd></td>
    </tr>
    <tr>
      <td>Refresh packs</td>
      <td><kbd>F5</kbd></td>
    </tr>
    <tr>
      <td>Focus search bar</td>
      <td><kbd>Ctrl</kbd> + <kbd>F</kbd>, <kbd>Ctrl</kbd> + <kbd>K</kbd></td>
    </tr>
  </tbody>
</table>

</details>

<a id="configuration"></a>
<details>
<summary><b>⚙️ Configuration</b></summary>

- Apply resource packs automatically on close.
- Replace the default resourcepack & datapack screens.
- Remove the red background on incompatible packs.
- Make folder packs locked by default.
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
        - **Resource Packs**:  when the `options.txt` or the `config/packed_packs/__version.json`, is missing.
        - **Data Packs**: when creating a new world.
- **Copy to Clipboard**: Copy the select pack's ID.
- **Pack Aliases**: Add aliases to Pack IDs
    - Designed to help modpack devs migrate resource/data packs when updates are needed without affecting the user's
      configured pack selection and order.
    - Only used if the pack is selected but doesn't exist. Matching starts from the top of the alias map
      and resolves to the first match, see the `config.meta.json` file.
    - Supports regex (must be prefixed with `regex:`), the text color will change in the _Edit Aliases_ dialog if done
      correctly.
    - Aliases can also map to a regex value (must be prefixed with `regex:`), which will attempt to match with any available pack. 
      This can only be done from the `config.meta.json` file.     
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

You can find example implementations in the [**testmod**](https://github.com/fishstiz/packed_packs/tree/master/testmod/common/src/main/java/io/github/fishstiz/testmod), 
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