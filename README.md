# Tool conversion

## v1.42.0 Tools download page
https://github.com/google/filament/releases/tag/v1.42.0

## matc

```
matc -p mobile -a opengl -o <output-path/filename.filamat> <input-path/filename.mat>
```

eg
```
matc -p mobile -a opengl -o app/src/main/assets/baked_color.filamat app/src/main/materials/baked_color.mat
```

## cmgen
Generates m0_nx.rgb32f files from .hdr environment file

```
cmgen -x app/src/main/assets/envs app/src/main/assets/envs/flower_road_no_sun_2k.hdr
```

## filamesh

.obj or other 3d model to .filamesh

```
filamesh source_mesh destination_mesh
```

eg
```
filamesh app/src/main/assets/models/shader_ball.obj app/src/main/assets/models/shader_ball.filamesh
```
