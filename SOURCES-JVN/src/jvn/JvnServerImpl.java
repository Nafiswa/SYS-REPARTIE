/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a JVN server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import irc.Sentence;

public class JvnServerImpl extends UnicastRemoteObject implements JvnRemoteServer, JvnLocalServer {
    private static JvnServerImpl instance;
    private JvnRemoteCoord coordinator;
    private ConcurrentHashMap<Integer, JvnObjectImpl> localObjects = new ConcurrentHashMap<>();
    private final String serverId;
    
    private JvnServerImpl() throws RemoteException {
        super();
        
        // Générer un identifiant unique pour ce serveur
        this.serverId = "Server-" + System.currentTimeMillis() + "-" + System.nanoTime();
        
        // Lire la configuration RMI depuis les propriétés système
        final String host = System.getProperty("jvn.registry.host", "127.0.0.1");
        final int port = Integer.getInteger("jvn.registry.port", 1099);
        
        // Forcer l'IP locale pour éviter les soucis de résolution
        if (System.getProperty("java.rmi.server.hostname") == null) {
            System.setProperty("java.rmi.server.hostname", host);
        }
        
        connectToCoordinator(host, port);
        
        // Démarrer le thread de surveillance de la connexion
        Thread healthCheck = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000); // Vérifier toutes les 3 secondes
                    if (coordinator != null) {
                        try {
                            coordinator.jvnPing(); // Test de connexion sans effet de bord
                        } catch (Exception e) {
                            System.out.println("❌ SERVER: Perte de connexion au coordinateur, tentative de reconnexion...");
                            try {
                                connectToCoordinator(host, port);
                            } catch (RemoteException re) {
                                System.out.println("❌ SERVER: Échec de la reconnexion: " + re.getMessage());
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "JVN-HealthCheck");
        healthCheck.setDaemon(true);
        healthCheck.start();
    }
    
    private void connectToCoordinator(String host, int port) throws RemoteException {
        System.out.println("SERVER: Tentative de connexion au coordinateur sur " + host + ":" + port);
        
        Exception last = null;
        for (int i = 0; i < 15; i++) { // Augmentation du nombre de tentatives
            try {
                Registry registry = LocateRegistry.getRegistry(host, port);
                coordinator = (JvnRemoteCoord) registry.lookup("JvnCoordinator");
                
                // Réinitialiser les états des objets locaux après reconnexion
                System.out.println("🔄 SERVER: Réinitialisation des objets après reconnexion...");
                for (JvnObjectImpl obj : localObjects.values()) {
                    obj.resetLockState();  // On va ajouter cette méthode
                }
                
                System.out.println("✅ SERVER: Connecté au coordinateur");
                return;
            } catch (Exception e) {
                last = e;
                System.out.println("SERVER: Tentative " + (i+1) + " échouée, nouvelle tentative dans 1s...");
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        
        if (last != null) {
            System.err.println("SERVER: Échec de connexion après 15 tentatives");
            throw new RemoteException("Impossible de se connecter au coordinateur après plusieurs tentatives", last);
        }
    }
    
    public static synchronized JvnServerImpl jvnGetServer() {
        if (instance == null) {
            try {
                instance = new JvnServerImpl();
            } catch (RemoteException e) {
                throw new RuntimeException("Erreur création serveur JVN", e);
            }
        }
        return instance;
    }
    
    @Override
    public JvnObject jvnCreateObject(Serializable jos) throws JvnException {
        try {
            int objectId = coordinator.jvnGetObjectId();
            JvnObjectImpl jvnObj = new JvnObjectImpl(objectId, jos, this);
            localObjects.put(objectId, jvnObj);
            //return jvnObj;
            return (JvnObject) JvnProxy.newInstance(jvnObj);
        } catch (RemoteException e) {
            throw new JvnException("Erreur communication coordinateur", e);
        }
    }
    
    @Override
    public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
        try {
            coordinator.jvnRegisterObject(jon, jo, this);
        } catch (RemoteException e) {
            throw new JvnException("Erreur enregistrement objet", e);
        }
    }
    
    @Override
    public JvnObject jvnLookupObject(String jon) throws JvnException {
        try {
            // Récupérer l'ID de l'objet existant depuis le coordinateur
            JvnObject existingObject = coordinator.jvnLookupObject(jon, this);
            if (existingObject != null) {
                // Si l'objet est trouvé, créer une instance locale avec le même ID
                int existingId = existingObject.jvnGetObjectId();
                System.out.println("SERVER: Objet '" + jon + "' trouvé avec ID " + existingId);
                
                // Créer une nouvelle instance locale en état NL (pas de copie locale)
                // Cela force la récupération de l'objet au premier verrou
                JvnObjectImpl newJvnObj = new JvnObjectImpl(existingId, null, this);
                
                // Stocker dans localObjects pour les invalidations du coordinateur
                localObjects.put(existingId, newJvnObj);
                return (JvnObject) JvnProxy.newInstance(newJvnObj);
            }
            throw new JvnException("Objet non trouvé: " + jon);
        } catch (RemoteException e) {
            throw new JvnException("Erreur lors de la recherche de l'objet: " + jon, e);
        }
    }
    
    // Méthodes appelées par les objets JVN pour demander des verrous
    public Serializable jvnLockRead(int joi) throws JvnException {
        try {
            return coordinator.jvnLockRead(joi, this);
        } catch (RemoteException e) {
            throw new JvnException("Erreur demande verrou lecture", e);
        }
    }
    
    public Serializable jvnLockWrite(int joi) throws JvnException {
        try {
            return coordinator.jvnLockWrite(joi, this);
        } catch (RemoteException e) {
            throw new JvnException("Erreur demande verrou écriture", e);
        }
    }
    
    @Override
    public void jvnTerminate() throws JvnException {
        try {
            coordinator.jvnTerminate(this);
        } catch (RemoteException e) {
            throw new JvnException("Erreur lors de la terminaison", e);
        }
    }
    
    // Méthodes JvnRemoteServer (appelées par le coordinateur)
    @Override
    public void jvnInvalidateReader(int joi) throws RemoteException, JvnException {
        System.out.println("🔄 SERVER: Reçu invalidation LECTURE objet " + joi);
        JvnObjectImpl obj = localObjects.get(joi);
        if (obj != null) {
            obj.jvnInvalidateReader();
        }
    }
    
    @Override
    public Serializable jvnInvalidateWriter(int joi) throws RemoteException, JvnException {
        System.out.println("🔄 SERVER: Reçu invalidation ÉCRITURE objet " + joi);
        JvnObjectImpl obj = localObjects.get(joi);
        if (obj != null) {
            return obj.jvnInvalidateWriter();
        }
        return null;
    }
    
    @Override
    public Serializable jvnInvalidateWriterForReader(int joi) throws RemoteException, JvnException {
        System.out.println("🔄 SERVER: Reçu réduction ÉCRITURE→LECTURE objet " + joi);
        JvnObjectImpl obj = localObjects.get(joi);
        if (obj != null) {
            return obj.jvnInvalidateWriterForReader();
        }
        return null;
    }
    
    @Override
    public String getServerId() throws RemoteException {
        return this.serverId;
    }
    
    public JvnRemoteCoord getCoordinator() {
        return this.coordinator;
    }
    
    @Override
    public void jvnFlushObject(int joi) throws JvnException {
        System.out.println("🧹 SERVER: Flushing objet " + joi);
        JvnObjectImpl obj = localObjects.get(joi);
        if (obj != null) {
            // Si l'objet est en mode écriture (WLC ou WLT), on doit d'abord le libérer
            if (obj.getLockState() == JvnObjectImpl.LockState.WLC || 
                obj.getLockState() == JvnObjectImpl.LockState.WLT) {
                throw new JvnException("Impossible de flusher un objet en mode écriture. Libérez d'abord le verrou d'écriture.");
            }
            // Retirer l'objet du cache local
            localObjects.remove(joi);
            System.out.println("✨ SERVER: Objet " + joi + " retiré du cache local");
        }
    }
    
}


