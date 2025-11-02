#!/bin/bash

echo "=== Test Javanaise Version 1 ==="


# Configuration RMI
RMI_PORT=10999  # Utiliser un port différent pour éviter les conflits
RMI_HOST="127.0.0.1"
RMI_PROPS="-Djava.rmi.server.hostname=${RMI_HOST}"
RMI_PROPS+=" -Djvn.registry.host=${RMI_HOST}"
RMI_PROPS+=" -Djvn.registry.port=${RMI_PORT}"
RMI_PROPS+=" -Dsun.rmi.transport.tcp.responseTimeout=30000"
RMI_PROPS+=" -Dsun.rmi.transport.tcp.readTimeout=30000"

# Compiler les classes
echo "Compilation..."
javac -d bin src/annotation/*.java src/jvn/*.java src/irc/*.java src/*.java

# Arrêter les processus précédents
echo "Nettoyage des processus précédents..."
pkill -f "rmiregistry.*${RMI_PORT}" || true
pkill -f JvnCoordLauncher || true
sleep 2

# S'assurer qu'aucun processus n'utilise le port
lsof -ti :${RMI_PORT} | xargs kill -9 2>/dev/null || true
sleep 1

# Démarrer le coordinateur
echo "Démarrage du coordinateur..."
java ${RMI_PROPS} -cp bin JvnCoordLauncher &
COORD_PID=$!

# Attendre que le coordinateur soit prêt
echo "Attente de l'initialisation du coordinateur..."
for i in {1..10}; do
    echo -n "."
    sleep 1
done
echo ""

# Lancer le test avec les mêmes propriétés RMI
echo "Lancement du test..."
java ${RMI_PROPS} -cp bin TestIrcJvn

# Nettoyage
echo -e "\nArrêt des services..."

# Tuer le processus du coordinateur
if [ ! -z "$COORD_PID" ]; then
    kill $COORD_PID 2>/dev/null || kill -9 $COORD_PID 2>/dev/null
fi

# Tuer tous les processus Java liés au test
pkill -f "java.*bin.*JvnCoordLauncher" 2>/dev/null
pkill -f "java.*bin.*TestIrcJvn" 2>/dev/null

# Nettoyer le registry RMI
pkill -f "rmiregistry.*${RMI_PORT}" 2>/dev/null

# S'assurer que le port est libéré
lsof -ti :${RMI_PORT} | xargs kill -9 2>/dev/null || true

echo "Test terminé."