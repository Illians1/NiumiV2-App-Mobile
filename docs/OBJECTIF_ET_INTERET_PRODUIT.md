# Niumi — Ce que le produit apporte

> Ce document explique **à quoi sert Niumi et pour qui**, sans aucun détail technique.
> Il ne remplace rien : pour le fonctionnement réel de l'application, voir
> [SPEC_CORE_KMP.md](../specs/SPEC_CORE_KMP.md), [SPEC_ANDROID.md](../specs/SPEC_ANDROID.md)
> et [SPEC_IOS.md](../specs/SPEC_IOS.md).

---

## En une phrase

Une application de réveil qui bloque les distractions dès qu'on s'engage sur une heure de lever, et qui oblige à se lever pour les retrouver le lendemain matin.

---

## Le problème

**On ne se lève pas à l'heure qu'on avait prévue.** L'alarme du téléphone s'éteint d'un geste, souvent sans même en garder le souvenir. On reste allongé, on repousse, on finit par se rendormir ou par attraper son téléphone. Le matin commence en retard et sous tension. Le problème n'est pas de se réveiller — le bruit y suffit très bien — le problème, c'est de **se lever**.

**Le téléphone prolonge aussi les soirées et ralentit les matins.** Le soir, il suffit d'ouvrir une application « cinq minutes » pour repousser l'heure de dormir bien plus longtemps que prévu. Le matin, c'est souvent la première chose qu'on attrape : on est réveillé, mais toujours allongé, déjà happé par les applications qui captent l'attention.

Ces deux problèmes ont la même origine : au moment où la volonté est la plus faible — tard le soir et juste après le réveil — le téléphone donne un accès immédiat à ce qu'on avait pourtant décidé d'éviter. **Niumi transforme donc une intention prise la veille en une contrainte concrète qui tient jusqu'au lever.**

---

## Ce qu'est Niumi

Une application mobile qui combine un réveil et un blocage temporaire des applications choisies.

Le soir, on règle son heure de réveil et on choisit les applications que l'on ne veut plus pouvoir ouvrir. Une fois le réveil configuré, ces applications restent bloquées jusqu'au lendemain matin.

Quand l'alarme sonne, elle ne s'éteint pas simplement depuis le lit. Pour terminer le réveil et retrouver l'accès aux applications bloquées, il faut se lever et scanner un boîtier NFC Niumi placé volontairement dans une autre pièce — par exemple dans la salle de bain, la cuisine ou le salon.

Le boîtier n'est donc pas le réveil : **c'est le point d'arrivée du réveil**, l'endroit qui matérialise le fait d'être réellement sorti du lit.

---

## Ce que ça apporte

### Se lever à l'heure, vraiment

C'est le bénéfice principal. Avec une alarme classique, le geste qui arrête le réveil est aussi facile que celui qui permet de continuer à dormir. Avec Niumi, arrêter définitivement l'alarme demande une action physique : sortir du lit, prendre son téléphone et rejoindre le boîtier NFC installé ailleurs.

L'objectif n'est pas seulement d'ouvrir les yeux à l'heure prévue, mais de faire en sorte que **l'heure de réveil décidée la veille devienne réellement l'heure du lever**.

### Couper les distractions dès la veille

Configurer son réveil devient aussi un engagement pour la soirée. Une fois l'heure choisie, les applications que l'on sait capables de prolonger inutilement la nuit ne sont plus accessibles.

Il n'y a plus à gagner, à 23 h 30, une nouvelle bataille contre TikTok, Instagram, YouTube, Reddit ou n'importe quelle autre application que l'on avait décidé de ne plus ouvrir. La décision a déjà été prise plus tôt, au moment où l'on était encore lucide sur ce que l'on voulait pour sa soirée et pour son lendemain.

### Garder son téléphone sans garder toutes ses tentations

Niumi ne demande pas de renoncer complètement au téléphone pendant la nuit. Seules les applications choisies sont bloquées.

Le téléphone peut donc rester disponible pour ce qui compte ou ce qui est utile, sans donner pour autant un accès immédiat aux applications que l'on cherche précisément à éviter. Le but n'est pas de rendre le téléphone inutilisable : **c'est de retirer, pendant quelques heures, ce qui empêche de décrocher le soir et de démarrer le matin.**

### Commencer la journée déjà debout

Le scan NFC change le moment critique du réveil. Au lieu de décider quoi faire alors qu'on est encore sous la couette, on prend cette décision la veille en plaçant le boîtier à un endroit où l'on devra aller le lendemain.

Quand l'alarme s'arrête, on n'est plus allongé avec le téléphone à la main : on est déjà dans la salle de bain, la cuisine ou une autre pièce choisie. La première étape de la journée est donc déjà faite.

### Une contrainte volontaire, limitée dans le temps

Niumi ne cherche pas à contrôler toute l'utilisation du téléphone. Le blocage commence parce que l'utilisateur l'a décidé en configurant son réveil, et il se termine lorsqu'il accomplit l'action qu'il avait lui-même choisie pour le lendemain matin.

Ce n'est pas une interdiction permanente. C'est **un engagement pris pour quelques heures**, destiné à protéger une décision simple : ce soir, je veux arrêter de scroller ; demain, je veux me lever à cette heure-là.

---

## Une journée avec Niumi

**Le soir**, on choisit l'heure à laquelle on veut se lever. En configurant le réveil, on active en même temps le blocage des applications que l'on ne veut plus ouvrir jusqu'au lendemain. Le téléphone reste disponible, mais les distractions que l'on a volontairement mises de côté ne le sont plus.

**La nuit**, rien à gérer. Le réveil est programmé et les applications choisies restent bloquées.

**Le matin**, l'alarme sonne à l'heure décidée. Pour l'arrêter et terminer la session, il faut prendre son téléphone, sortir du lit et aller scanner le boîtier NFC placé dans une autre pièce.

Une fois le scan effectué, l'alarme s'arrête et les applications redeviennent accessibles. Mais le moment important a déjà eu lieu : on est debout dans une autre pièce, et la journée a commencé avant que le téléphone puisse redevenir une distraction.

---

## Ce que ces deux choses produisent ensemble

Bloquer les applications le soir et obliger à se déplacer le matin ne sont pas deux fonctions séparées. Ce sont les deux côtés du même engagement.

Le soir, Niumi empêche une décision prise consciemment d'être annulée quelques minutes plus tard par automatisme. Le matin, il empêche cette même décision d'être annulée à moitié endormi par un simple geste sur l'écran.

Entre les deux, l'utilisateur n'a rien à décider de nouveau.

**La veille, il choisit quand sa journée doit s'arrêter et quand la suivante doit vraiment commencer. Niumi fait tenir cette décision jusqu'au bout.**

C'est ce qui permet de récupérer du temps sur les soirées, d'éviter les réveils qui s'éternisent et de commencer plus souvent ses journées debout, à l'heure prévue.

---

## Ce que Niumi ne cherche pas à être

Ce n'est pas une application de productivité générale ni un outil destiné à bloquer le téléphone toute la journée. Ce n'est pas non plus un traqueur de sommeil, un coach ou un assistant personnel.

Niumi se concentre sur un moment précis : **la transition entre la fin de la journée et le début de la suivante**.

Il fait deux choses qui se renforcent mutuellement : empêcher les applications choisies de prolonger la soirée, puis faire en sorte que le réveil du lendemain se termine debout plutôt que dans le lit.

---

## L'objet

Le boîtier NFC Niumi est volontairement simple. Il n'est pas destiné à rester sur la table de nuit : sa place est précisément ailleurs, dans l'endroit où l'utilisateur veut être conduit le matin.

Salle de bain, cuisine, entrée, bureau : chacun choisit l'emplacement qui correspond à sa routine.

L'objet sert de repère physique et permanent. Une fois posé, il matérialise la règle que l'on s'est fixée : **pour terminer le réveil, il faut venir jusqu'ici.**

Comme dans le projet d'origine, l'intention reste celle d'un produit sobre et soigné, suffisamment discret pour rester en place au quotidien, mais suffisamment identifiable pour devenir un élément naturel du rituel du soir et du matin.
