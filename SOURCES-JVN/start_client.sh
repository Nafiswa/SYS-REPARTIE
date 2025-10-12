#!/bin/bash

CLIENT_NAME=${1:-Client1}
OBJECT_NAME=${2:-IRC_SENTENCE}
DELAY=${3:-0}

echo "=== Démarrage du $CLIENT_NAME sur objet $OBJECT_NAME ==="

# Attendre si demandé
if [ $DELAY -gt 0 ]; then
    echo "Attente de $DELAY secondes avant de démarrer..."
    sleep $DELAY
fi

# Configuration RMI
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
export CLASSPATH="$SCRIPT_DIR/bin"
export JAVA_RMI_SERVER_HOSTNAME="127.0.0.1"

# Démarrer le client
echo "Démarrage du $CLIENT_NAME sur objet $OBJECT_NAME..."
java -cp "$SCRIPT_DIR/bin" \
     -Djava.rmi.server.hostname=127.0.0.1 \
     -Djvn.registry.host=127.0.0.1 \
     -Djvn.registry.port=1099 \
     -DclientName="$CLIENT_NAME" \
     TestIrcJvnManual "$OBJECT_NAME"

echo "$CLIENT_NAME terminé."