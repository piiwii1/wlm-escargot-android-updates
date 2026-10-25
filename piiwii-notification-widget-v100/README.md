# PiiWii Notification Widget — 1.0.0

Application Android native, package `ch.piiwii.notificationwidget`, `versionCode = 1`.
Widget 1 × 1 prévu (le lanceur Samsung peut imposer une taille minimale différente), extensible, fond transparent, pictogrammes modifiables.
27 catégories activées par défaut. Accès aux notifications via NotificationListenerService, tri par priorité, prise en compte de l'état de recharge et batterie faible.
L'application ne stocke pas les textes des notifications et n'envoie rien à un serveur.
Les catégories bancaires dépendent des informations visibles dans les notifications. Les sous-types paiement/virement sont détectés uniquement quand le texte est assez explicite.
Les pictogrammes sont personnalisables sous forme d'émojis et symboles Unicode (pas encore d'import de fichiers graphiques).
Installation : APK → ouvrir l'application → autoriser l'accès aux notifications → ajouter le widget → choisir les pictogrammes.
