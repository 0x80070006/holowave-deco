# HOLOWAVE v3 — audit de la v2.2.1 et plan par phases

Document de travail, 2026-11-01. À valider avant toute ligne de code de la v3.

## 1. Audit de l'existant (v2.2.1)

### Architecture actuelle

```
HolowaveApp            conteneur de dépendances manuel (pas de DI, pas de réflexion)
data/                  MediaLibrary (MediaStore), AudioScanner, SourceFolders, PlaylistStore (JSON atomique),
                       Settings (SharedPreferences), ArtworkLoader (LRU), Track/Collections
lyrics/                Lrc, EmbeddedLyrics (ID3/FLAC), LrcLibClient (HTTPS), LyricsRepository (hors ligne d'abord)
playback/              PlaybackService (Media3 MediaSession), PlayerConnection (MediaController → StateFlow), NotificationArt
ui/                    MainViewModel (unique), HolowaveRoot (navigation maison + overlays), screens/, components/, theme/ (palette « Deco »)
```

~35 fichiers Kotlin, 26 tests unitaires (LRC, ID3/FLAC, playlists, dossiers sources, formats), APK release de 4,5 Mo, minSdk 29, compileSdk 37.

### Ce qui est solide (à conserver tel quel)

Scanner et dossiers sources, lecture Media3 en arrière-plan avec notification et écran de verrouillage, paroles hors ligne, écritures atomiques, parseurs défensifs, manifeste restreint.

### Points de friction pour la v3

| # | Constat | Conséquence |
|---|---|---|
| F1 | **Aucune base de données** : playlists en JSON, réglages en SharedPreferences. Aucun historique d'écoute. | Il faut une base pour les événements d'écoute et la provenance (P1). |
| F2 | **Couleurs codées en dur** dans l'objet `Deco` (une seule palette sombre). | Impossible d'avoir thème clair, couleur dynamique ou deux directions visuelles sans extraire des « jetons » de design (P2). |
| F3 | **Un seul `MainViewModel`** (~350 lignes) qui mélange navigation, catalogue, paroles, playlists, réglages. | À découper avant d'y ajouter accueil, recommandations et pont NouTube (P1/P2). |
| F4 | **Navigation maison** (pile de routes + overlays), pas de barre inférieure, pas de transitions partagées. | Refonte vers 4 onglets avec mini-lecteur persistant (P2). |
| F5 | **Tout le catalogue est chargé en mémoire** et regroupé à chaque changement. | Correct jusqu'à ~10 000 titres (3 600 sur le Pixel de test, fluide) ; à mesurer et à rendre incrémental si besoin (P5). |
| F6 | **Pas de Baseline Profile**, pas de mesure de démarrage. | À ajouter (P5). |
| F7 | **`INTERNET` déclaré** et recherche de paroles en ligne **activée par défaut**. | En tension avec le principe « local-first » de la v3 : décision à prendre (D1). |
| F8 | WMA / AIFF / APE : aucun décodeur Android ni Media3. | Signalés « non pris en charge » (fait en 2.2.1). Les lire exigerait un décodeur FFmpeg (D2). |
| F9 | Thème, mini-lecteur, notifications : le style Art Déco est dessiné à la main dans une dizaine de composants. | Réécriture de ces composants en P2, sans toucher à la logique. |

## 2. Rapport d'interopérabilité NouTube

Voir [`noutube-interop.md`](noutube-interop.md) (code de NouTube 0.7.4 lu, plus mesures sur un Pixel 9a). Les trois faits qui changent le plan :

1. **NouTube ne conserve aucun compteur d'écoutes** (historique privé, dédoublonné, 1000 entrées, non exportable). Un « Top 100 par nombre d'écoutes » n'est possible qu'avec un **export Google Takeout**.
2. **Les téléchargements vont dans `Download/`**, sans dossier dédié, sans balises, nommés `<titre>.mp3`. On les reconnaît grâce au **propriétaire Android** (`jp.nonbili.noutube`), pas à un dossier. Cela évite de demander l'accès à tout `Download/`.
3. **Aucun intent ne déclenche un téléchargement.** Ouvrir un titre ou une recherche dans NouTube fonctionne (testé) ; télécharger reste un geste manuel par morceau.

## 3. Plan par phases

Chaque phase : compilation, tests unitaires (26 existants + nouveaux), vérification sur le Pixel avec captures, bilan court. Aucune ne casse la précédente.

### P1 — Fondations (données)
- Base locale : événements d'écoute, provenance des morceaux (`local` / `noutube` / `online`), préférences de goût ; **migration** depuis `collections.json` et SharedPreferences (aucune perte de playlists, favoris, dossiers, réglages).
- Découpage du `MainViewModel` en modèles par écran ; extraction du dépôt de données.
- Enregistrement des écoutes (début, durée écoutée, fin naturelle, saut, réécoute) depuis `PlayerConnection`.
- Tests : migration, enregistrement des événements, règles de saut.
- *Risque à lever d'abord :* compatibilité Room/KSP avec AGP 9.4 et Kotlin 2.4. Repli : SQLite brut (`SQLiteOpenHelper`), sans dépendance, ce qui revient moins cher en poids d'APK.

### P2 — Design system et navigation
- **Trois pistes visuelles** (captures sur le Pixel) à choisir avant de généraliser :
  - *A — « Lampe ambrée »* : anthracite brun, ambre et crème, cartes douces, grain discret.
  - *B — « Papier crème »* : clair dominant, encre brune, accents terracotta (le thème sombre en alternative).
  - *C — « Pochette vivante »* : fond entièrement dérivé des couleurs de la pochette en cours (Material You dynamique).
- Jetons de design (couleurs, formes, mouvements) ; thèmes clair et sombre ; couleur dynamique depuis la pochette ; réglage « réduire les animations » respecté.
- Navigation inférieure à 4 onglets (Accueil, Bibliothèque, Recherche, Réglages), mini-lecteur persistant balayable, transition pochette → lecteur, file d'attente réorganisable par glisser-déposer.
- Le panneau de scan est conservé, redessiné. Mise en page adaptative (tablette, pliable), accessibilité (cibles 48 dp, TalkBack, police agrandie).

### P3 — Moteur de goûts et mixes (100 % local)
- Scores d'affinité (titre, artiste, genre) avec décroissance exponentielle (demi-vie réglable, 30 jours par défaut) ; graphe de co-écoute ; contexte (heure, jour) ; re-classement de diversité (MMR) ; fraîcheur ; **raison stockée pour chaque suggestion**.
- Mixes : Mix du jour, Redécouvertes, Soirée cosy, Coup de fouet, « Dans la veine de… » ; radio infinie ; file intelligente (désactivable).
- Contrôles : « Moins de ce genre », « Ne plus proposer », réinitialiser, **exporter et effacer l'historique en un geste**.
- Analyse audio facultative (tempo, énergie, luminosité spectrale, sonie) en tâche de fond WorkManager, appareil en charge : **DSP écrit en Kotlin sur un extrait décodé par `MediaCodec`**, sans bibliothèque native, donc **+0 Mo d'APK** (compromis : plus lent qu'une bibliothèque optimisée, d'où la tâche de fond).
- Tests : scores, décroissance, diversité, démarrage à froid.

### P4 — Pont NouTube
- Détection (`<queries>` sur `jp.nonbili.noutube`, sans `QUERY_ALL_PACKAGES`), section explicative et lien F-Droid si absent.
- Import d'un fichier choisi par l'utilisateur : **Takeout `watch-history.json`** (classement chiffré), export des favoris NouTube (CSV), avec validation sur un vrai échantillon avant d'écrire le parseur.
- Aperçu du Top avec indicateur « déjà dans ma bibliothèque » (rapprochement titre + durée), sélection, **file de récupération guidée** par lots (ouvrir dans NouTube un morceau à la fois, écran de suivi).
- Détection des fichiers NouTube par propriétaire Android ; rapprochement avec la file ; vidéos NouTube jouées en audio seul (piste vidéo désactivée), sans jamais lister les vidéos personnelles.
- Provenance visible (Local / NouTube / En ligne) et filtrable ; mention sobre sur les conditions d'utilisation de YouTube.

### P5 — Recherche unifiée, finitions, performances
- Recherche : locale d'abord, puis « Chercher “…” dans YouTube Music » (intent NouTube) ; « Plus de cet artiste sur YouTube Music » sur les fiches.
- Baseline Profiles, mesure du démarrage à froid, catalogue incrémental si besoin, pochettes en cache.
- Accessibilité vérifiée avec TalkBack, essais en paysage et grand écran, mise à jour depuis la v2.x testée sur le Pixel (données conservées), **v3.0.0**, journal des modifications en français, publication GitHub.

## 4. Décisions à prendre avant de commencer

- **D1 — `INTERNET` et paroles en ligne.** Aujourd'hui la permission est déclarée et la recherche LRCLIB est active par défaut. Android ne permet pas de retirer `INTERNET` à l'exécution : soit on la garde, soit on supprime les paroles en ligne.
- **D2 — Décodeur FFmpeg** pour lire WMA/AIFF/APE : +10 à 15 Mo par architecture, licence LGPL à respecter.
- **D3 — Source du classement NouTube** : Takeout (chiffré, demande une démarche à l'utilisateur) ou favoris NouTube seuls (sans compteur).
- **D4 — Base de données** : Room (si compatible) ou SQLite brut.
