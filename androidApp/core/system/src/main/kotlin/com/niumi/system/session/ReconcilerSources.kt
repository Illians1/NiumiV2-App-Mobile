package com.niumi.system.session

import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLogFlush
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.boot.DirectBootMerger
import com.niumi.system.notification.ScanRequestNotifier
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.ringing.RingingController

/**
 * Regroupe ce que [SessionReconciler] consulte pour décider d'une reprise (SPEC_ANDROID §9.2,
 * §9.3, §13.1). Un seul paramètre de constructeur plutôt que plusieurs : la décomposition en
 * lectures distinctes reste dans le corps du réconciliateur, seule la signature du constructeur
 * en bénéficie (`LongParameterList` de detekt).
 *
 * `AccessibilityServiceStatus` en est sortie à l'étape 12, remplacée par [readinessMonitor] :
 * l'état du service d'accessibilité n'est plus lu isolément, il fait partie des six contrôles
 * que §13.1 surveille d'un seul tenant.
 *
 * [ringingController] rejoint le groupe à l'étape 17 : une session `RINGING` dont le service a
 * disparu avec son processus doit retrouver son son à la première réconciliation (§10.2).
 *
 * [snapshotPublisher] rejoint le groupe à l'étape 14 : la réconciliation est le seul chemin qui
 * relit la persistance après un redémarrage du processus, c'est donc à elle de réamorcer le flux
 * que l'interface observe (défaut mesuré sur appareil, voir `ETAPE-14.md`).
 *
 * [scanRequestNotifier] rejoint le groupe à l'étape 18 : une session en attente de scan dont la
 * notification a disparu avec le processus n'a plus aucun rappel visible, l'écran de réveil étant
 * fermé (§10.5).
 *
 * [directBootMerger] et [incidentsReader] rejoignent le groupe à l'étape 19. Le premier parce que
 * §9.3 confie explicitement la fusion au réconciliateur (« À `USER_UNLOCKED`, le réconciliateur
 * fusionne de façon idempotente le registre et l'outbox Direct Boot dans Room ») ; le second parce
 * qu'un incident de changement d'heure ne doit être consigné qu'une fois par session, et que la
 * garde se lit en base — ces deux raisons n'arrivent que par broadcast, parfois dans un processus
 * qui vient de naître, où une garde en mémoire serait toujours vide.
 *
 * [ringingWatchdog] rejoint le groupe à l'étape 20 : chaque passe applique la politique de l'alarme
 * de secours (§10.2) sur l'état décidé, qu'elle l'arme (`RINGING`) ou la désarme (tout autre état).
 *
 * [runtimeReconciler] rejoint le groupe à l'étape 20 : les deux écarts que
 * `SessionReadinessMonitor` ne couvre pas (§7.1, §18) — une alarme `ARMED` disparue et le NFC —
 * sont traités en fin de passe, sur le snapshot le plus à jour.
 *
 * [storageIntegrity] rejoint le groupe à l'étape 20 : le réconciliateur est le seul point qui voit
 * `LoadResult.Unreadable`, donc le seul qui puisse tenir à jour l'état lu par l'accueil et l'écran
 * de diagnostic.
 *
 * [technicalEventFlush] rejoint le groupe à l'étape 20 : le journal écrit avant déverrouillage doit
 * rejoindre Room dès que possible (§17), au même endroit que la fusion Direct Boot et pour la même
 * raison — un déverrouillage n'attend pas éternellement.
 */
data class ReconcilerSources(
    val alarmScheduler: AlarmScheduler,
    val blockedPackagesProjection: BlockedPackagesProjection,
    val readinessMonitor: SessionReadinessMonitor,
    val snapshotPublisher: SessionSnapshotPublisher,
    val ringingController: RingingController,
    val scanRequestNotifier: ScanRequestNotifier,
    val directBootMerger: DirectBootMerger,
    val incidentsReader: SessionIncidentsReader,
    val ringingWatchdog: RingingWatchdog,
    val runtimeReconciler: SessionRuntimeReconciler,
    val storageIntegrity: StorageIntegrityState,
    val technicalEventFlush: TechnicalEventLogFlush,
)
