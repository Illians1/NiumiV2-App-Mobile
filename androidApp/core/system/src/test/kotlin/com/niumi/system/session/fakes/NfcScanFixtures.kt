package com.niumi.system.session.fakes

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.SessionBox
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

    /** Même boîtier, token différent : `NO_PAIRED_BOX_MATCH`, donc `UnknownBox`. */
    const val SESSION_BOX_WRONG_TOKEN_URI: String =
        "niumi://box/v1/${SessionDtoFixtures.BOX_ID}?token=BAAAAAAAAAAAAAAAAAAAAA"

    /** Un autre boîtier Niumi, valide mais hors de la liste figée : `NO_PAIRED_BOX_MATCH`. */
    const val OTHER_BOX_URI: String =
        "niumi://box/v1/33333333-3333-3333-3333-333333333333?token=AAAAAAAAAAAAAAAAAAAAAA"

    /** Lot 8 : deuxième et troisième boîtiers d'une liste figée à trois. */
    const val SECOND_BOX_URI: String =
        "niumi://box/v1/44444444-4444-4444-4444-444444444444?token=AAAAAAAAAAAAAAAAAAAAAA"
    const val THIRD_BOX_URI: String =
        "niumi://box/v1/55555555-5555-5555-5555-555555555555?token=AAAAAAAAAAAAAAAAAAAAAA"

    /** Rejeté par le parseur avant toute vérification (`MALFORMED_URI`). */
    const val MALFORMED_URI: String = "niumi:/box/v1/pas-un-uuid"

    /**
     * Extras dont la liste figée contient le boîtier de [uri], seul (Lot 8 : voir [extrasForMany]
     * pour une liste à plusieurs boîtiers). `tokenSha256Hex` vient de
     * [PairedBoxCredentialDto.fromPayload], comme à l'association réelle (SPEC_ANDROID §11.1).
     */
    fun extrasFor(
        facade: NiumiCoreFacade,
        uri: String = SESSION_BOX_URI,
    ): AndroidSessionExtras = SessionDtoFixtures.extras(boxes = listOf(credentialBox(facade, uri)))

    /** Liste figée à plusieurs boîtiers, dans l'ordre des URI données. */
    fun extrasForMany(
        facade: NiumiCoreFacade,
        uris: List<String>,
    ): AndroidSessionExtras = SessionDtoFixtures.extras(boxes = uris.map { credentialBox(facade, it) })

    private fun credentialBox(
        facade: NiumiCoreFacade,
        uri: String,
    ): SessionBox {
        val payload =
            requireNotNull(facade.parseBoxPayload(uri).payload) {
                "URI de fixture rejetée par le parseur commun : $uri"
            }
        val credential = PairedBoxCredentialDto.fromPayload(payload)
        return SessionBox(credential.boxId, credential.tokenSha256Hex, "Boîtier")
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
