# CocoFlix API और Movie Playback Flow

यह document `backup/cocoflix` के decompiled APK source, runtime Logcat और browser में मिले API responses के निरीक्षण पर आधारित है। Decompiled source obfuscated है, इसलिए class और method names मूल source जैसे readable नहीं हैं।

## निष्कर्ष

CocoFlix एक native Android Java/XML app है। App की movie list APK में hardcoded नहीं है। Firebase Remote Config से API base URL लिया जाता है और उसके आगे छोटे endpoint suffix जोड़कर catalogue feeds fetch की जाती हैं।

वर्तमान verified base URL:

```text
https://kiox.pages.dev/
```

Primary homepage feed:

```text
https://kiox.pages.dev/f
```

अन्य home-section endpoints:

```text
https://kiox.pages.dev/ms
https://kiox.pages.dev/mh
https://kiox.pages.dev/mb
```

Root URL `https://kiox.pages.dev/` का browser में page न दिखाना सामान्य है। API host पर केवल specific routes उपलब्ध हो सकते हैं।

## Launch Flow

```text
App launch
    ↓
Splash Activity
    ↓
Firebase Remote Config fetch
    ↓
Remote Config parameter: api
    ↓
Decoded base URL: https://kiox.pages.dev/
    ↓
Main Activity
    ↓
Base URL + endpoint suffix
    ↓
OkHttp GET request
    ↓
JSON records
    ↓
RecyclerView और slider में content
```

`Splash` launcher activity है। Phone पर वह `Main` और Android TV पर `MainTv` खोलती है। Remote Config की `api` value activities को दी जाती है।

## Dynamic API URL निर्माण

App conceptually इस प्रकार final URLs बनाती है:

```java
String baseUrl = "https://kiox.pages.dev/";

String featuredFeed = baseUrl + "f";
String sectionMs = baseUrl + "ms";
String sectionMh = baseUrl + "mh";
String sectionMb = baseUrl + "mb";
```

इससे final URLs बनते हैं:

```text
https://kiox.pages.dev/f
https://kiox.pages.dev/ms
https://kiox.pages.dev/mh
https://kiox.pages.dev/mb
```

## Homepage Feed

`GET https://kiox.pages.dev/f` एक JSON object देता है, जिसमें `records` array होता है:

```json
{
  "records": [
    {
      "so": 21900,
      "t": "Example Movie",
      "g": "Action,Drama",
      "y": 2025,
      "q": "HD",
      "c": "https://image.tmdb.org/t/p/original/example.jpg",
      "u": "encoded-playback-value",
      "d": ""
    }
  ]
}
```

Observed fields का अर्थ:

| Field | अनुमानित अर्थ |
|---|---|
| `records` | Content records की list |
| `so` | Content/source ID |
| `t` | Title |
| `g` | Genre |
| `y` | Release year |
| `q` | Quality tag, जैसे `HD` या `CAM` |
| `s` | Season या section-specific value |
| `co` | Content classification/type |
| `c` | Cover/poster URL |
| `u` | Encoded playback या embed URL value |
| `d` | Download-related value |
| `f` | Feature/fetch flag |
| `l` | Link/live-related flag |
| `tg` | Rating या display tag |
| `ci` | Secondary image/content value |

Fields अलग feeds या content types में थोड़ा बदल सकते हैं।

## Poster Image कैसे दिखती है

Poster URL `c` field में backend से आता है:

```json
{
  "c": "https://image.tmdb.org/t/p/original/example.jpg"
}
```

Flow:

```text
/f response
    ↓
record.c
    ↓
Glide image request
    ↓
Movie card पर poster
```

TMDB poster URL browser में सीधे खुलना expected है। Poster app में per-movie hardcoded नहीं है; backend response से dynamically मिलता है।

## Movie Playback URL कैसे मिलता है

Actual movie URL `https://kiox.pages.dev` में movie ID जोड़कर नहीं बनता। Playback information selected record के `u` field में encoded रूप में आती है:

```text
Catalogue API → record.u → decode → playback/embed URL
```

इसलिए निम्न pattern current implementation से सिद्ध नहीं है:

```text
https://kiox.pages.dev/movie/21900
```

इसके बजाय app selected record को `MovieDetails` तक भेजती है:

```java
intent.putExtra("title", title);
intent.putExtra("cover", coverUrl);
intent.putExtra("url", encodedPlaybackValue);
```

## Playback Value Decoder

`p0.java` में alternating XOR keys इस्तेमाल होती हैं:

```java
public static String decode(String value) {
    char[] key = {12425, 12327};
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < value.length(); i++) {
        result.append((char) (value.charAt(i) ^ key[i % 2]));
    }

    return result.toString();
}
```

यह reversible obfuscation है, encryption या authorization boundary नहीं।

## Player Selection

Decoded value के आधार पर app playback method चुनती है:

```text
HLS/.m3u8     → Media3 ExoPlayer + HlsMediaSource
DASH/.mpd     → DashMediaSource
RTSP          → RtspMediaSource
Direct media  → Progressive media source
Embed/web URL → WebView
```

यदि encoded field में multiple server values हों, app उन्हें split करके server-selection dialog दिखाती है।

## Complete Data Flow

```text
Firebase Remote Config
    ↓ api
https://kiox.pages.dev/
    ↓ + f
https://kiox.pages.dev/f
    ↓
records[]
    ├── t  → title
    ├── c  → poster URL → Glide
    ├── u  → encoded playback value → XOR decode
    └── so → content ID
                         ↓
                  ExoPlayer/WebView
```

## Browser Testing

Catalogue endpoint को browser या PowerShell में test किया जा सकता है:

```powershell
Invoke-RestMethod "https://kiox.pages.dev/f"
```

HTTP status और headers देखने के लिए:

```powershell
Invoke-WebRequest "https://kiox.pages.dev/f"
```

Root route blank या `404` हो सकती है, जबकि `/f` valid JSON दे सकती है।

## Security Findings

वर्तमान `/f` endpoint बिना authentication catalogue और encoded `u` field expose करता है। मुख्य risks:

- Catalogue को कोई भी browser से enumerate कर सकता है।
- Encoded playback value reversible है।
- XOR encoding access control नहीं है।
- Permanent playback URLs leak होने पर सीधे reuse हो सकते हैं।
- Endpoint secrecy पर निर्भर रहना सुरक्षित नहीं है।

Poster URLs public होना सामान्य है। Playback credentials, permanent media URLs और privileged keys public feed में नहीं होने चाहिए।

## Recommended Production Architecture

Public catalogue response में केवल metadata दें:

```json
{
  "id": 21900,
  "title": "Example Movie",
  "posterUrl": "https://image.tmdb.org/t/p/original/example.jpg",
  "year": 2025,
  "quality": "HD"
}
```

Play action पर protected endpoint call करें:

```http
POST /v1/content/21900/playback
Authorization: Bearer <short-lived-user-token>
```

Backend authorization के बाद short-lived signed URL दे:

```json
{
  "playbackUrl": "https://licensed-cdn.example/master.m3u8?...",
  "expiresIn": 300
}
```

Production controls:

- Server-side authentication और authorization
- Short-lived signed playback URLs
- Rate limiting और abuse monitoring
- Release build में URL/token logging बंद
- APK में service/admin keys नहीं
- Secrets और private keys केवल backend पर
- Licensed और authorized media sources का उपयोग

## Source References

- `backup/cocoflix/resources/AndroidManifest.xml`
- `backup/cocoflix/sources/com/app/bluesun/view/Splash.java`
- `backup/cocoflix/sources/com/app/bluesun/view/Main.java`
- `backup/cocoflix/sources/com/app/bluesun/view/MainTv.java`
- `backup/cocoflix/sources/com/app/bluesun/view/MovieDetails.java`
- `backup/cocoflix/sources/com/app/bluesun/view/Player.java`
- `backup/cocoflix/sources/com/app/bluesun/view/Web.java`
- `backup/cocoflix/sources/defpackage/mc4.java`
- `backup/cocoflix/sources/defpackage/io2.java`
- `backup/cocoflix/sources/defpackage/jo2.java`
- `backup/cocoflix/sources/defpackage/p0.java`
- `backup/cocoflix/resources/res/xml/config.xml`
