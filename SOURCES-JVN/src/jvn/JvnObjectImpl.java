package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private int jvnObjectId;
    private Serializable sharedObject;
    private transient JvnLocalServer jvnServer;
    private transient LockState lockState = LockState.NL;
    
    public enum LockState {
        NL,     // no local lock
        RLC,    // read lock cached
        WLC,    // write lock cached
        RLT,    // read lock taken
        WLT,    // write lock taken
        RLT_WLC // read lock taken - write lock cached
    }
    
    public JvnObjectImpl(int id, Serializable obj, JvnLocalServer server) {
        this.jvnObjectId = id;
        this.sharedObject = obj;
        this.jvnServer = server;
        if (this.lockState == null) {
            this.lockState = LockState.NL;
        }
    }
    

    
    // Méthode pour réinitialiser le serveur après désérialisation
    public void setJvnServer(JvnLocalServer server) {
        this.jvnServer = server;
        if (this.lockState == null) {
            this.lockState = LockState.NL;
        }
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (lockState) {
            case NL:
                sharedObject = ((JvnServerImpl)jvnServer).jvnLockRead(jvnObjectId);
                lockState = LockState.RLT;
                System.out.println("🔐 CLIENT: Verrou LECTURE obtenu objet " + jvnObjectId);
                break;
            case RLC:
                lockState = LockState.RLT;
                System.out.println("🔐 CLIENT: Verrou LECTURE (cache) objet " + jvnObjectId);
                break;
            case WLC:
                lockState = LockState.RLT_WLC;
                System.out.println("🔐 CLIENT: Verrou LECTURE (écriture→lecture) objet " + jvnObjectId);
                break;
            case RLT:
            case WLT:
            case RLT_WLC:
                throw new JvnException("Verrou déjà pris pour l'objet " + jvnObjectId + " dans l'état " + lockState);
            default:
                throw new JvnException("État invalide pour la prise du verrou de lecture: " + lockState);
        }
    }
    
    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        switch (lockState) {
            case NL:
                // SEULEMENT dans ce cas, demander au coordinateur
                sharedObject = ((JvnServerImpl)jvnServer).jvnLockWrite(jvnObjectId);
                lockState = LockState.WLT;
                System.out.println("✏️  CLIENT: Verrou ÉCRITURE obtenu (coordinateur) objet " + jvnObjectId);
                break;
            case RLC:
                // Upgrade lecture → écriture : DOIT demander au coordinateur
                // pour invalider les autres RLC et obtenir l'exclusivité
                sharedObject = ((JvnServerImpl)jvnServer).jvnLockWrite(jvnObjectId);
                lockState = LockState.WLT;
                System.out.println("✏️  CLIENT: Verrou ÉCRITURE obtenu (upgrade RLC→WLT) objet " + jvnObjectId);
                break;
            case WLC:
                // J'ai déjà le verrou d'écriture en cache, juste l'activer
                lockState = LockState.WLT;
                System.out.println("✏️  CLIENT: Verrou ÉCRITURE (cache) objet " + jvnObjectId);
                break;
            case RLT:
            case WLT:
            case RLT_WLC:
                throw new JvnException("Verrou déjà pris pour l'objet " + jvnObjectId + " dans l'état " + lockState);
            default:
                throw new JvnException("État invalide pour la prise du verrou d'écriture: " + lockState);
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        LockState oldState = lockState;
        switch (lockState) {
            case RLT:
                lockState = LockState.RLC;
                System.out.println("🔓 CLIENT: Libération LECTURE objet " + jvnObjectId + " (→cache)");
                break;
            case WLT:
                lockState = LockState.WLC;
                System.out.println("🔓 CLIENT: Libération ÉCRITURE objet " + jvnObjectId + " (→cache)");
                break;
            case RLT_WLC:
                lockState = LockState.WLC;
                System.out.println("🔓 CLIENT: Libération LECTURE objet " + jvnObjectId + " (garde écriture en cache)");
                break;
            case NL:
            case RLC:
            case WLC:
                return; // Pas de verrou actif à libérer
            default:
                throw new JvnException("État invalide pour la libération du verrou: " + lockState);
        }
        
        if (oldState != lockState) {
            notifyAll();
        }
    }
    

    
    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        return sharedObject;
    }
    
    @Override
    public int jvnGetObjectId() throws JvnException {
        return jvnObjectId;
    }
    
    // Méthodes d'invalidation appelées par le coordinateur
    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        switch (lockState) {
            case RLC:
                // Verrou en cache, invalidation immédiate
                lockState = LockState.NL;
                System.out.println("❌ CLIENT: LECTURE (cache) invalidée objet " + jvnObjectId);
                break;
            case RLT:
                // Verrou de lecture ACTIF - attendre la fin de la lecture
                while (lockState == LockState.RLT) {
                    try {
                        System.out.println("⏳ CLIENT: Attente fin de lecture objet " + jvnObjectId);
                        wait(); // Attendre que jvnUnLock() libère la lecture
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JvnException("Interruption pendant l'attente de fin de lecture", e);
                    }
                }
                // Après jvnUnLock(), on passe de RLT → RLC, puis on invalide
                if (lockState == LockState.RLC) {
                    lockState = LockState.NL;
                    System.out.println("❌ CLIENT: LECTURE invalidée (après attente) objet " + jvnObjectId);
                }
                break;
            case RLT_WLC:
                // Attendre que la lecture se termine
                while (lockState == LockState.RLT_WLC) {
                    try {
                        System.out.println("⏳ CLIENT: Attente fin de lecture (RLT_WLC) objet " + jvnObjectId);
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JvnException("Interruption pendant l'attente de fin de lecture", e);
                    }
                }
                // Après jvnUnLock(), on passe de RLT_WLC → WLC, garde juste le cache d'écriture
                if (lockState == LockState.WLC) {
                    System.out.println("❌ CLIENT: LECTURE invalidée (garde écriture) objet " + jvnObjectId);
                }
                break;
            default:
                // Pas de verrou de lecture à invalider
                break;
        }
    }
        
    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        Serializable obj = null;
        switch (lockState) {
            case WLC:
            case WLT:
                obj = sharedObject;
                lockState = LockState.NL;
                System.out.println("❌ CLIENT: ÉCRITURE invalidée objet " + jvnObjectId);
                notifyAll();
                break;
            case RLT_WLC:
                obj = sharedObject;
                lockState = LockState.RLT;
                System.out.println("❌ CLIENT: ÉCRITURE (cache) invalidée objet " + jvnObjectId);
                notifyAll();
                break;
            default:
                // Pas de verrou d'écriture à invalider
                break;
        }
        return obj;
    }
    
    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        Serializable obj = null;
        
        switch (lockState) {
            case WLC:
                obj = sharedObject;
                lockState = LockState.RLC;
                System.out.println("🔄 CLIENT: ÉCRITURE→LECTURE objet " + jvnObjectId);
                notifyAll();
                break;
            case WLT:
                // Attendre que le verrou soit libéré
                while (lockState == LockState.WLT) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JvnException("Interruption pendant l'attente de libération", e);
                    }
                }
                if (lockState == LockState.WLC) {
                    obj = sharedObject;
                    lockState = LockState.RLC;
                    System.out.println("🔄 CLIENT: ÉCRITURE→LECTURE (après attente) objet " + jvnObjectId);
                    notifyAll();
                }
                break;
            case RLT_WLC:
                obj = sharedObject;
                lockState = LockState.RLT;
                System.out.println("🔄 CLIENT: Suppression cache ÉCRITURE objet " + jvnObjectId);
                notifyAll();
                break;
            case NL:
            case RLC:
            case RLT:
                // Pas de verrou d'écriture à réduire
                break;
            default:
                throw new JvnException("État invalide pour la réduction d'écriture: " + lockState);
        }
        return obj;
    }
    
    // Méthode interne pour mettre à jour l'objet
    public void updateSharedObject(Serializable obj) {
        this.sharedObject = obj;
    }
}