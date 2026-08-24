- Fixed exact aliases not updating the pack id in profiles ([#72](https://github.com/fishstiz/packed_packs/issues/72)).
- Aliases can now resolve to a regex pack id when prefixed with `regex:`. The regex pack id will attempt to find the
  first pack with the matching id. This can only be done manually in the `config.meta.json` file for now.
    - Example using an exact alias.
        ```json
        {
          "aliases": {
            "file/test-pack v1.2.zip": "regex:file/test-pack v.*\\.zip"          
          }
        }
        ``` 
    - Example using a regex alias:
      ```json
      {
        "aliases": {
          "regex:file/test-pack v.*\\.zip": "regex:file/test-pack v.*\\.zip"          
        }
      }
      ```
    - Note that `aliases` in the `config.meta.json` maps alias to canonical. Pack ids saved in configs but do not exist
      will attempt to find a matching key in the alias map. The value is the id of the would-be existing pack. If the mapped
      value is a regex id, then it will attempt to find an existing pack that matches the regex.  