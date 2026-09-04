package com.niumi.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `@HiltAndroidApp` n'a pas de rétention runtime : on ne peut pas la détecter par réflexion.
 * Le signal fiable que Hilt a traité `NiumiApplication` est la présence de la classe générée
 * `Hilt_NiumiApplication`, qui n'existe que si le processeur d'annotations KSP a tourné.
 */
class SmokeTest {
    @Test
    fun hiltGeneratedBaseClassExists() {
        val generatedClass = Class.forName("com.niumi.app.Hilt_NiumiApplication")

        assertThat(NiumiApplication::class.java.superclass).isEqualTo(generatedClass)
    }
}
