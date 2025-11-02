/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */ 

package jvn;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord {
    
    private static final String SAVE_FILE = "coordinator_state.bin";
	
	@Override
	public void jvnPing() throws RemoteException {
		// Cette méthode ne fait rien, elle sert juste à vérifier si le coordinateur est vivant
		// Si le coordinateur est mort, une RemoteException sera lancée automatiquement par RMI
	}
    
    // Sauvegarde l'état du coordinateur sur le disque
    private synchronized void saveState() {
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream(SAVE_FILE))) {
            // Créer un objet qui contient tout l'état
            java.util.HashMap<String, Object> state = new java.util.HashMap<>();
            state.put("nextObjectId", nextObjectId.get());
            state.put("objects", objects);
            state.put("nameToId", nameToId);
            
            // Sauvegarder l'état
            out.writeObject(state);
            System.out.println("💾 COORD: État sauvegardé sur disque");
        } catch (Exception e) {
            System.err.println("❌ COORD: Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    // Restaure l'état du coordinateur depuis le disque
    @SuppressWarnings("unchecked")
    private synchronized void loadState() {
        java.io.File file = new java.io.File(SAVE_FILE);
        if (!file.exists()) {
            System.out.println("ℹ️ COORD: Pas de fichier d'état existant, démarrage à zéro");
            return;
        }
        
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.FileInputStream(file))) {
            // Lire l'état
            java.util.HashMap<String, Object> state = 
                (java.util.HashMap<String, Object>) in.readObject();
            
            // Restaurer l'état
            this.nextObjectId.set((Integer) state.get("nextObjectId"));
            this.objects = (ConcurrentHashMap<Integer, ObjectInfo>) state.get("objects");
            this.nameToId = (ConcurrentHashMap<String, Integer>) state.get("nameToId");
            
            // Réinitialiser les connexions car elles ne sont pas sérialisables
            for (ObjectInfo info : objects.values()) {
                // Réinitialiser complètement l'état de verrouillage
                info.writer = null;
                info.writerId = null;
                info.readers = java.util.concurrent.ConcurrentHashMap.newKeySet();
                info.readerIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
                
                System.out.println("🔓 COORD: Réinitialisation des verrous pour l'objet");
            }
            
            System.out.println("📂 COORD: État restauré depuis le disque");
            System.out.println("   - " + objects.size() + " objets");
            System.out.println("   - Prochain ID: " + nextObjectId.get());
        } catch (Exception e) {
            System.err.println("❌ COORD: Erreur lors de la restauration: " + e.getMessage());
            // En cas d'erreur, on repart à zéro
            this.nextObjectId = new AtomicInteger(1);
            this.objects = new ConcurrentHashMap<>();
            this.nameToId = new ConcurrentHashMap<>();
        }
    }

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
  
  private AtomicInteger nextObjectId;
  private ConcurrentHashMap<Integer, ObjectInfo> objects;
  private ConcurrentHashMap<String, Integer> nameToId;
  
  // Classe interne pour les informations d'objets
  private static class ObjectInfo implements Serializable {
      private static final long serialVersionUID = 1L;
      
      public Serializable object;
      public transient JvnRemoteServer writer;  // transient car RMI n'est pas sérialisable
      public String writerId;
      public transient java.util.Set<JvnRemoteServer> readers;  // transient car RMI n'est pas sérialisable
      public java.util.Set<String> readerIds;
      
      public ObjectInfo(Serializable obj) {
          this.object = obj;
          this.writer = null;
          this.writerId = null;
          this.readers = java.util.concurrent.ConcurrentHashMap.newKeySet();
          this.readerIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
      }
      
      // Vérifie si un serveur est toujours vivant
      public boolean isServerAlive(JvnRemoteServer server) {
          if (server == null) return false;
          try {
              server.getServerId();
              return true;
          } catch (RemoteException e) {
              return false;
          }
      }
      
      // Nettoie les références aux serveurs morts
      public void cleanDeadServers() {
          boolean hasCleanedSomething = false;
          
          // Vérifie le writer
          if (writer != null) {
              try {
                  writer.getServerId();
              } catch (RemoteException e) {
                  System.out.println("🔥 COORD: Writer " + writerId + " ne répond plus, nettoyage...");
                  writer = null;
                  writerId = null;
                  hasCleanedSomething = true;
              }
          }
          
          // Vérifie les readers
          int initialSize = readers.size();
          readers.removeIf(server -> {
              try {
                  server.getServerId();
                  return false;
              } catch (RemoteException e) {
                  System.out.println("🔥 COORD: Un reader ne répond plus, suppression...");
                  return true;
              }
          });
          
          if (readers.size() < initialSize) {
              hasCleanedSomething = true;
              System.out.println("🧹 COORD: " + (initialSize - readers.size()) + " readers morts ont été nettoyés");
          }
          
          if (!hasCleanedSomething) {
              System.out.println("✅ COORD: Tous les clients sont actifs (" + readers.size() + " readers, writer: " + (writer != null ? "oui" : "non") + ")");
          }
      }
  }

/**
  * Default constructor
  * @throws JvnException
  **/
	public JvnCoordImpl() throws RemoteException {
		super();
		
		// Initialiser les structures
		this.nextObjectId = new AtomicInteger(1);
		this.objects = new ConcurrentHashMap<Integer, ObjectInfo>();
		this.nameToId = new ConcurrentHashMap<String, Integer>();
		
		// Restaurer l'état depuis le disque
		loadState();
        System.out.println("COORDINATEUR: Démarré");
        
        // Démarrer le thread de nettoyage des serveurs morts et de sauvegarde
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    // Nettoyer tous les objets
                    System.out.println("\n🔍 COORD: Vérification des clients...");
                    for (ObjectInfo info : objects.values()) {
                        info.cleanDeadServers();
                    }
                    
                    // Sauvegarder l'état périodiquement
                    saveState();
                    
                    Thread.sleep(2000); // Vérifier et sauvegarder toutes les 2 secondes
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "JVN-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    int id = nextObjectId.getAndIncrement();
    System.out.println("COORDINATEUR: Nouvel ID généré - " + id);
    return id;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) 
  throws java.rmi.RemoteException,jvn.JvnException{
    try {
      // Vérifier si l'objet existe déjà
      if (nameToId.containsKey(jon)) {
          System.out.println("COORDINATEUR: Objet '" + jon + "' déjà enregistré");
          return;
      }

      int joi = jo.jvnGetObjectId();
      Serializable o = jo.jvnGetSharedObject();
      System.out.println("COORDINATEUR: Enregistrement objet '" + jon + "' avec ID " + joi);
      
      // Sauvegarder l'objet et son mapping
      ObjectInfo info = new ObjectInfo(o);
      objects.put(joi, info);
      nameToId.put(jon, joi);
      
      // Sauvegarder l'état après modification
      saveState();
      
      System.out.println("COORDINATEUR: Objet enregistré avec succès");
    } catch (JvnException e) {
      System.err.println("COORDINATEUR: Erreur lors de l'enregistrement de l'objet: " + e.getMessage());
      throw new RemoteException("Erreur lors de l'enregistrement", e);
    }
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    System.out.println("COORDINATEUR: Recherche objet '" + jon + "'");
    Integer joi = nameToId.get(jon);
    if (joi == null) {
        System.out.println("COORDINATEUR: Objet '" + jon + "' non trouvé");
        throw new JvnException("Objet non trouvé: " + jon);
    }
    
    // Vérifier si l'objet existe toujours
    ObjectInfo info = objects.get(joi);
    if (info == null) {
        System.out.println("COORDINATEUR: ID " + joi + " invalide pour l'objet '" + jon + "'");
        throw new JvnException("Objet inexistant: " + joi);
    }
    
    System.out.println("COORDINATEUR: Objet '" + jon + "' trouvé avec ID " + joi);
    // Retourner un objet temporaire contenant juste l'ID pour que le client puisse le récupérer
    try {
        return new JvnObjectImpl(joi, null, null);
    } catch (Exception e) {
        throw new JvnException("Erreur création objet temporaire", e);
    }
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js) throws RemoteException {
    System.out.println("📖 COORD: Demande LECTURE objet " + joi);
    ObjectInfo info = objects.get(joi);
    if (info == null) {
      throw new RemoteException("Objet inexistant: " + joi);
    }
    
    String jsId;
    try {
        jsId = js.getServerId();
    } catch (RemoteException e) {
        throw new RemoteException("Impossible d'obtenir l'ID du serveur", e);
    }
    
    // Si quelqu'un a le verrou en écriture, il faut l'invalider
    if (info.writer != null) {
        try {
            System.out.println("🔄 COORD: → Invalide ÉCRITURE sur objet " + joi + " (pour permettre lecture)");
            Serializable obj = info.writer.jvnInvalidateWriterForReader(joi);
            if (obj != null) {
                info.object = obj;
            }
            info.readers.add(info.writer);
            info.readerIds.add(info.writerId);
            info.writer = null;
            info.writerId = null;
        } catch (JvnException e) {
            throw new RemoteException("Erreur d'invalidation pour lecture", e);
        }
    }
    
    info.readers.add(js);
    info.readerIds.add(jsId);
    System.out.println("✅ COORD: LECTURE accordée objet " + joi);
    return info.object;
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws RemoteException {
    System.out.println("✏️  COORD: Demande ÉCRITURE objet " + joi);
    ObjectInfo info = objects.get(joi);
    if (info == null) {
      throw new RemoteException("Objet inexistant: " + joi);
    }
    
    String jsId;
    try {
        jsId = js.getServerId();
    } catch (RemoteException e) {
        throw new RemoteException("Impossible d'obtenir l'ID du serveur", e);
    }
    
    // Si ce serveur a déjà le verrou d'écriture, juste le lui donner
    if (info.writerId != null && info.writerId.equals(jsId)) {
        System.out.println("✅ COORD: ÉCRITURE déjà possédée par ce client objet " + joi);
        return info.object;
    }
    
    // Invalider tous les lecteurs SAUF celui qui demande le verrou d'écriture
    int invalidatedReaders = 0;
    for (JvnRemoteServer reader : info.readers) {
        String readerId;
        try {
            readerId = reader.getServerId();
        } catch (RemoteException e) {
            System.err.println("❌ COORD: Erreur obtention ID lecteur: " + e.getMessage());
            continue;
        }
        
        if (!readerId.equals(jsId)) {
            try {
                System.out.println("🔄 COORD: → Invalide LECTURE d'un autre client sur objet " + joi + " (client " + readerId + ")");
                reader.jvnInvalidateReader(joi);
                invalidatedReaders++;
            } catch (JvnException e) {
                System.err.println("❌ COORD: Erreur invalidation lecteur: " + e.getMessage());
            }
        } else {
            System.out.println("🔄 COORD: → Skip invalidation (même client upgrade lecture→écriture) objet " + joi + " (client " + readerId + ")");
        }
    }
    if (invalidatedReaders > 0) {
        System.out.println("🔄 COORD: " + invalidatedReaders + " lecteur(s) invalidé(s)");
    }
    info.readers.clear();
    info.readerIds.clear();
    
    // Si un AUTRE serveur a le verrou en écriture, il faut l'invalider
    if (info.writer != null && info.writerId != null && !info.writerId.equals(jsId)) {
        try {
            System.out.println("🔄 COORD: → Invalide ÉCRITURE d'un autre client sur objet " + joi + " (client " + info.writerId + ")");
            Serializable obj = info.writer.jvnInvalidateWriter(joi);
            if (obj != null) {
                info.object = obj;
            }
        } catch (JvnException e) {
            throw new RemoteException("Erreur d'invalidation du verrou d'écriture", e);
        }
    }
    
    info.writer = js;
    info.writerId = jsId;
    System.out.println("✅ COORD: ÉCRITURE accordée objet " + joi + " (client " + jsId + ")");
    return info.object;
   }
   
   /**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, jvn.JvnException {
	 // to be completed
    }
    

}


