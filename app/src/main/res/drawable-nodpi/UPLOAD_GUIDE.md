# YUBERA Android — upload guide for visual assets

Upload the five original image files into this exact folder:

`app/src/main/res/drawable-nodpi/`

Do not upload complete screen mockups. The welcome/login and startup screens are built in Jetpack Compose; only their clean background artwork is stored here.

| Purpose | Exact repository filename | Original source file in the supplied archive |
|---|---|---|
| Wide section banner | `yubera_banner.png` | `1D6140AC-3ACC-46F1-8392-2B4FDE0FF97A.PNG` |
| Startup/loading background | `yubera_splash_background.png` | `5DDEF5BA-61D8-4147-91BD-3BEDC3DDCCED.PNG` |
| Sign-in / registration background | `yubera_auth_background.png` | `66777F03-CFBD-49C1-B8E0-10230752C9BA.PNG` |
| Transparent YUBERA logo | `yubera_logo.png` | `E84CEB15-DC6D-4FC4-B1D7-2C21837CB7B9.PNG` |
| App icon source artwork | `yubera_app_icon_source.png` | `6FEC0088-C034-4715-A64A-91075C10984F 2.PNG` |

## Where each asset appears

- `yubera_banner.png`: Catalog, My, News, and Profile section headers. It is one shared asset, not four copies. Compose will use controlled crop/position per screen.
- `yubera_splash_background.png`: branded startup screen only.
- `yubera_auth_background.png`: welcome, sign-in, and registration flow background. Buttons, text, and navigation are native Compose elements.
- `yubera_logo.png`: brand mark in app headers and relevant branded screens. Keep its transparent pixels intact.
- `yubera_app_icon_source.png`: source artwork for generating Android launcher icon resources. It is not itself the final adaptive icon; its existing rounded-square artwork must not be masked a second time without visual validation.

## Upload rules

1. Use the exact lowercase filenames in the middle column, including the `.png` extension.
2. Keep the original image content and transparency. Do not screenshot, recompress, crop, or resize the files before upload.
3. Upload only these five assets. The complete welcome/catalog/My/News/Profile screenshots are references and must not be placed in this resource folder.
4. Keep the existing `yubera_upload_placeholder.xml`; it is a temporary transparent anchor and is not one of the five images.
