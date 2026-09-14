- Port to 26.3-rc.3
- Typing anywhere no longer focuses and inputs the character to the closest search field (due to the switch from GLFW to
  SDL3 in 26.3). Instead, the closest search field can be focused using `Ctrl` + `K` or `Ctrl` + `F`. 