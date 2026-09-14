package com.niumi.system.session.fakes

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision

/**
 * Payloads NFC réels et sessions pré-persistées pour les scénarios de libération (étape 18).
 *
 * Les URI sont canoniques au sens de SPEC_CORE_KMP §9.1 (`niumi://box/v1/{boxId}?token={token}`,
 * 22 caractères Base64 URL sans bourrage, dernier caractère dans `AQgw`). L'empreinte attendue
 * n'est **jamais écrite à la main** : elle est dérivée du payload par la façade commune, seule
 * autorité sur le hachage (§9.2). Un hash codé en dur se désynchroniserait en silence du parseur.
 */
object NfcScanFixtures {
    /** Le boîtier figé dans la session active, identique à `SessionDtoFixtures.BOX_ID`. */
    const val SESSION_BOX_URI: String =
        "niumi://box/v1/${SessionDtoFixtures.BOX_ID}?token=AAAAAAAAAAAAAAAAAAAAAA"

    /** Même boîtier, token différent : `TOKEN_MISMATCH`, donc `UnknownBox`. */
    const val SESSION_BOX_WRONG_TOKEN_URI: String =
        "niumi://box/v1/${SessionDtoFixtures.BOX_ID}?token=BAAAAAAAAAAAAAAAAAAAAA"

    /** Un autre boîtier Niumi, valide mais étranger à la session : `BOX_MISMATCH`. */
    const val OTHER_BOX_URI: String =
        "niumi://box/v1/33333333-3333-3333-3333-333333333333?token=AAAAAAAAAAAAAAAAAAAAAA"

    /** Rejeté par le parseur avant toute vérification (`MALFORMED_URI`). */
    const val MALFORMED_URI: String = "niumi:/box/v1/pas-un-uuid"

    /**
     * Extras dont le boîtier figé correspond à [uri]. `boxTokenSha256Hex` vient de
     * [PairedBoxCredentialDto.fromPayload], comme à l'association réelle (SPEC_ANDROID §11.1).
     */
    fun extrasFor(
        facade: NiumiCoreFacade,
        uri: String = SESSION_BOX_URI,
    ): AndroidSessionExtras {
        val payload =
            requireNotNull(facade.parseBoxPayload(uri).payload) {
                "URI de fixture rejetée par le parseur commun : $uri"
            }
        val credential = PairedBoxCredentialDto.fromPayload(payload)
        return SessionDtoFixtures.extras().copy(
            boxId = credential.boxId,
            boxTokenSha256Hex = credential.tokenSha256Hex,
        )
    }
}

/**
 * Écrit une session directement dans la passerelle, sans rejouer `ACTIVATION_REQUESTED`.
 *
 * Les scénarios de reprise partent d'un état que seule une mort de processus produit
 * (`RELEASING` avec une outbox incomplète, `AWAITING_NFC` sans notification) : le reconstruire par
 * le parcours normal exigerait de faire échouer des effets à mi-chemin, ce qui testerait
 * l'activation plutôt que la reprise. Même parti pris que `SessionReconcilerRingingTest`.
 */
@Suppress("LongParameterList")
suspend fun TestCoordinatorHarness.persistSession(
    state: SessionStateDto,
    revision: Long = 3,
    extras: AndroidSessionExtras = SessionDtoFixtures.extras(),
    effects: List<PendingEffect> = emptyList(),
    releaseTarget: ReleaseTargetDto? = null,
    eventId: String = "00000000-0000-0000-0000-00000000000f",
) {
    val snapshot =
        SessionDtoFixtures
            .snapshotInState(state, revision = revision)
            .copy(releaseTarget = releaseTarget)
    gateway.commit(
        StoredDecision(
            snapshot = snapshot,
            receipt =
                EventReceipt(
                    eventId = eventId,
                    sessionId = SessionDtoFixtures.SESSION_ID,
                    payloadSha256Hex = "c".repeat(64),
                    appliedRevision = revision,
                    receivedAtEpochMillis = 1_000L,
                ),
            effects = effects,
            androidExtras = extras,
        ),
    )
}
