# StreamVault

Frontend Netflix-style para Android em Kotlin + Jetpack Compose. Open-source e pronto para conectar à sua própria API de streaming.

## Stack

- Kotlin + Jetpack Compose + Material 3
- ExoPlayer / Media3 — HLS, DASH, SmoothStreaming, MP4, MKV, RTSP, RTMP
- DRM Widevine, PlayReady, ClearKey
- Hilt (DI) · Room (banco local) · Retrofit + OkHttp (REST) · WebSocket nativo
- Google Cast (Chromecast) · DataStore Preferences
- Criptografia AES-256-GCM via Android Keystore
- Proteção de captura de tela (`FLAG_SECURE`)

---

## Estrutura

```
app/src/main/java/com/streamvault/
├── data/
│   ├── local/          # Room DB, DAOs, DataStore
│   ├── models/         # Data classes (domain)
│   ├── remote/         # Retrofit service, DTOs, Mappers, WebSocket
│   └── repository/     # StreamRepository
├── di/                 # Hilt modules
├── cast/               # Google Cast integration
├── player/             # ExoPlayer engine, PlaybackService, PlayerActivity
├── security/           # TokenManager (AES), ScreenshotProtection
└── ui/
    ├── components/      # Cards, rows, banners, shimmer, top bar
    ├── screens/         # Setup, Profiles, Home, Search, Detail, Player, MyArea, Notifications
    └── theme/           # StreamVaultTheme, StreamColors, Typography
```

---

## Como conectar sua API

### 1. Abrir o app → tela de Setup

Preencha:
| Campo | Descrição |
|---|---|
| **URL Base** | `https://api.seusite.com` |
| **Tipo de conexão** | `REST` ou `WEBSOCKET` |
| **WebSocket URL** | `wss://api.seusite.com/ws` (se WS) |
| **API Key** | Enviada como header `X-API-Key` |
| **URL Licença DRM** | Ex: `https://widevine.seusite.com/license` |

### 2. Autenticação

O app faz `POST` para o endpoint que você configurar (padrão `/auth/login`) com:
```json
{ "username": "...", "password": "..." }
```
Espera em resposta:
```json
{ "token": "...", "refresh_token": "...", "expires_in": 3600 }
```
O token é criptografado com **AES-256-GCM** no Android Keystore antes de salvar.

### 3. Endpoints esperados

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/home` | Array de `HomeRow` |
| `GET` | `/content/:id` | `MediaItem` |
| `GET` | `/search?q=` | Array de `MediaItem` |
| `GET` | `/profiles` | Array de `UserProfile` |
| `GET` | `/notifications` | Array de `NotificationItem` |

#### Exemplo `MediaItem`
```json
{
  "id": "tt1234",
  "title": "Meu Filme",
  "description": "Sinopse...",
  "posterUrl": "https://cdn.../poster.jpg",
  "backdropUrl": "https://cdn.../backdrop.jpg",
  "logoUrl": "https://cdn.../logo.png",
  "year": 2024,
  "rating": "18",
  "imdbRating": 8.2,
  "duration": 120,
  "genres": ["Ação", "Drama"],
  "cast": ["Ator 1", "Atriz 2"],
  "director": "Diretor X",
  "type": "MOVIE",
  "streamUrl": "https://cdn.../video.m3u8",
  "videoFormat": "HLS",
  "drmScheme": "WIDEVINE",
  "drmLicenseUrl": "https://widevine.../license",
  "drmHeaders": { "X-Custom-Header": "valor" },
  "subtitles": [
    { "language": "pt", "label": "Português", "url": "https://.../sub.vtt", "mimeType": "text/vtt" }
  ],
  "audioTracks": [
    { "language": "pt", "label": "Português", "channelCount": 2 }
  ],
  "trailerUrl": "https://cdn.../trailer.mp4",
  "isFeatured": true,
  "isTopTen": true,
  "rank": 1,
  "maturityRating": "16",
  "tags": ["Novidade"]
}
```

#### Exemplo `HomeRow`
```json
{
  "id": "row1",
  "title": "Séries em Alta",
  "displayType": "PORTRAIT",
  "items": [ /* array de MediaItem */ ]
}
```
`displayType`: `PORTRAIT` | `LANDSCAPE` | `NUMBERED` | `HERO`

### 4. WebSocket (opcional)

Se `connectionType = WEBSOCKET`, o app conecta em `wsUrl` e escuta eventos:
```json
{ "type": "PRESENCE_UPDATE", "data": { ... } }
{ "type": "HOME_UPDATE",     "data": [ /* HomeRow[] */ ] }
```
Implemente `StreamWebSocketClient` para reagir a esses eventos no ViewModel.

---

## Formatos de vídeo suportados

| Formato | `videoFormat` value |
|---------|---------------------|
| HLS (`.m3u8`) | `HLS` |
| MPEG-DASH (`.mpd`) | `DASH` |
| SmoothStreaming (`.ism`) | `SMOOTH_STREAMING` |
| MP4 progressivo | `MP4` |
| MKV | `MKV` |
| RTSP | `RTSP` |
| RTMP | `RTMP` |

## DRM suportado

| Esquema | `drmScheme` value |
|---------|-------------------|
| Widevine L1/L3 | `WIDEVINE` |
| PlayReady | `PLAYREADY` |
| ClearKey | `CLEARKEY` |
| Sem DRM | `NONE` |

---

## Proteção de captura de tela

Ativa/desativa `FLAG_SECURE` na tela do player. Configurável em **Minha área → Proteção de tela**.

Para ativar por padrão no `build.gradle.kts`:
```kotlin
buildConfigField("Boolean", "SCREENSHOT_PROTECTION", "true")
```

---

## Build local

```bash
./gradlew :app:assembleDebug
```

### Build release (com keystore)

```bash
KEYSTORE_PASSWORD=xxx KEY_ALIAS=xxx KEY_PASSWORD=xxx \
./gradlew :app:assembleRelease
```

### GitHub Actions

Push para `main` gera APK debug automaticamente.
Dispatch manual com `buildType=release` gera APK + cria release no GitHub.

Secrets necessários:
- `KEYSTORE_BASE64` — keystore em base64 (`base64 keystore.jks`)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

---

## Licença

MIT — use, modifique e distribua livremente.
