# Notice

## Licence de HOLOWAVE

Depuis la version 3, HOLOWAVE est distribué sous licence **GNU Affero General Public License v3.0** (fichier `LICENSE`).
Les versions 2.x publiées avant ce changement restent disponibles sous licence MIT.

La raison du changement : HOLOWAVE intègre du code de NouTube, publié sous AGPL-3.0, qui impose que l'œuvre dérivée soit publiée sous la même licence.

## Code de NouTube

Certaines parties de HOLOWAVE sont dérivées de [NouTube](https://github.com/nonbili/NouTube) (version 0.7.4, commit `a2eeb7b`),
© ses auteurs, licence AGPL-3.0 :

- les scripts injectés dans la page de YouTube Music (répertoire `tube-scripts/`, adaptés de `content/` dans NouTube) ;
- l'hôte WebView et le pont natif (`app/src/main/java/io/github/x80070006/holowave/tube/`), adaptés de `modules/nou-tube-view` dans NouTube.

Les fichiers concernés portent un en-tête qui l'indique. Le code source complet de HOLOWAVE est disponible à
<https://github.com/0x80070006/holowave-deco>.

## Autres composants

- AndroidX Media3, AndroidX Room, Jetpack Compose : Apache-2.0
- Manrope : SIL Open Font License 1.1 (`licenses/OFL-Manrope.txt`)
- Paroles : LRCLIB (base communautaire, accès réseau optionnel)

HOLOWAVE n'est affilié ni à Google, ni à YouTube, ni à NouTube. L'usage de YouTube Music dans l'application reste soumis aux
conditions d'utilisation de YouTube.
