package com.niumi.system.session

import com.niumi.core.interop.SessionStateDto

/**
 * États finaux de la machine commune. `FINAL_STATES` existe déjà dans `:shared:core`
 * (`ReducerSupport.kt`) mais est `internal` à `commonMain`, inaccessible depuis Android — redéfini
 * ici pour les mêmes trois valeurs (SPEC_CORE_KMP §5.1).
 */
internal val SESSION_FINAL_STATES: Set<SessionStateDto> =
    setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)
