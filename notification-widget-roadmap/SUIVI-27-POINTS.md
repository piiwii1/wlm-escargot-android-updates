# PiiWii Notification Widget — Suivi vivant des améliorations

**Référence : version 1.0.12** (versionCode 13). **Évaluation : 26 octobre 2026**.

## Règles de suivi

- **Codé** : la fonction est présente dans le code / APK; ce statut ne garantit PAS un fonctionnement réel sur Samsung.
- **Partiel** : implémentation incomplète, couverture limitée, ou fonctionnalité ajoutée mais non suffisamment validée.
- **Absent** : fonctionnalité pas encore implémentée.
- **Validé sur appareil** : à déclarer uniquement après retour de test du Galaxy S23 Ultra de l'utilisateur. Aucun de ces 27 points n'a été vérifié de bout en bout sur l'appareil à ce jour.
- Toute version suivante doit préciser ce qui a changé, combien de points restent, les régressions, le nombre de pages, et quelles vérifications ont été effectuées.
- Ne jamais annoncer une fonction comme « pleinement fonctionnelle » uniquement parce que la compilation a réussi.

## Résumé

- **27 points suivis** : 25 historiques + Spotify (26) + pages de l'application (27).
- **8 codés**, **15 partiels**, **4 absents**.
- **0/27 confirmés fonctionnels sur appareil**. La validation réelle doit être remontée par l'utilisateur.
- **4 pages en 1.0.12** : Accueil, Notifications, Apparence, Tests.
- **19/27 non entièrement implémentés** (partiels ou absents); les 8 autres restent à valider sur appareil.

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

## Détail

| # | Point | État | Reste à faire / preuve attendue |
|---|---|---|---|
| 01 | Recharge/débranchement fiable | Partiel | Vérifier comportement en veille et reprise système |
| 02 | Batterie pleine à 100 % | Partiel | Seuil non arrondi corrigé et état BATTERY_STATUS_FULL reconnu ; validation Samsung requise |
| 03 | Appels entrants vs manqués | Partiel | 1.0.9 : catégories Android call/missed_call et filtrage de répondeur ; test sur Samsung manquant |
| 04 | Banques suisses | Partiel | 1.0.9 : détection enrichie et normalisation multilingue ; validation des applications réelles requise |
| 05 | Ouvrir notification au toucher | Partiel | Cas PendingIntent indisponible à fiabiliser |
| 06 | Anciennes notifications vs récentes | Codé | Déclassement par âge; test appareil restant |
| 07 | Rafraîchissements/simulations | Partiel | Timers et processus en veille à valider |
| 08 | Restauration après redémarrage | Partiel | Contrôle périodique toutes les 20 min et demande de reconnexion ajoutés ; vérifier en veille et après redémarrage |
| 09 | Priorités personnalisables | Codé | Réglage présent; test appareil restant |
| 10 | Détection automatique apps installées | Absent | Non implémenté |
| 11 | Messages privés Instagram/Facebook | Partiel | 1.0.9 : catégorie message et canaux direct/chat ; comportement des applis réelles à valider |
| 12 | Colis/agenda/réveils/minuteurs | Partiel | 1.0.9 : Swiss Post, canaux timer, rappels et anniversaires améliorés ; non exhaustif |
| 13 | Plus de banques et transactions | Partiel | 1.0.9 : carte/virement distingués en FR/DE/IT/EN ; à valider sur alertes bancaires réelles |
| 14 | Mode éphémère durée et retour | Codé | Fonction présente; test en veille restant |
| 15 | Importer icônes PNG/JPG/WebP | Codé | Fonction présente; test appareil restant |
| 16 | Galerie pictogrammes | Partiel | Emojis/symboles, pas vraie bibliothèque graphique |
| 17 | Couleurs/transparence/contours | Codé | Réglages présents; test appareil restant |
| 18 | Taille adaptative | Partiel | Limites de rendu selon launcher |
| 19 | Autres formats widgets 2x1/2x2 | Absent | Non implémenté |
| 20 | Interface et navigation basse | Codé | Menu moderne; toutes pages à revalider sur appareil |
| 21 | Recherche et catégories de réglages | Codé | Recherche par nom et catégorie ajoutée à Notifications, validation Samsung attendue |
| 22 | Aperçu/simulation tous les états | Codé | Les 30 catégories sont testables via l'onglet Tests, validation Samsung attendue |
| 23 | Plusieurs widgets indépendants | Absent | Pas de profils indépendants par widget |
| 24 | Sécurité/diagnostic/tests | Partiel | 1.0.9 : diagnostic éphémère des catégories reconnues + 41 tests de régression ; essais Android nécessaires |
| 25 | PiiWii Store Bêta | Absent | Aucune publication configurée |
| 26 | Détection Spotify réelle | Partiel | MediaSession ajoutée en 1.0.7, appareil non testé |
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
