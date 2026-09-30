package com.niumi.system.recents

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Étape 25 : le réglage `locked_apps` de HyperOS, tel que relevé sur appareil le 2026-09-28. */
class LockedAppsParserTest {
    @Test
    fun aMissingSettingMeansTheDeviceHasNoSuchLock() {
        assertThat(LockedAppsParser.stateOf(null, PACKAGE)).isEqualTo(RecentsLockState.UNSUPPORTED)
        assertThat(LockedAppsParser.stateOf("", PACKAGE)).isEqualTo(RecentsLockState.UNSUPPORTED)
    }

    /** Relevé avant le verrouillage : la liste de l'utilisateur 0 est vide. */
    @Test
    fun anEmptyListForTheUserMeansUnlocked() {
        val raw = """[{"u":0,"pkgs":[]},{"u":-100,"pkgs":["com.xiaomi.bsgamecenter","com.jeejen.family.miui"]}]"""

        assertThat(LockedAppsParser.stateOf(raw, PACKAGE)).isEqualTo(RecentsLockState.UNLOCKED)
    }

    /** Relevé après l'appui long et le cadenas. */
    @Test
    fun aListContainingThePackageMeansLocked() {
        val raw = """[{"u":0,"pkgs":["com.niumi.app"]},{"u":-100,"pkgs":["com.xiaomi.bsgamecenter"]}]"""

        assertThat(LockedAppsParser.stateOf(raw, PACKAGE)).isEqualTo(RecentsLockState.LOCKED)
    }

    @Test
    fun anotherPackageDoesNotCount() {
        val raw = """[{"u":0,"pkgs":["com.niumi.application"]}]"""

        assertThat(LockedAppsParser.stateOf(raw, PACKAGE)).isEqualTo(RecentsLockState.UNLOCKED)
    }

    /** Un réglage illisible n'est pas jugé : jamais un blocage d'activation sur une lecture douteuse. */
    @Test
    fun anUnreadableSettingIsNotJudged() {
        assertThat(LockedAppsParser.stateOf("{not json", PACKAGE)).isEqualTo(RecentsLockState.UNSUPPORTED)
        assertThat(LockedAppsParser.stateOf("""{"u":0}""", PACKAGE)).isEqualTo(RecentsLockState.UNSUPPORTED)
    }

    private companion object {
        const val PACKAGE = "com.niumi.app"
    }
}
