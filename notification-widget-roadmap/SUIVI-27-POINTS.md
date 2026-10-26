# PiiWii Notification Widget — Suivi vivant des améliorations

**Référence : version 1.0.24** (versionCode 25). **Évaluation : 26 octobre 2026**.

## Règles de suivi

- **Codé** : la fonction est présente dans le code / APK; ce statut ne garantit PAS un fonctionnement réel sur Samsung.
- **Partiel** : implémentation incomplète, couverture limitée, ou fonctionnalité ajoutée mais non suffisamment validée.
- **Absent** : fonctionnalité pas encore implémentée.
- **Validé sur appareil** : à déclarer uniquement après retour de test du Galaxy S23 Ultra de l'utilisateur. Aucun de ces 27 points n'a été vérifié de bout en bout sur l'appareil à ce jour.
- Toute version suivante doit préciser ce qui a changé, combien de points restent, les régressions, le nombre de pages, et quelles vérifications ont été effectuées.
- Ne jamais annoncer une fonction comme « pleinement fonctionnelle » uniquement parce que la compilation a réussi.

## Résumé

- **27 points suivis** : 25 historiques + Spotify (26) + pages de l'application (27).
- **11 codés**, **15 partiels**, **1 absent**.
- **0/27 confirmés fonctionnels sur appareil**. La validation réelle doit être remontée par l'utilisateur.
- **4 pages en 1.0.24** : Accueil, Notifications, Apparence, Tests.
- **16/27 non entièrement implémentés** (partiels ou absents); les 11 autres restent à valider sur appareil.

## Passe 1/6 — Fiabilité générale (1.0.10)

**Travail codé :**
- (01) ACTION_BATTERY_CHANGED écouté dynamiquement par le service de notifications et l'activité. La réception immédiate dépend du service Android et de son autorisation.
- (02) Niveau exact, seuil de 100 % sans arrondi; gestion des informations de batterie manquantes; 12 cas de régression automatisés.
- (07) Statistiques locales du dernier rafraîchissement et des événements batterie; annulation des alarmes temporaires quand aucun widget n'est actif.
- (08) Demande de reconnexion du listener après redémarrage, mise à jour d'APK et activation du premier widget; nettoyage des alertes éphémères périmées.
- (27) Toujours **4/4 pages dans le code**.

**Résultat des vérifications :** 41 tests de classification existants + 12 tests de batterie; compilation Android et signature vérifiées séparément avant diffusion de l'APK. Aucun test réel Samsung: donc **8 codés / 15 partiels / 4 absents / 0 confirmés sur appareil**.

**Plan de livraison : passe 1 sur 6 effectuée en code, 5 passes de développement restantes**. Le nombre de passes pourrait augmenter si les essais révèlent une régression.

## Passe 2/6 — Reconnaissance des notifications (1.0.11)

**Changements effectivement codés :**
- (03) Ajout de formulations d'appels entrants et appels manqués, notamment allemandes.
- (04, 13) Reconnaissance complémentaire de banques cantonales par identifiant d'application ; plus de formulations de paiement et de virement.
- (11) Distinction des canaux de messages privés et des commentaires/réactions Instagram/Facebook afin de diminuer les faux positifs.
- (26) Spotify : réconciliation des sessions multimédias lors des événements de notification et d'un contrôle de santé, suivi par jeton de session, effacement de l'icône éphémère de Spotify lorsqu'une lecture s'arrête.
- (27) Les quatre pages et le menu bas sont toujours présents dans les sources.
- 54 tests de classification + 12 cas de batterie passés; tests de fonctionnement Samsung toujours non réalisés.

**Statuts sans promotion artificielle :** 8 codés, 15 partiels, 4 absents, **0 validé sur appareil**. Il reste 19 points partiels ou absents à finaliser. **Passe 2/6 accomplie en code ; 4 passes planifiées restantes**, hors éventuelles corrections signalées pendant les tests.

## Passe 3/6 — Clics et diagnostics (1.0.12)

**Résultats de code :**
- (05) **Partiel, amélioré** : une activité transparente non exportée gère le toucher du widget. Elle récupère l'action d'une notification encore active et tente le PendingIntent d'origine, puis ouvre l'application source en cas d'annulation/absence et, en dernier recours, PiiWii.
- (24) **Partiel, amélioré** : diagnostic local du nombre de touchers et de l'ouverture (notification/app/réglages), sans enregistrer de texte privé. Six nouveaux tests logiques du clic.
- (27) **Partiel, inchangé dans le code** : dispatch des quatre pages vérifié (Accueil, Notifications, Apparence, Tests). Test réel sur Samsung nécessaire pour déclarer fonctionnel.
- Pas de modification des modes Priorité/Éphémère, de la recharge, de la détection Spotify, des 30 catégories ni du menu inférieur.

**Vérifications :** GitHub Actions 40355101441, build Android réussi après correction de syntaxe ; 54 tests de classification + 12 tests de batterie + 6 tests de routage = 72 tests ; APK signée v2/v3 avec certificat précédent ; SHA-256 APK `b6728cf97f1e273af24fcd174a3071df36243a6e88bb084a3b9a1b3726d9bd57`.

**Décompte honnête inchangé :** 8 points codés, 15 partiels, 4 absents, 0/27 confirmés sur Samsung ; 19 points partiels ou absents à finaliser ; **3 passes prévues restantes** (fonctionnalités avancées, finitions visuelles, validation/publication conditionnelle).

## Passe 4/6 — Widgets avancés (1.0.13)

**Réalisations réellement codées :**
- **10 — PARTIEL :** inventaire des applications visibles dans le lanceur Android et association manuelle à une catégorie ; ce n'est pas une reconnaissance universelle automatique.
- **18 — CODÉ :** taille de rendu adaptée aux dimensions du widget, réglage propre à chaque exemplaire.
- **19 — CODÉ :** trois mises en page : 1×1 (icône), 2×1 (icône et texte), 2×2 (icône et libellé), sélectionnées selon les dimensions transmises par Android.
- **23 — PARTIEL :** chaque widget a maintenant son filtre de notifications, sa couleur et sa taille d'icône ; le choix Priorité/Éphémère et la durée restent globaux.
- **27 — PARTIEL :** toujours quatre pages dans le code et le menu inférieur inchangé. Test physique à réaliser.

**Bilan en code : 10 codés / 16 partiels / 1 absent. 0/27 validés physiquement.**
Les nouveaux tests de format et filtrage sont ajoutés aux 72 vérifications existantes pour un total de **84 tests automatisés**.
**Deux passes prévues restent** sur le plan de six. L'absence de validation réelle sur Samsung ne permet pas de prétendre à une finition complète.

## Passe 5/6 — Galerie graphique native (1.0.14)

- 24 pictogrammes vectoriels distincts (fichiers VectorDrawable Android), scalables sans dégradation d'une image bitmap étirée.
- Interface de choix avec deux onglets Graphiques / Emojis, sélection mise en évidence, légendes et accès depuis Notifications et Apparence.
- Les symboles/emojis et importations personnelles PNG/JPG/WebP restent disponibles.
- Le choix d'une icône vectorielle utilise la préférence existante `icon_<category>` préfixée `vector:`; aucune migration destructive.
- Les quatre pages (Accueil, Notifications, Apparence, Tests), le menu inférieur flottant, Spotify, les 30 catégories et les modes Priorité/Éphémère sont conservés.
- Tests GitHub Actions `40361310137` : **84/84 scénarios Java existants réussis**, **24 ressources XML vérifiées**, Gradle release et signature v2/v3 conformes. Certificat identique à la 1.0.13.
- **Nouveau bilan : 11 codés, 15 partiels, 1 absent, 0/27 confirmés fonctionnels sur Galaxy S23 Ultra.**
- **Passe 5/6 achevée ; passe 6/6 planifiée restante.** Les retours de test réel peuvent nécessiter des corrections supplémentaires.

## Passe 6/6 — Contrôle final de la phase et préparation de bêta (1.0.15)

**Correctifs de l'APK :**
- Batterie inconnue (niveau `-1`) : ne provoque plus la fausse icône « batterie faible » dans le mode éphémère et l'aperçu.
- Spotify : conserve la recherche de MediaSession même lorsque Android retourne une liste de notifications nulle.
- Onglet Tests : ajout d'un **rapport partageable sur action de l'utilisateur** et d'une checklist des essais sur Galaxy S23 Ultra; le rapport exclut le contenu des messages et les identifiants de comptes.
- 4 pages, 30 catégories, 24 vecteurs, trois formats de widget, clic sur notification, modes Priorité/Éphémère et menu bas préservés.

**Vérifications de la phase :**
- Reconstruction de l'APK signée sur GitHub Actions, tests Java récurrents (54 classification, 12 batterie, 6 clics, 12 formats : 84 au total), analyse des 24 XML vectoriels et contrôle des 4 branches de navigation.
- La compilation et les signatures ne remplacent pas une validation de comportement sur Samsung, notamment sous One UI en veille.
- **Statut : 11 codés / 15 partiels / 1 absent / 0 confirmé sur appareil**, donc **16 points encore partiels ou absents**.
- **Le plan de six passes de développement est achevé**, mais **le produit n'est PAS finalisé à 27/27**. Les corrections de terrain et les fonctions partielles nécessitent des passes supplémentaires.
- **PiiWii Store bêta (point 25) : non publié**. Cette APK est une candidate à test manuel, pas une publication de boutique.

**Essais à réaliser :** afficher chaque page, brancher/débrancher, vérifier l'affichage à 100 %, appels/messages/bancaires, lecture/pause Spotify, redimensionner plusieurs widgets, toucher une notification, mise en veille et redémarrage. Ouvrir Tests → Rapport et tests de la bêta pour partager les résultats. Ne passer « validé » qu'après tests réels.

## Amélioration 1.0.16 — Éditeur visuel des icônes de réaction

L'utilisateur a demandé de simplifier la personnalisation : identifier immédiatement l'icône d'origine de chaque réaction, et attribuer une icône personnalisée distincte à chacun des 30 types d'alertes.

**Modifications vérifiées dans le code :**
- Chaque ligne de catégorie affiche la grande icône actuelle, le nom clair de la réaction, le type de personnalisation et le bouton visible **Changer l'icône**.
- Un seul éditeur visuel présente **Actuelle → Nouvelle** et les choix **Graphiques / Emojis / Ma photo**.
- Le nouveau choix reste en **aperçu provisoire** ; il n'est appliqué qu'en appuyant sur **Enregistrer**.
- **Annuler** conserve l'ancienne icône ; **Par défaut** prépare le retour au pictogramme d'origine (puis Enregistrer).
- Les photos importées restent provisoires, en fichier privé temporaire, jusqu'à la validation ; en cas d'annulation, le fichier provisoire est supprimé.
- Les clés historiques `icon_<id>` et `custom_icon_<id>` sont conservées ; personnalisation **indépendante par catégorie** et autres réglages préservés.
- Quatre pages et barre inférieure conservées ; application, versionCode **17**.
- GitHub Actions **40365867320** : tests historiques réussis et compilation release Android réussie ; APK signée avec certificat historique SHA-256 `f61b854fb859b6a9843824dedf582939d33d34c12c41772cf37b3bbf212e5b25`. Fonctionnement sur Samsung **à confirmer**.

**Suivi honnête : 11 codés / 15 partiels / 1 absent, 0 validés sur Samsung.** La nouvelle interface améliore les points 16 (galerie), 20 (ergonomie), 24 (sécurité d'enregistrement) sans augmenter artificiellement le total. **16 points techniquement incomplets**, plus les tests physiques du reste.

## Correction ergonomique 1.0.17 — Fenêtres uniques

Après examen des captures Samsung de la version 1.0.16, la priorité était de **supprimer l'empilement des boîtes de dialogue**, non d'ajouter encore un éditeur.

- Dans Apparence, « Personnaliser mes 30 pictogrammes » ouvre **une seule fenêtre** avec recherche, icône actuelle et bouton « Changer » sur chaque réaction.
- Toucher « Changer » **déplie le choix Graphiques/Emojis/Ma photo dans la même fenêtre**, ainsi que l'aperçu de remplacement et les commandes Enregistrer/Annuler/Par défaut.
- Plus de fenêtre de transition « Catégorie du pictogramme ». L'import d'une photo passe seulement par le sélecteur de fichiers du système Android (nécessaire pour accéder à la galerie du téléphone).
- Dans Mes widgets indépendants, les réglages catégorie/couleur/taille apparaissent **ensemble dans une seule fenêtre**, avec recherche de catégorie, boutons visuels de couleur et taille et confirmation explicite. Aucun sous-menu Android intermédiaire.
- Les choix sont préparés **sans modifier les préférences** avant Enregistrer ; Annuler n'écrase pas l'ancien choix ; les anciennes personnalisations restent intactes.
- Code, navigation 4 pages, mode Priorité/Éphémère, Spotify, 30 catégories, 24 pictogrammes, 3 formats de widget conservés.
- Compilation réussie GitHub Actions `40372985855`, signatures v2/v3 valides, certificat historique préservé. Tests de régression existants réussis.
- **Bilan inchangé : 11 codés / 15 partiels / 1 absent / 0 validé réellement sur Galaxy S23 Ultra**. Les points 16, 20, 23 et 24 sont concernés sans promotion artificielle. **16 points partiels ou absents**.

## Extension des états de charge 1.0.18

À la demande de l'utilisateur, le point **01 — Recharge** comprend désormais la détection du **type de source (câble/induction)** et des **niveaux de vitesse estimés**.

- Détection fiable du branchement, débranchement, charge complète et source **filaire vs induction** par les indicateurs BatteryManager Android.
- Sept nouveaux états indépendants et personnalisables dans la galerie visuelle : **charge lente, normale, rapide, super rapide, ultra rapide, induction, induction rapide**. L'état historique « En charge » reste comme repli lorsque le courant est inconnu ; la catégorie « Batterie pleine » reste prioritaire.
- **Attention technique : les cinq vitesses filaires et l'induction rapide sont uniquement des estimations côté batterie**, à partir du courant et de la tension communiqués par Android. Elles ne prouvent PAS le protocole Samsung 25W/45W, surtout quand la batterie est presque pleine. L'application affiche alors un état générique plutôt qu'un faux mode ultra-rapide.
- Total des catégories personnalisables et simulables : **37** (anciennement 30), sans changer les clés de personnalisation existantes.
- La sélection de chaque icône dans la fenêtre unique (version 1.0.17) reste disponible, y compris pour les nouveaux états.
- Les quatre pages et les modes Priorité/Éphémère sont conservés. Tests Java de charge **16/16** réussis en local et dans le workflow. La classification Samsung en situation réelle reste à confirmer.
- **Statuts : 11 codés / 15 partiels / 1 absent, 0 validés sur Samsung**. Cela améliore le point 01 sans promouvoir une estimation en détection certifiée. 16 points du plan restent partiels ou absents.

## Nouveaux modèles 1, 2 ou 3 informations (1.0.19)

**Besoin exprimé :** afficher simultanément la recharge et les notifications sans priorité exclusive entre leurs emplacements. Préserver le widget actuel et permettre jusqu'à trois informations à l'écran.

**Implémentation :**
- Trois entrées distinctes dans le sélecteur de widgets Android :
  1. **Widget classique (1 information)** : ancien fonctionnement Priorité/Éphémère inchangé.
  2. **Widget duo (2 informations)** : charge + une alerte, ou deux alertes si pas en charge.
  3. **Widget trio (3 informations)** : charge + deux alertes, ou jusqu'à trois alertes si pas en charge.
- Le slot de recharge reste visible pendant que les autres cases affichent les événements. La classification/priorité des notifications ne peut plus masquer la charge sur les nouveaux modèles.
- Les cases vides se replient ; quand aucune information n'est active, l'icône de repos personnalisée est affichée.
- Les zones de clic sont distinctes par case ; chaque zone transmet l'action de sa notification à WidgetClickActivity.
- Deux dispositions par widget multi (horizontale/verticale) selon les dimensions remontées par le lanceur.
- Anciens widgets toujours installables ; profils par widget et pictogrammes déjà personnalisés conservés.
- Les quatre pages (Accueil, Notifications, Apparence, Tests), 37 catégories, modes Priorité/Éphémère et galerie restent présentes.

**Vérifications :** GitHub Actions `40379446400` : compilation `assembleRelease` réussie, description XML des 3 fournisseurs vérifiée, navigation contrôlée, 8 tests de répartition simultanée + 87 autres assertions Java (12 formats, 41 classification, 12 batterie, 6 clic, 16 charge), soit **95 cas de test Java passés**. APK signée par la même clé que la 1.0.18 (certificat SHA-256 `f61b854fb859b6a9843824dedf582939d33d34c12c41772cf37b3bbf212e5b25`). **Aucun test réel Samsung n'a été effectué par l'assistant.**

**Statuts inchangés : 11 codés, 15 partiels, 1 absent, 0 fonction certifiée sur appareil.** 16 points nécessitent encore développement ou validation de l'implémentation. Pour les nouveaux widgets, les essais physiques et les tests de clics restent indispensables.

## Amélioration 1.0.20 — Duo et Trio plus lisibles et fiables

**Corrigé dans le code / APK :**
- Les 2 ou 3 cases sont toujours présentes même si moins d'alertes sont actives. Les cases libres affichent un pictogramme discret et restent cliquables pour ouvrir les réglages, au lieu de disparaître et de déplacer les autres.
- Les petits formats n'affichent que les icônes si leurs libellés seraient illisibles. Les libellés restent visibles quand la taille est suffisante, dans les dispositions horizontales et verticales.
- Les appels actifs passent d'abord. Les autres notifications sont triées de la plus récente à la plus ancienne, tandis que **Spotify, média ambiant**, remplit seulement un emplacement disponible après les alertes ; la recharge reste un emplacement indépendant.
- Quand un aperçu teste une catégorie déjà présente en notification active, la catégorie n'est plus affichée deux fois dans deux cases.
- Conservés : widget classique 1 information, Duo 2, Trio 3, 37 catégories de pictogrammes, 4 pages et menu du bas, personnalisation par widget, charge câblée/induction et réglages historiques.
- **107 tests Java de régression réussis** dans l'Action GitHub `40381723838`, dont **20 scénarios MultiSlot** (8 anciens + 12 nouveaux) ; compilation Android release OK ; signature v2/v3 avec certificat historique.
- **Validation Samsung réelle à effectuer** : comportements en veille, dimensions One UI, touchers simultanés et affichage réel sur écran d'accueil.

**Décompte inchangé : 11 codés / 15 partiels / 1 absent. 16 points restent partiels ou absents, 0/27 validés physiquement.** Cette mise à jour améliore surtout les points 19 et 23 sans considérer les tests automatiques comme une certification sur téléphone.

## 1.0.21 — Pictogrammes par défaut reconnaissables

- WhatsApp : forme de bulle et combiné du logo WhatsApp ; pas un disque vert sans indication.
- Messenger : pictogramme officiel de bulle avec éclair, en bleu.
- Facebook : forme « f » Facebook, en bleu (sans pouce levé).
- Messages privés Facebook : pictogramme Facebook avec une petite bulle de conversation, sans cœur.
- TikTok : symbole TikTok reconnaissable, en blanc avec décalages cyan et rose (pas une simple note de musique).
- Charge par induction / induction rapide : téléphone sur son socle de recharge avec bobine/éclair, distinct du symbole Wi-Fi.
- Aucun événement : cloche discrète barrée en gris, au lieu du cercle blanc ; les emplacements Duo/Trio inoccupés affichent des traits discrets, pas un anneau.
- Le rendu « couleurs de marque » ne subit plus la recoloration monochrome par défaut.
- Ces pictogrammes deviennent les **valeurs par défaut**, uniquement si aucun réglage utilisateur n'existe. Les choix de pictogrammes, images importées et préférences précédentes sont conservés.
- Galerie graphique : **32 pictogrammes vectoriels** (24 précédents + 8 nouveaux). Les **37 catégories**, 4 pages et 3 types de widgets restent.
- Tests : validation XML et compilation GitHub Actions. **Aucune validation réelle sur Samsung pour l'instant**.
- Statuts inchangés : **11 codés, 15 partiels, 1 absent, 0 validé Samsung ; 16 points techniques à finaliser.** Les points 16 (bibliothèque graphique) et 20 (ergonomie) ont progressé, sans inventer une validation matérielle.

## Correction urgente 1.0.22 — fermeture de l'onglet Apparence

**Signalement réel sur Galaxy S23 Ultra** : l'utilisateur rapporte que l'application se ferme quand il touche **Apparence** après la version 1.0.21. Il s'agit d'une **régression utilisateur**, pas d'un test passé.

**Code audité :** le rendu de cinq aperçus vectoriels nouveaux en 1.0.21 était appelé synchroniquement et sans protection dans showAppearance(). Le chargement ou dessin d'un VectorDrawable défaillant peut provoquer une RuntimeException et interrompre l'activité. **Cette hypothèse est plausible ; sans trace logcat, la cause exacte n'est pas démontrée.**

**Correctifs livrés dans le code 1.0.22 :**
- Chargement/dessin de chaque icône entouré d'une protection RuntimeException + pictogramme alternatif lisible ; une icône défectueuse ne peut plus faire fermer toute la page.
- Protection secondaire de l'écran Apparence avec interface de secours qui maintient la navigation et l'éditeur de pictogrammes accessibles si une autre sous-section échoue.
- Aperçus de galerie et de recharge isolés ; taille maximale des bitmaps de galerie limitée pour éviter des allocations excessives liées à la densité d'écran.
- Enregistrement de l'identifiant du pictogramme et du type d'exception, **sans données privées des notifications**, dans le rapport de l'onglet Tests.
- Aucune suppression de préférences, de fichiers personnels, ou des logos ajoutés en 1.0.21.
- Vérifications statiques : 4 pages, 3 widgets, 37 réactions, 32 vecteurs, 107 tests de non-régression hérités. Compilation APK indépendante. Test tactile réel de l'onglet sur Galaxy S23 Ultra **encore nécessaire**.

**Statut inchangé** : 11 codés, 15 partiels, 1 absent, **0/27 fonctions certifiées sur Samsung**. 16 points demeurent incomplets ; la fermeture de l'onglet est un bug de qualité urgent distinct des 27 fonctionnalités du tableau.

## 1.0.23 — Refondre WhatsApp et la recharge par induction

Suite au retour de l'utilisateur sur le rendu de la 1.0.22, correction limitée **aux trois dessins vectoriels** suivants :

- WhatsApp : tuile verte à coins arrondis, **bulle blanche entourant un combiné téléphonique**, reconnaissable en petit format ; le simple rond vert de l'ancien pictogramme est abandonné.
- Induction normale : **téléphone sur un socle de recharge**, éclair cyan dans l'écran ; plus de forme rappelant le Wi-Fi.
- Induction rapide : même téléphone et socle, mais éclair doré et signal distinct.
- Modèles classiques/Duo/Trio et onglet Apparence protégés ; conservation de toutes les autres catégories, icônes et préférences existantes.
- Remplacement des **valeurs par défaut uniquement** : si l'utilisateur a enregistré manuellement une icône pour ces catégories, son choix reste en priorité et doit être remis à « Par défaut » pour afficher les nouveaux dessins.
- Contrôle XML, compilation et signature release ; compatibilité du certificat. Vérification effective du rendu sur Galaxy S23 Ultra toujours à réaliser.
- Suivi : **11 codés / 15 partiels / 1 absent, 0 validés sur Samsung, 16 points partiels ou absents**. Amélioration qualitative du point 16, aucun point déclaré terminé sur simple compilation.

## 1.0.24 — Galerie plus rapide et personnalisation de l'état vide

- **38 réactions personnalisables** (37 précédentes + **Aucune notification**). L'icône d'attente / état au repos devient éditable par la même fenêtre que toutes les autres : graphique, emoji, photo, Enregistrer/Annuler/Par défaut.
- Suppression de l'ancien comportement qui forçait le tiret comme symbole d'état vide et ignorait donc toute sélection emoji personnalisée.
- Le gestionnaire de réactions ne construit plus les 38 lignes et toutes les images au premier affichage. **12 catégories visibles initialement**, puis « Afficher 12 réactions supplémentaires » ; recherche instantanée sur les 38, compteur visible.
- La galerie graphique et les emojis chargent les choix par lots de **16**, avec accès aux suivants dans la même fenêtre, pour réduire le coût de rendu initial sur Galaxy.
- Pour les états ayant un logo dédié, bouton **« Utiliser le dessin conseillé »** : prévisualisation WhatsApp/Facebook/Messenger/TikTok/induction/état vide sans écraser les préférences tant qu'Enregistrer n'est pas touché.
- Les nouvelles icônes WhatsApp/induction de 1.0.23 et les 3 modèles Classique/Duo/Trio restent inchangés.
- **Les 4 pages, l'onglet Apparence protégé, les 32 pictogrammes vectoriels et les anciens réglages sont conservés.**
- Compilation et vérifications automatisées réalisées ; validation tactile sur Samsung reste à faire.
- **11 codés, 15 partiels, 1 absent, 0 confirmé sur Samsung ; 16 points incomplets** (amélioration qualitative des points 16, 20 et 24, sans déclarer de nouvelle validation matériel).

## Détail

| # | Point | État | Reste à faire / preuve attendue |
|---|---|---|---|
| 01 | Recharge/débranchement fiable | Partiel | Câble/induction détectés ; 7 états supplémentaires dont vitesses estimées côté batterie (pas certification Samsung) ; test en veille et sur plusieurs chargeurs à réaliser |
| 02 | Batterie pleine à 100 % | Partiel | Seuil non arrondi corrigé et état BATTERY_STATUS_FULL reconnu ; validation Samsung requise |
| 03 | Appels entrants vs manqués | Partiel | 1.0.9 : catégories Android call/missed_call et filtrage de répondeur ; test sur Samsung manquant |
| 04 | Banques suisses | Partiel | 1.0.9 : détection enrichie et normalisation multilingue ; validation des applications réelles requise |
| 05 | Ouvrir notification au toucher | Partiel | Cas PendingIntent indisponible à fiabiliser |
| 06 | Anciennes notifications vs récentes | Codé | Déclassement par âge; test appareil restant |
| 07 | Rafraîchissements/simulations | Partiel | Timers et processus en veille à valider |
| 08 | Restauration après redémarrage | Partiel | Contrôle périodique toutes les 20 min et demande de reconnexion ajoutés ; vérifier en veille et après redémarrage |
| 09 | Priorités personnalisables | Codé | Réglage présent; test appareil restant |
| 10 | Détection automatique apps installées | Partiel | Liste des apps Android installées visible; association aux catégories encore manuelle |
| 11 | Messages privés Instagram/Facebook | Partiel | 1.0.9 : catégorie message et canaux direct/chat ; comportement des applis réelles à valider |
| 12 | Colis/agenda/réveils/minuteurs | Partiel | 1.0.9 : Swiss Post, canaux timer, rappels et anniversaires améliorés ; non exhaustif |
| 13 | Plus de banques et transactions | Partiel | 1.0.9 : carte/virement distingués en FR/DE/IT/EN ; à valider sur alertes bancaires réelles |
| 14 | Mode éphémère durée et retour | Codé | Fonction présente; test en veille restant |
| 15 | Importer icônes PNG/JPG/WebP | Codé | Fonction présente; test appareil restant |
| 16 | Galerie pictogrammes | Codé | 24 pictogrammes Android vectoriels, sélecteur Graphiques/Emojis, prévisualisation, rendu sur widget ; validation Samsung à faire |
| 17 | Couleurs/transparence/contours | Codé | Réglages présents; test appareil restant |
| 18 | Taille adaptative | Codé | Limites de rendu selon launcher |
| 19 | Modèles de widgets, formats et affichage simultané | Codé | 3 entrées installables distinctes (1/2/3 informations), recharge sur sa propre case et jusqu'à 2 autres notifications, orientations adaptatives ; validation Samsung manquante |
| 20 | Interface et navigation basse | Codé | Menu moderne; toutes pages à revalider sur appareil |
| 21 | Recherche et catégories de réglages | Codé | Recherche par nom et catégorie ajoutée à Notifications, validation Samsung attendue |
| 22 | Aperçu/simulation tous les états | Codé | Les 30 catégories sont testables via l'onglet Tests, validation Samsung attendue |
| 23 | Plusieurs widgets indépendants | Partiel | Chaque identifiant de widget a un filtre, une couleur et une taille ; les variantes à 1, 2 ou 3 emplacements peuvent coexister, mais les réglages Priorité/Éphémère restent globaux |
| 24 | Sécurité/diagnostic/tests | Partiel | 1.0.15 : rapport de diagnostic partageable sans contenu des notifications, checklist Samsung, 84 tests Java précédents; tests appareil et sécurité approfondie manquants |
| 25 | PiiWii Store Bêta | Absent | Aucune publication configurée |
| 26 | Détection Spotify réelle | Partiel | MediaSession et actualisations intégrées; 1.0.15 garde la lecture musicale même si la liste des notifications est indisponible; Samsung non testé |
| 27 | Pages de l'application : nombre et fonctionnement | Partiel | 4 pages : Accueil / Notifications / Apparence / Tests; code présent, vérifier que toutes sont visibles et utilisables sur téléphone |

## Format de compte rendu obligatoire des prochaines versions

> Version X.Y.Z — **N/27 codés · P/27 partiels · A/27 absents · V/27 validés sur appareil**. Pages : **4/4 attendues**, avec signalement de régression si problème. Progrès par rapport à la version précédente : …  À finaliser : …  Tests à faire sur Galaxy S23 Ultra : …

Mettre à jour ce fichier **après chaque nouvelle APK**, en s'appuyant sur le code réel et sur les tests effectivement réalisés. Si le nombre de pages change, corriger explicitement le point 27.

## Livraison 1.0.8 (27 points)

- +2 points codés depuis la 1.0.7 : **21** Recherche dans les catégories et **22** simulation de 30 états.
- Points 01, 02, 07, 08, 24 renforcés : contrôle toutes les 20 minutes, rebind de la surveillance si autorisée, contrôle charge pleine exacte, boutons/diagnostics de récupération.
- Toujours **4 pages** : Accueil / Notifications / Apparence / Tests. Contrôle statique automatisé pour éviter une disparition des pages.
- **8 codés, 15 partiels, 4 absents, 0 validés sur Samsung**. 19 points restent techniquement à finaliser, plus les validations matérielles des 8 codés.
- Les délais de rafraîchissement dépendent des optimisations Android/One UI et ne peuvent être garantis sans essai réel.

## Livraison 1.0.9 (27 points)

- **8 codés, 15 partiels, 4 absents, 0 confirmés sur Samsung** : aucun point partialisé n'est promu à tort en « terminé ».
- Les points **03, 04, 11, 12, 13 et 24** sont améliorés sans casser les priorités, les 30 catégories ou Spotify.
- Classification indépendante testée par **41 scénarios automatisés Java**, passés localement et dans l'action GitHub `40343772513`.
- Nouvelle rubrique de diagnostic : total des notifications observées/classées, dernier package/catégorie uniquement en mémoire, sans conservation du contenu.
- **Pages toujours 4/4 dans les sources** : Accueil, Notifications, Apparence, Tests.
- APK 1.0.9 compilée, signée avec le même certificat que la 1.0.8, signatures v2/v3 vérifiées. Test Samsung S23 Ultra non effectué.
