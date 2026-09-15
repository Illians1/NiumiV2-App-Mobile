# Règles R8 propres à Niumi (SPEC_ANDROID §16 : le mode release active R8 et la suppression des
# ressources inutilisées).
#
# Ce fichier est volontairement vide de toute règle, et c'est un constat, pas un oubli. Mesuré à
# l'étape 21 sur le premier `:app:assembleRelease` du dépôt : le build passe sans aucune règle
# ajoutée, et R8 ne produit pas de `missing_rules.txt`. Les quatre bibliothèques qui auraient pu
# en réclamer embarquent les leurs, appliquées automatiquement — vérifié dans
# `build/outputs/mapping/release/configuration.txt` :
#
#   - Hilt 2.60.1        : `hilt-android/proguard.txt`
#   - Room 2.8.5         : `room-runtime/proguard.txt`, `room-ktx/proguard.txt`
#   - kotlinx-serialization 1.11.0 : `META-INF/com.android.tools/r8/kotlinx-serialization-r8.pro`
#                          et `kotlinx-serialization-common.pro`
#   - kotlinx-datetime, DataStore, Compose, Navigation : idem
#
# Les DTO de `NiumiCore` n'ont pas besoin d'un `-keep` : ils ne sont jamais atteints par
# réflexion. Room génère son code d'accès à la compilation, et les sérialiseurs de
# kotlinx-serialization portent les noms de champs dans leur descripteur — l'obfuscation des
# classes ne change donc pas le JSON du snapshot Direct Boot (§7.3).
#
# N'ajouter ici qu'une règle exigée par un échec réel, en la commentant avec sa cause : un
# `-keep` préventif désactive silencieusement l'optimisation qu'il prétend protéger, et masque
# le jour où la règle deviendrait nécessaire pour une autre raison.
