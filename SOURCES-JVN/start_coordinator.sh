#!/bin/bash

echo "=== Démarrage du Coordinateur Javanaise ==="

# Nettoyer les processus précédents
pkill -f "JvnCoordLauncher" 2>/dev/null || true
pkill -f "rmiregistry" 2>/dev/null || true

# Attendre un peu
sleep 1

# Compiler si nécessaire
echo "Compilation..."
javac -d bin src/annotation/*.java src/jvn/*.java src/irc/*.java src/*.java

# Configuration RMI
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
export CLASSPATH="$SCRIPT_DIR/bin"
export JAVA_RMI_SERVER_HOSTNAME="127.0.0.1"

# Démarrer le coordinateur
echo "Démarrage du coordinateur sur 127.0.0.1:1099..."
java -cp "$SCRIPT_DIR/bin" \
     -Djava.rmi.server.hostname=127.0.0.1 \
     -Djvn.registry.host=127.0.0.1 \
     -Djvn.registry.port=1099 \
     -Djvn.debug=true \
     JvnCoordLauncher

echo "Coordinateur arrêté."