# PiiWii Notification Widget — Suivi vivant des améliorations

**Référence : version 1.0.7** (versionCode 8). **Évaluation : 26 octobre 2026**.

## Règles de suivi

- **Codé** : la fonction est présente dans le code / APK; ce statut ne garantit PAS un fonctionnement réel sur Samsung.
- **Partiel** : implémentation incomplète, couverture limitée, ou fonctionnalité ajoutée mais non suffisamment validée.
- **Absent** : fonctionnalité pas encore implémentée.
- **Validé sur appareil** : à déclarer uniquement après retour de test du Galaxy S23 Ultra de l'utilisateur. Aucun de ces 27 points n'a été vérifié de bout en bout sur l'appareil à ce jour.
- Toute version suivante doit préciser ce qui a changé, combien de points restent, les régressions, le nombre de pages, et quelles vérifications ont été effectuées.
- Ne jamais annoncer une fonction comme « pleinement fonctionnelle » uniquement parce que la compilation a réussi.

## Résumé

- **27 points suivis** : 25 historiques + Spotify (26) + pages de l'application (27).
- **6 codés**, **17 partiels**, **4 absents**.
- **0/27 confirmés fonctionnels sur appareil**. La validation réelle doit être remontée par l'utilisateur.
- **4 pages en 1.0.7** : Accueil, Notifications, Apparence, Tests.
- **21/27 non entièrement implémentés** (partiels ou absents); les 6 autres restent à valider sur appareil.

## Détail

| # | Point | État | Reste à faire / preuve attendue |
|---|---|---|---|
| 01 | Recharge/débranchement fiable | Partiel | Vérifier comportement en veille et reprise système |
| 02 | Batterie pleine à 100 % | Partiel | Valider détection et affichage à 100 % |
| 03 | Appels entrants vs manqués | Partiel | Validation multi-application téléphone |
| 04 | Banques suisses | Partiel | Couverture packages et langues incomplète |
| 05 | Ouvrir notification au toucher | Partiel | Cas PendingIntent indisponible à fiabiliser |
| 06 | Anciennes notifications vs récentes | Codé | Déclassement par âge; test appareil restant |
| 07 | Rafraîchissements/simulations | Partiel | Timers et processus en veille à valider |
| 08 | Restauration après redémarrage | Partiel | Contrôle multi-redémarrages nécessaire |
| 09 | Priorités personnalisables | Codé | Réglage présent; test appareil restant |
| 10 | Détection automatique apps installées | Absent | Non implémenté |
| 11 | Messages privés Instagram/Facebook | Partiel | Reconnaissance heuristique |
| 12 | Colis/agenda/réveils/minuteurs | Partiel | Reconnaissance limitée à certaines apps |
| 13 | Plus de banques et transactions | Partiel | Classification par mots-clés |
| 14 | Mode éphémère durée et retour | Codé | Fonction présente; test en veille restant |
| 15 | Importer icônes PNG/JPG/WebP | Codé | Fonction présente; test appareil restant |
| 16 | Galerie pictogrammes | Partiel | Emojis/symboles, pas vraie bibliothèque graphique |
| 17 | Couleurs/transparence/contours | Codé | Réglages présents; test appareil restant |
| 18 | Taille adaptative | Partiel | Limites de rendu selon launcher |
| 19 | Autres formats widgets 2x1/2x2 | Absent | Non implémenté |
| 20 | Interface et navigation basse | Codé | Menu moderne; toutes pages à revalider sur appareil |
| 21 | Recherche et catégories de réglages | Partiel | Regroupement présent, recherche absente |
| 22 | Aperçu/simulation tous les états | Partiel | Couverture de simulation incomplète |
| 23 | Plusieurs widgets indépendants | Absent | Pas de profils indépendants par widget |
| 24 | Sécurité/diagnostic/tests | Partiel | Diagnostics ponctuels; tests insuffisants |
| 25 | PiiWii Store Bêta | Absent | Aucune publication configurée |
| 26 | Détection Spotify réelle | Partiel | MediaSession ajoutée en 1.0.7, appareil non testé |
| 27 | Pages de l'application : nombre et fonctionnement | Partiel | 4 pages : Accueil / Notifications / Apparence / Tests; code présent, vérifier que toutes sont visibles et utilisables sur téléphone |

## Format de compte rendu obligatoire des prochaines versions

> Version X.Y.Z — **N/27 codés · P/27 partiels · A/27 absents · V/27 validés sur appareil**. Pages : **4/4 attendues**, avec signalement de régression si problème. Progrès par rapport à la version précédente : …  À finaliser : …  Tests à faire sur Galaxy S23 Ultra : …

Mettre à jour ce fichier **après chaque nouvelle APK**, en s'appuyant sur le code réel et sur les tests effectivement réalisés. Si le nombre de pages change, corriger explicitement le point 27.
