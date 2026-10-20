# Justification Play Console — service au premier plan `specialUse`

Document préparatoire à la déclaration du type de service au premier plan `specialUse`
(exigence Play depuis Android 14 : chaque type FGS déclaré doit être justifié individuellement,
et `specialUse` est examiné au cas par cas d'après la propriété du manifeste et cette
déclaration). Statut : préparatoire, pas encore soumis.

## Ce que fait `BlockingGuardService`

Niumi est un réveil qui bloque des applications choisies par l'utilisateur jusqu'à ce qu'il
scanne un tag NFC au réveil. `BlockingGuardService` tourne pendant toute la session que
l'utilisateur a lui-même activée, du moment où il programme son réveil jusqu'au scan. Il ne lit
aucun média, ne synchronise rien, n'accède ni à la position, ni à la caméra, ni au micro. Il
affiche une notification d'état silencieuse (« Réveil programmé à 07:00 », puis « Blocage en
cours jusqu'à 07:00 ») et maintient le processus au premier plan.

Propriété déclarée dans le manifeste :

```xml
<property
    android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    android:value="Keeps the user's app-blocking session alive while the apps they chose are blocked, until they scan their NFC tag at wake-up. Shows a persistent notification; no media, no sync, no location." />
```

## Pourquoi un service au premier plan

Le blocage repose sur l'`AccessibilityService` de Niumi, déclaré séparément
(`ACCESSIBILITY_DECLARATION.md`). Mesuré le 2026-10-19 sur un Xiaomi sous HyperOS : quand
l'utilisateur retire Niumi de ses applications récentes, la surcouche tue le processus malgré le
service d'accessibilité lié, et Android ne relie plus ce service tant que l'utilisateur ne l'a pas
réactivé à la main. Le blocage que l'utilisateur a programmé ne s'applique alors plus, souvent sans
qu'il le sache avant le lendemain. Un service au premier plan est la seule protection mesurée : le
même geste épargne alors le processus. Rien d'autre ne garantit qu'une session demandée par
l'utilisateur tienne jusqu'à son terme.

## Pourquoi `specialUse` et pas un autre type

Aucun type dédié ne correspond. Ce n'est ni de la lecture multimédia (`mediaPlayback`), ni une
synchronisation (`dataSync`, limité à six heures par jour alors qu'une session dure une nuit), ni
un appel, ni une localisation. `specialUse` est le type prévu par Android pour ce cas.

## Comment il est démarré et arrêté

Démarré à l'activation d'une session, geste explicite de l'utilisateur dans l'application au
premier plan ; redémarré si besoin à l'heure de début du blocage (alarme exacte demandée par
l'utilisateur), après un redémarrage du téléphone (`BOOT_COMPLETED`) ou après un changement
d'heure, exemptions documentées par Android. Arrêté dès que la session se termine (scan du tag NFC)
ou échoue. Il n'expose aucune action d'arrêt : rien n'arrête une session hors du scan, ce que
l'application explique avant la première activation. La notification peut être écartée par
l'utilisateur ; elle ne revient pas d'elle-même.

## Durée

Bornée à la session : quelques heures, une nuit au plus, une fois par jour au plus.

## Preuve dans le code

`BlockingGuardService`
([`androidApp/feature/session/.../blocking/guard/BlockingGuardService.kt`](../../../androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking/guard/BlockingGuardService.kt)),
déclaré dans le manifeste de `:feature:session` ; règle de démarrage et d'arrêt dans
`BlockingGuardPolicy` (`:core:system`) ; SPEC_ANDROID §12.5.
