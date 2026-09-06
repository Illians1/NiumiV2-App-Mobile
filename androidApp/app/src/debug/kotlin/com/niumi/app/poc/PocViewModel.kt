package com.niumi.app.poc

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.database.BlockedPackage
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.Clock
import com.niumi.system.pairing.PairedBoxStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

// SPEC_ANDROID §16 : jamais le token ni son hash complet, seulement un préfixe de boxId.
private const val BOX_ID_PREFIX_LENGTH = 8

data class PocUiState(
    val secondsInput: String = "90",
    val isScheduled: Boolean = false,
    val pairedBoxIdPrefix: String? = null,
    val blockPackageInput: String = "",
    val isBlocked: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false,
)

@HiltViewModel
class PocViewModel
    @Inject
    constructor(
        private val alarmScheduler: AlarmScheduler,
        private val clock: Clock,
        private val pairedBoxStore: PairedBoxStore,
        private val blockingController: BlockingController,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        var state by
            mutableStateOf(
                PocUiState(
                    isScheduled = alarmScheduler.isScheduled(PocSession.ID),
                    isBlocked = blockingController.effectivePackages().isNotEmpty(),
                    isAccessibilityServiceEnabled = blockingController.isServiceEnabled(),
                ),
            )
            private set

        init {
            viewModelScope.launch {
                state = state.copy(pairedBoxIdPrefix = pairedBoxStore.current()?.boxId?.take(BOX_ID_PREFIX_LENGTH))
            }
        }

        fun onSecondsInputChanged(value: String) {
            state = state.copy(secondsInput = value.filter { it.isDigit() })
        }

        fun onBlockPackageInputChanged(value: String) {
            state = state.copy(blockPackageInput = value.trim())
        }

        /** Recharge le boîtier associé (appelé au retour de `PocPairingActivity`). */
        fun refreshPairedBox() {
            viewModelScope.launch {
                state = state.copy(pairedBoxIdPrefix = pairedBoxStore.current()?.boxId?.take(BOX_ID_PREFIX_LENGTH))
            }
        }

        /** Recharge l'état du service (appelé au retour de l'écran de consentement). */
        fun refreshAccessibilityStatus() {
            state = state.copy(isAccessibilityServiceEnabled = blockingController.isServiceEnabled())
        }

        fun schedule() {
            val seconds = state.secondsInput.toLongOrNull() ?: return
            val triggerAtEpochMillis = clock.nowEpochMillis() + seconds * MILLIS_PER_SECOND
            alarmScheduler.schedule(PocSession.ID, PocSession.REVISION, triggerAtEpochMillis)
            refresh()
        }

        fun cancel() {
            alarmScheduler.cancel(PocSession.ID)
            refresh()
        }

        fun block() {
            val packageName = state.blockPackageInput
            if (packageName.isEmpty()) return
            blockingController.apply(PocSession.ID, setOf(BlockedPackage(packageName, resolveLabel(packageName))))
            state = state.copy(isBlocked = true)
        }

        fun unblock() {
            blockingController.remove(PocSession.ID)
            state = state.copy(isBlocked = false)
        }

        // Repli sur le nom de package si PackageManager ne peut pas résoudre de libellé
        // (application non installée sur l'appareil de test) — SPEC_ANDROID §12.1.
        private fun resolveLabel(packageName: String): String =
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }

        private fun refresh() {
            state = state.copy(isScheduled = alarmScheduler.isScheduled(PocSession.ID))
        }

        private companion object {
            const val MILLIS_PER_SECOND = 1000L
        }
    }
