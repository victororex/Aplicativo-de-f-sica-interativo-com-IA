Avatar model setup
==================

SceneView/Filament loads glTF/GLB models reliably. The source file `avatar.obj`
is kept here for reference, but the runtime component expects:

    app/src/main/assets/models/avatar.glb

Recommended Blender conversion:

1. Open Blender.
2. File > Import > Wavefront (.obj) and choose `avatar.obj`.
3. If you have the matching `renatao.mtl`, keep it next to the OBJ before import.
4. Adjust orientation/scale if needed.
5. File > Export > glTF 2.0.
6. Choose Format: GLB, enable selected objects if desired, and save as `avatar.glb`.

Keep the GLB small for mobile. Aim for compressed textures and a modest polygon
count because this avatar is embedded inside Compose screens, not a full-screen
3D scene.
