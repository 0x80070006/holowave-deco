# Interopérabilité HOLOWAVE ↔ NouTube

Rapport établi le 2026-11-01 à partir du **code source de NouTube v0.7.4** (commit `a2eeb7b`, https://github.com/nonbili/NouTube), lu en lecture seule. Aucun code de NouTube n'est copié dans HOLOWAVE (NouTube est sous AGPL-3.0, HOLOWAVE sous MIT, et les piles techniques diffèrent). L'intégration se limite à des interfaces publiques d'Android : intents, fichiers en stockage partagé, fichiers que l'utilisateur choisit lui-même.

Légende : ✅ lu dans le code ou **mesuré sur appareil** · ⚠️ déduit, à vérifier · ❓ non vérifiable sans échantillon réel.

Les points marqués « mesuré » ont été vérifiés le 2026-11-01 sur un **Pixel 9a (Android 17) avec NouTube 0.7.4** installé (la même version que le code lu).

## 1. Ce qu'est NouTube

- Paquet Android `jp.nonbili.noutube`, distribué sur F-Droid et GitHub Releases. ✅
- App Expo / React Native qui enveloppe `m.youtube.com` et `music.youtube.com` dans une WebView, plus un module natif Kotlin (`modules/nou-tube-view`) qui embarque **yt-dlp** (`NouYtDlp.kt`) pour les téléchargements. ✅
- Permissions déclarées : `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `POST_NOTIFICATIONS`, `WAKE_LOCK` (+ réseau via React Native). ✅

## 2. Points d'entrée (intents)

| Mécanisme | Détail | Source |
|---|---|---|
| `ACTION_VIEW` | URL `https` des hôtes `youtube.com`, `www.youtube.com`, `m.youtube.com`, `music.youtube.com`, `youtu.be` ; catégories `BROWSABLE` + `DEFAULT` | `app.config.ts` ✅ |
| `ACTION_SEND` | type `text/*` (partage d'un lien) | `app.config.ts` (expo-sharing) ✅ |
| Schéma `noutube://` | réécrit en `https://` avant traitement : `noutube://music.youtube.com/watch?v=ID` est équivalent | `lib/supported-url.ts` ✅ |

**Conséquence pour HOLOWAVE :**
- Ouvrir un morceau : `Intent(ACTION_VIEW, "https://music.youtube.com/watch?v=<id>")` avec `setPackage("jp.nonbili.noutube")`. Pas de sélecteur d'application, et si NouTube est absent l'appel échoue proprement (`ActivityNotFoundException`) au lieu d'ouvrir un navigateur.
- Ouvrir une recherche : `https://music.youtube.com/search?q=<texte encodé>`. ✅ **Mesuré** : `am start -a VIEW -d "https://music.youtube.com/search?q=…" -p jp.nonbili.noutube` ouvre directement la recherche, onglet « YT MUSIC », sans sélecteur d'application.
- Détection d'installation : `<queries><package android:name="jp.nonbili.noutube"/></queries>` dans le manifeste (pas de permission `QUERY_ALL_PACKAGES`).
- **Il n'existe aucun intent qui déclenche un téléchargement.** ✅ Le téléchargement n'est lancé que depuis l'interface de NouTube (fenêtre « Outils », choix d'un format, une vidéo à la fois, `ToolsModal.tsx` → `downloadVideo`). Le code ne contient aucun mode de téléchargement par lot.

## 3. Historique : ce qui est (et n'est pas) exposé

- L'historique est stocké **en privé** dans MMKV (`states/history.ts`, clé `history`). Le bac à sable Android interdit à HOLOWAVE de le lire. ✅
- Il est plafonné à **1000 entrées** et **dédoublonné par vidéo** (`getKey = videoId|home`) : une vidéo revue remonte en tête au lieu de créer une nouvelle entrée. Chaque entrée porte `videoId, url, title, thumbnail, duration, current, updatedAt`. ✅
- **Il n'y a donc aucun compteur d'écoutes**, seulement la date de la dernière lecture et la position atteinte. ✅
- Aucune fonction d'**export de l'historique** : `lib/export.ts` n'exporte que les **favoris** (CSV `url,title,folder,tab,thumbnail,channel_id`), et il existe une sauvegarde JSON des **réglages**. ✅

> **Conclusion majeure : un « Top 100 par nombre d'écoutes » ne peut pas être calculé à partir de NouTube.** Ni l'historique (privé, 1000 entrées, sans compteur), ni les exports disponibles (favoris, réglages) ne contiennent cette information.

### Sources réalistes d'un classement

1. **Google Takeout** — `Takeout/YouTube et YouTube Music/historique/watch-history.json` : un événement par lecture, avec horodatage. C'est la seule source qui permet de compter. Les entrées de YouTube Music se distinguent par leur en-tête. ❓ *Format décrit d'après l'export public de Google (`header`, `title` commençant par « Watched / Vous avez regardé », `titleUrl`, `subtitles[].name` pour la chaîne, `time`) : à valider sur un vrai fichier de l'utilisateur avant d'écrire le parseur.* HOLOWAVE le lit localement, sans réseau ; c'est l'utilisateur qui demande l'export à Google.
2. **Export des favoris NouTube** (CSV ci-dessus) : donne l'ensemble des morceaux que l'utilisateur a gardés, **sans compteur**. Utile comme signal positif et pour rapprocher des titres.
3. **Écoutes dans HOLOWAVE** : les morceaux téléchargés via NouTube, une fois dans la bibliothèque, accumulent leurs propres écoutes.

Le Takeout est donc la voie principale pour un classement chiffré ; sans lui, le « Top » devient « mes favoris NouTube, classés par récence ».

## 4. Téléchargements : où, sous quel nom, dans quel format

D'après `NouYtDlp.kt` (`downloadVideo`, `publishToDownloads`) ✅ :

- yt-dlp télécharge d'abord dans le cache privé de NouTube, puis le fichier est **publié dans `MediaStore.Downloads`** avec `RELATIVE_PATH = Download/`, pendant la copie `IS_PENDING = 1` puis `0`.
- Modèle de nom dans le code : **`%(title)s [%(format_id)s].%(ext)s`**. Le nom contient le **titre**, **pas l'identifiant de la vidéo**.
- ✅ **Mesuré** : le seul téléchargement présent sur l'appareil de test, un MP3, s'appelle simplement `<titre>.mp3` (sans suffixe `[format_id]`) : **le modèle de nom n'est pas fiable**, il ne faut pas s'appuyer dessus. Android le déclare propriétaire de NouTube (`owner_package_name = jp.nonbili.noutube`, `relative_path = Download/`, `media_type = 2`, durée renseignée).
- ✅ **Mesuré** : ce MP3 ne contient **aucune balise** hormis le champ encodeur (`TSSE = Lavf…`) : ni titre, ni artiste, ni URL d'origine, ni pochette, malgré `--add-metadata --embed-thumbnail` dans le code.
- Options : `--no-playlist`.
  - Format « MP3 » : `--extract-audio --audio-format mp3 --add-metadata --embed-thumbnail` → un `.mp3` avec tags et pochette.
  - Formats « vidéo / audio » prédéfinis : fusion en `mp4`.
  - Autres identifiants : le conteneur est laissé au choix de yt-dlp (`mp4`, `webm`, `m4a`, `opus`…).
- Le réglage `downloadPath` existe dans l'interface, mais le chemin Android montré ci-dessus publie toujours dans `Download/`. ⚠️ À confirmer sur appareil.

**Conséquences pour HOLOWAVE :**

1. **Il n'y a pas de dossier dédié NouTube** : c'est `Download/`, partagé avec tout le reste. Ajouter `Download/` comme « dossier source » indexerait n'importe quel fichier audio téléchargé par ailleurs. Il vaut mieux **identifier les fichiers NouTube** par :
   - `MediaStore.…OWNER_PACKAGE_NAME == jp.nonbili.noutube` (API 29+) ⚠️ la colonne n'est renseignée que si Android autorise l'app à la voir : à tester sur appareil, avec la déclaration `<queries>` ;
   - à défaut, le motif de nom `… [<format_id>].<ext>` dans `Download/`.
2. **Le rapprochement avec la file de récupération ne peut pas se faire par identifiant vidéo** (absent du nom) **ni par balises** (absentes dans l'échantillon mesuré). Il se fera par **titre normalisé (nom de fichier) + durée** (tolérance de quelques secondes), l'artiste n'étant pas disponible côté fichier. Les homonymes sont à départager par la durée, puis par l'utilisateur.
3. **Les téléchargements vidéo** (`mp4` / `webm` avec image) sont rangés par Android dans l'index *vidéo*. Le filtre actuel de HOLOWAVE (v2.2) ne garde que les fichiers sans image : il faudra l'étendre aux seuls fichiers dont le propriétaire est NouTube, et les lire en **audio seul** (piste vidéo désactivée dans Media3). Les vidéos personnelles de l'utilisateur restent ignorées.
4. **Pas d'automatisation possible** : un téléchargement exige plusieurs gestes dans NouTube (ouvrir, Outils, choisir le format). Un lot de 100 morceaux représente donc plusieurs centaines de gestes. Le parcours devra être pensé comme une **file de récupération guidée** par morceau, avec des lots raisonnables (ex. 10 à 20 à la fois) et l'option de ne récupérer que ce qui manque.

## 5. Conditions d'utilisation et licence

- Télécharger depuis YouTube peut contrevenir aux conditions d'utilisation de YouTube. HOLOWAVE n'embarque aucun téléchargeur et ne contourne aucune protection : les téléchargements sont faits par NouTube, à l'initiative de l'utilisateur. L'écran concerné affichera une mention sobre le rappelant.
- NouTube est sous AGPL-3.0 : HOLOWAVE n'en reprend ni code ni ressources, et ne communique avec lui que par intents publics et fichiers.

## 6. Ce que cela change pour la conception

| Idée du cahier des charges | Faisable ? | Adaptation |
|---|---|---|
| Lire l'historique NouTube directement | Non (bac à sable) | Import de fichier choisi par l'utilisateur |
| Export d'historique NouTube | Non (n'existe pas) | Takeout `watch-history.json`, ou export des favoris |
| Top 100 par nombre d'écoutes | Seulement via Takeout | Sans Takeout : « favoris NouTube » classés par récence + écoutes HOLOWAVE |
| Dossier NouTube comme source surveillée | Partiellement | Identifier les fichiers par propriétaire (`OWNER_PACKAGE_NAME`) ou par nom, dans `Download/` |
| Rapprochement par identifiant vidéo | Non | Titre normalisé + artiste + durée |
| Ouvrir un morceau / une recherche dans NouTube | Oui | `ACTION_VIEW` explicite vers `jp.nonbili.noutube` |
| Déclencher les téléchargements | Non | File guidée, un morceau à la fois |
| Lecture audio seule des téléchargements vidéo | Oui | Piste vidéo désactivée, limité aux fichiers NouTube |

## 7. Vérifications sur le Pixel 9a (NouTube 0.7.4)

| Point | Résultat |
|---|---|
| Identifier les fichiers NouTube par propriétaire | ✅ `OWNER_PACKAGE_NAME` est lisible dans MediaStore (testé en ligne de commande ; à re-tester depuis l'application, avec `<queries>`) |
| Dossier, type, index | ✅ `Download/`, indexé dans l'**audio** pour un MP3 |
| Nom de fichier | ✅ `<titre>.mp3`, sans identifiant de format ni de vidéo |
| Balises intégrées | ✅ aucune (titre, artiste, URL, pochette absents) |
| Ouvrir une recherche YT Music dans NouTube | ✅ intent explicite, ouverture directe |
| Téléchargement vidéo (mp4/webm) : index et propriétaire | ❓ non mesuré (aucun échantillon sur l'appareil) : à faire en P4 avec un téléchargement vidéo réel |
| `watch-history.json` réel | ❓ à fournir par l'utilisateur avant d'écrire le parseur |

### Points découverts au passage (impact sur la conception)

- **Android 14+ propose un « accès limité » aux photos et vidéos** lorsqu'une app demande `READ_MEDIA_VIDEO`. Avec l'accès limité, seuls les éléments choisis un par un sont visibles : l'option WebM/3GP et la lecture des téléchargements vidéo NouTube exigent « Tout autoriser ». L'interface doit l'expliquer avant la demande.
- **AIFF et WMA ne peuvent pas être lus** par Android ni par Media3 (aucun extracteur, durée `NULL` dans l'index). Les 12 autres formats testés se lisent (mp3, m4a, m4b, aac, flac, ogg, opus, wav, mka, amr, 3gp, webm). Les titres AIFF/WMA sont désormais signalés « non pris en charge » et exclus des files.
