# Changelog

## 1.0.7 — 2026-09-29
- Refonte ergonomique de l’écran principal.
- Écriture en deux temps : préparation puis armement explicite.
- Résultat de lecture séparé des détails techniques.
- Actions contextuelles sur URI : ouvrir, appeler, e-mail, SMS.
- Copie rapide du contenu lu dans le presse-papiers.
- Détails techniques masqués par défaut et affichables à la demande.
- Réutilisation du dernier contenu ouvre automatiquement l’éditeur.
- Aucun changement des limites de sécurité : copie NDEF uniquement.

## 1.0.6 — 2026-09-29
- Correction renforcée des zones système Android haut/bas.
- targetSdk ramené à 34 pour éviter le edge-to-edge forcé d’Android 15 sur cette version sideload.
- Insets appliqués au conteneur de contenu avec prise en compte de la découpe écran.
- Aucun changement du moteur NFC.

## 1.0.5 — 2026-09-29
- Correction de l’interface sous la barre d’état Android (heure/batterie).
- Correction de l’interface sous la barre de navigation/gestes en bas.
- Insets système dynamiques pour Android récent et anciennes versions compatibles.
- Couleur cohérente des barres système avec le thème sombre.
- Aucun changement du moteur NFC.

## 1.0.3 — 2026-09-28
- Anti-double-scan pour éviter les lectures/écritures déclenchées deux fois trop rapidement.
- État NFC visible en permanence et rafraîchi au retour dans l’application.
- Brouillon et type d’écriture conservés entre deux ouvertures.
- Estimation de la taille NDEF avant écriture.
- Bouton d’annulation pour les modes écriture, copie et effacement.
- Décodage plus clair des liens téléphone, e-mail et SMS.
- Ajout de l’écriture NFC téléphone (`tel:`), e-mail (`mailto:`) et SMS (`sms:`).
- Ajout d’un bouton d’accès direct aux réglages NFC Android.
- Ajout d’un bouton pour vider l’historique avec confirmation.
- Affichage du nombre d’enregistrements NDEF lus.
- Affichage de la longueur de l’UID visible.
- Conservation du diagnostic de capacité, de la copie NDEF source/cible et de la protection contre la copie sur le tag source.

## 1.0.1 — 2026-09-28
- Ajout du diagnostic de capacité NDEF utilisée.
- Indicateur de compatibilité de copie du contenu.
- Détection des tags formatables NDEF.
- Empêche d'utiliser accidentellement le tag source comme tag cible pendant une copie.

## 1.0.4
- Export du dernier message NDEF vers un fichier .ndef.
- Import d'un fichier .ndef puis écriture directe sur un tag cible.
- Réutilisation du dernier contenu lu dans l'éditeur.
- Garde-fou de taille (1 Mo) lors de l'import.
