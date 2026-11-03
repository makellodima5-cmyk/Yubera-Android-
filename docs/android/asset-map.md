# YUBERA Android — Asset Map

**Source archive:** `Архив.zip` supplied in the project conversation  
**Inventory date:** 2026-11-03  
**Inventory result:** 15 actual PNG files, 14 unique image contents, one byte-identical duplicate. ZIP metadata sidecars under `__MACOSX/` are not image assets.

## Inventory and visual mapping

| # | Original filename | Dimensions / mode | Size | SHA-256 | Visual role / planned use |
|---:|---|---|---:|---|---|
| 1 | `5DDEF5BA-61D8-4147-91BD-3BEDC3DDCCED.PNG` | 1024×1536 RGB | 1,752,148 B | `f5498fe3a37ac82bc0b595c053940a9cc0bfa25187044c983731e3a14d93e545` | Portrait illustration with two characters, blossoms and dark wave at the bottom. Planned startup background: `app/src/main/res/drawable-nodpi/yubera_splash_background.png`. |
| 2 | `F153CA22-1EEC-48BF-9CD9-90CE0E2B5CCF.PNG` | 1024×1536 RGB | 1,662,690 B | `ca585cf6e8404bd391d6cfebfbca99266ee124d2207edf8c75eef8f951ab4a04` | Full branded splash composition with baked YUBERA mark and loading ring. Reference only; do not use as the live screen because the logo and progress indicator must be native UI elements. |
| 3 | `1D6140AC-3ACC-46F1-8392-2B4FDE0FF97A.PNG` | 2172×724 RGB | 1,854,476 B | `22af92ca1073ee9b06ec9d8a43af27f9ef079b37630ec5523ed0e84d6f563106` | Wide character artwork, 3:1 aspect ratio. Planned wide banner: `app/src/main/res/drawable-nodpi/yubera_banner.png`. Preserve aspect ratio; do not stretch into portrait cards. |
| 4 | `5BBDBD7E-D070-4CF0-961C-A50889CB8792.PNG` | 1024×1536 RGB | 2,122,166 B | `c92cf9247f6d51c8ff65615934d96cd7822b2835d9487bb9fee2f70ae8c24b13` | Full Catalog screen screenshot. Design reference only; implement the screen in Compose. |
| 5 | `6FEC0088-C034-4715-A64A-91075C10984F 2.PNG` | 1254×1254 RGB | 1,468,813 B | `b99450f384440c371974b5b021cf11b2435a709b3971209e23132972532da3d9` | Square YUBERA app icon artwork. Canonical icon source. Planned retained source copy: `app/src/main/res/drawable-nodpi/yubera_app_icon_source.png`. It has a baked rounded-square background; adaptive-icon masking must be checked before using it as a foreground layer. |
| 6 | `5FAE7F5D-EA2C-4955-A1D9-FCC1CB764FFE.PNG` | 1024×1536 RGB | 1,763,623 B | `fc7522741ebcf9df2f91dcc8b9b071184b52c95a1c4746fe7b625946d72c7a27` | Full My screen screenshot. Design reference only; implement in Compose. |
| 7 | `6FEC0088-C034-4715-A64A-91075C10984F.PNG` | 1254×1254 RGB | 1,468,813 B | `b99450f384440c371974b5b021cf11b2435a709b3971209e23132972532da3d9` | Byte-identical duplicate of #5. Keep as an archive duplicate only; do not create a second Android resource. |
| 8 | `24B937CA-79B0-43BE-BC15-6248BFD7A48D.PNG` | 1024×1536 RGB | 1,636,252 B | `e907d601c9fa03f6d9dfd1ae9adafefca38488f786ce12e53f609c6a0f5da086` | Full sign-in screen screenshot. Design reference only. Background artwork may be reused separately only from the original standalone art source. |
| 9 | `08C9E0C8-D38D-455A-9386-6F2DD93A91BB.PNG` | 1024×1536 RGB | 2,202,972 B | `6d159eada9f9434a44aae657869af4de1ff26965ba57d53c04db845fc7dd6007` | Full News screen screenshot. Design reference only; implement in Compose. |
| 10 | `38FC7EE0-F246-4538-8210-13960907ACFD.PNG` | 1199×1312 RGB | 1,989,025 B | `d5da803d22110dad7655e859a25ef8ce28b52c3315ae5780ddf3ac11bf4afe13` | Composition of multiple Profile/settings screens. Design reference only; do not use as a single UI image. |
| 11 | `898FFC8D-4BB3-435C-B5E8-141EFD6845C3.PNG` | 1024×1536 RGB | 1,680,229 B | `6b65dbd70bb2dba7937b3a60ac540f49c9040e8e1c9ad9fa15c55fdb514402e7` | Full registration screen screenshot. Design reference only. |
| 12 | `66777F03-CFBD-49C1-B8E0-10230752C9BA.PNG` | 1024×1536 RGB | 1,686,822 B | `c44dc0605a22504324b044e90efb6bf56c78f24d6808040c96044cdbc0f9b6fd` | Standalone portrait character illustration with dark lower region suitable for overlaying native controls. Planned authentication background: `app/src/main/res/drawable-nodpi/yubera_auth_background.png`. |
| 13 | `E8CA3A80-61B5-41DF-BD58-A975D887050F.PNG` | 1024×1536 RGB | 2,070,861 B | `ea664d7ac9f642b51b50faf512b50e08697c862c29c4f1e4a04a02809a85366c` | Full Home screen screenshot. Design reference only; implement in Compose. |
| 14 | `CCE96AB8-F5A0-41AA-87CD-72E27DB60712.PNG` | 1024×1536 RGB | 1,735,661 B | `53d5ef904b6d6b10a57a088ee883df16f0c22c6d5e807826538508d94d786f64` | Full welcome screen screenshot. Design reference only. |
| 15 | `E84CEB15-DC6D-4FC4-B1D7-2C21837CB7B9.PNG` | 2172×724 RGBA | 506,780 B | `54b135ef0a225ccf937923ad63cc834ea20c33e0c5d7ea9a3f8111c40d0fbd8a` | Transparent YUBERA emblem and wordmark. Planned logo resource: `app/src/main/res/drawable-nodpi/yubera_logo.png`. Preserve RGBA transparency. |

## Resource handling rules

1. Preserve the original files unchanged. The planned resource names above are destination names, not edits to the source files.
2. Use the standalone art (#1 and #12), standalone banner (#3), transparent logo (#15) and canonical icon source (#5) as candidate production assets.
3. The complete screenshots (#4, #6, #8, #9, #10, #11, #13, #14) are layout references only. Rebuild visible labels, controls, cards, bars and other UI with native Compose.
4. The branded splash composition (#2) is also a reference, not a production background: logo and progress must remain independently rendered app UI.
5. Do not crop, stretch or distort the 3:1 banner to fill portrait layouts. Use an appropriate crop or a different asset.
6. The icon source includes rounded corners baked into the image. A valid adaptive launcher icon needs a deliberate foreground/background composition and must be visually checked against Android launcher masks. Do not assume that copying the source square alone completes the adaptive icon.
7. The inventory ignores macOS `__MACOSX/._*` metadata entries; they are not additional PNG artwork.

## User upload handoff

When the resource-integration commit is ready, the user will be given the exact source filenames and target repository folders. No new artwork is requested at this inventory stage. The source archive is already available to the implementation process.
