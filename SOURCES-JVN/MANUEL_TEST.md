
## Lancement
1. **Terminal 1:** `./start_coordinator.sh`
2. **Terminal 2:** `./start_client.sh Client1 IRC_SENTENCE`
3. **Terminal 3:** `./start_client.sh Client2 IRC_SENTENCE`
3. **Terminal N:** `./start_client.sh Client<N> <Nom de l'objet>`


## Commandes clients
- `r` : Lire | `w <texte>` : Écrire | `q` : Quitter
- `long <texte> < x:secondes>` : Écriture avec verrou long x seconde
- `obj <nom>` : Changer d'objet

## Test rapide
1. Client1: `w Hello Client1`
2. Client2: `r` → doit voir "Hello Client1"
3. Client1: `long "Working..." 5` → garde le verrou 5s
4. Client2: `w Test` → doit attendre que Client1 libère

## Autres objets disponibles
- `IRC_SENTENCE` | `DOCUMENT_1` | `SHARED_TEXT`