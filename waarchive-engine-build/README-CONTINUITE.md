# WhatsApp Archive Engine — continuité

## Base stable
- 0.3.2 : première base Android officielle installée, ouverte et restée stable sur le téléphone de test.
- Ne jamais revenir aux anciennes bases NativeActivity/JNI/DEX fabriqués à la main.

## Version actuelle
- 0.3.3
- applicationId : `ch.piiwii.waarchive`
- versionCode : 6
- minSdk 24 / targetSdk 35
- Java Android natif, Activity standard, aucune bibliothèque JNI.

## Changements 0.3.3
- barre supérieure proche de WhatsApp avec avatar et nom du contact ;
- fond de discussion natif avec motif discret ;
- bulles reçues/envoyées retravaillées ;
- dates et message de sécurité ;
- aperçu photo natif ;
- aperçu vocal avec waveform ;
- barre basse en lecture seule façon WhatsApp.

## Architecture à conserver
La vraie archive sera branchée ensuite via SQLite/indexation locale. Le moteur d’interface ne doit pas parser 1 Go au démarrage. Le futur scelleur Windows préparera la base, les miniatures et les médias puis les incorporera à l’APK finale.

## Règle de sécurité projet
Toujours conserver une copie de la dernière APK réellement installée et ouvrable avant toute évolution. Ne jamais modifier la fondation fonctionnelle pour une simple retouche graphique.
