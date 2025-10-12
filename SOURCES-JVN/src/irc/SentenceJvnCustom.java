package irc;

import java.io.Serializable;
import jvn.JvnException;
import jvn.JvnLocalServer;
import jvn.JvnObject;
import jvn.JvnServerImpl;

/**
 * Version JVN de Sentence configurable avec nom d'objet personnalisé
 */
public class SentenceJvnCustom implements Serializable {
    private JvnObject jvnSentence;
    private JvnLocalServer jvnServer;
    private String objectName;
    
    public SentenceJvnCustom(String objectName) throws JvnException {
        this.objectName = objectName;
        // Récupérer le serveur JVN
        jvnServer = JvnServerImpl.jvnGetServer();
        
        try {
            // Essayer d'abord de récupérer l'objet s'il existe déjà
            jvnSentence = jvnServer.jvnLookupObject(objectName);
            // L'objet existe - PAS de verrou automatique pour éviter les invalidations
            System.out.println("Objet JVN '" + objectName + "' existant récupéré avec ID: " + jvnSentence.jvnGetObjectId());
        } catch (JvnException e) {
            if (!e.getMessage().contains("non trouvé")) {
                throw e;
            }
            // Si l'objet n'existe pas, le créer et l'enregistrer
            Sentence sentence = new Sentence();
            jvnSentence = jvnServer.jvnCreateObject(sentence);
            jvnServer.jvnRegisterObject(objectName, jvnSentence);
            System.out.println("Nouvel objet JVN '" + objectName + "' créé et enregistré avec ID: " + jvnSentence.jvnGetObjectId());
        }
    }
    
    public void write(String text) throws JvnException {
        jvnSentence.jvnLockWrite();
        try {
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            sentence.write(text);
            System.out.println("📝 CLIENT: Écrit '" + text + "' sur " + objectName);
        } finally {
            jvnSentence.jvnUnLock();
        }
    }
    
    public String read() throws JvnException {
        jvnSentence.jvnLockRead();
        try {
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            String result = sentence.read();
            System.out.println("📖 CLIENT: Lu '" + result + "' sur " + objectName);
            return result;
        } finally {
            jvnSentence.jvnUnLock();
        }
    }
    
    /**
     * Simule une opération d'écriture longue qui garde le verrou pendant toute la durée
     */
    public void simulateLongWriteOperation(String text, long durationMs) throws JvnException {
        System.out.println("⏳ CLIENT: Début traitement LONG (" + durationMs/1000 + "s) sur " + objectName);
        
        jvnSentence.jvnLockWrite();
        try {
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            sentence.write(text);
            System.out.println("📝 CLIENT: Écrit '" + text + "' - GARDE LE VERROU...");
            
            try {
                Thread.sleep(durationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException("Interruption pendant le traitement long", e);
            }
            
            System.out.println("✅ CLIENT: Traitement long TERMINÉ sur " + objectName);
        } finally {
            jvnSentence.jvnUnLock();
        }
    }
    
    public String getObjectName() {
        return objectName;
    }
    
    public int getObjectId() throws JvnException {
        return jvnSentence.jvnGetObjectId();
    }
}