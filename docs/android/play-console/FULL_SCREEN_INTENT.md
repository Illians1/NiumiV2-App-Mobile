# `USE_FULL_SCREEN_INTENT` — retirée (note de retrait)

**Retirée le 2026-10-18 (Lot 9, étape 32).** Niumi ne déclare plus la permission
`USE_FULL_SCREEN_INTENT` et n'a donc plus rien à justifier à ce titre sur Google Play. Le
document préparatoire qui occupait ce fichier (jamais soumis) est conservé dans l'historique du
dépôt, au commit `6c722ef` et avant.

## Pourquoi

La notification de sonnerie portait un `fullScreenIntent` qui présentait `AlarmActivity`
au-dessus de l'écran verrouillé. Mesuré aux étapes 4, 6 et 30 (Xiaomi 25080RABDG / Android 16) :
le lecteur NFC n'est pas actif tant que le téléphone est verrouillé, si bien que cet écran ne
permettait pas de scanner le boîtier. Le 2026-10-18, il a en plus gêné le déverrouillage
(`docs/android/implementation-reports/ETAPE-31.md`). Décision prise avec l'utilisateur :
conception `docs/superpowers/specs/2026-10-18-reveil-sans-plein-ecran-design.md`.

## Ce qui le remplace

- La notification de sonnerie (`niumi_alarm_ringing`, `CATEGORY_ALARM`, `setOngoing(true)`,
  visibilité publique, sans action) est visible sur l'écran de verrouillage ; son
  `contentIntent` ouvre l'écran de réveil une fois le téléphone déverrouillé.
- L'icône de Niumi mène à l'écran de réveil pendant tout état de scan.
- SPEC_ANDROID §4.1, §4.4, §10.2 à §10.4, §11.2 et §14 (huit permissions).

## Ce qui change dans la déclaration Play

- Aucune déclaration « intent plein écran » n'est à remplir.
- La vidéo de revue (`REVIEW_VIDEO_SCRIPT.md`, plan 8) montre la notification sur l'écran de
  verrouillage, le déverrouillage et le tap, et non plus un écran imposé.
