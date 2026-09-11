# PiiWii Dev Bridge

Relais technique zéro-configuration pour PiiWii Dev Center 2.7+.

- `queue/current.json` contient uniquement une enveloppe chiffrée destinée à piiwii.ch.
- `payloads/` peut contenir des fragments chiffrés lorsque la commande est volumineuse.
- Aucun mot de passe, jeton, secret ni code source en clair ne doit être placé ici.
- Le résultat du déploiement est lu directement depuis l’endpoint public non sensible de Dev Center.

Le dépôt est public uniquement pour permettre à WordPress de lire la file sans jeton GitHub. La confidentialité du contenu est assurée par chiffrement côté client avant le commit.
