



Pour les extensions, comme cité dans la soutenance, on a toujours quelques soucis
mais la gestion de panne du coordinateur fonctionne.
Pour trouver cette version , il est sur la branche dev et a compilé de la même manière (les instructions ci-dessous)


## Lancement
1. **Terminal 1:** `./start_coordinator.sh`
2. **Terminal 2:** `./start_client.sh Client1 IRC_SENTENCE`
3. **Terminal 3:** `./start_client.sh Client2 IRC_SENTENCE`
3. **Terminal N:** `./start_client.sh Client<N> <Nom de l'objet>`


## Commandes clients
- `r` : Lire | `w <texte>` : Écrire | `q` : Quitter
- `w <texte> < x:secondes>` : Écriture avec verrou long x seconde
- `r < x:secondes>` : lecture avec verrou long x seconde
- `obj <nom>` : Changer d'objet

## Test rapide
1. Client1: `w Hello Client1`
2. Client2: `r` → doit voir "Hello Client1"
4. Client2: `w Test` → doit attendre que Client1 libère
4. Client2: `w Test2 20` → doit attendre que Client1 libère pendant 20 secondes


## Autres objets disponibles
- `IRC_SENTENCE` | `DOCUMENT_1` | `SHARED_TEXT`