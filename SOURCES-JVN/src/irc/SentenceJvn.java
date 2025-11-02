package irc;

import java.io.Serializable;
import jvn.JvnException;
import jvn.JvnLocalServer;
import jvn.JvnObject;
import jvn.JvnServerImpl;
import jvn.JvnObjectImpl;
import annotation.Read;
import annotation.Write;


/**
 * Version JVN de Sentence pour démonstration
 */
public class SentenceJvn implements Serializable {
    private JvnObject jvnSentence;
    private JvnLocalServer jvnServer;
    
    public SentenceJvn() throws JvnException {
        // Récupérer le serveur JVN
        jvnServer = JvnServerImpl.jvnGetServer();
        
        try {
            // Essayer d'abord de récupérer l'objet s'il existe déjà
            jvnSentence = jvnServer.jvnLookupObject("IRC_SENTENCE");
            // L'objet existe, on doit obtenir son contenu avec un verrou de lecture
            jvnSentence.jvnLockRead();
            try {
                if (jvnSentence.jvnGetSharedObject() == null) {
                    ((JvnObjectImpl)jvnSentence).updateSharedObject(new Sentence());
                }
            } finally {
                jvnSentence.jvnUnLock();
            }
            System.out.println("Sentence JVN existante récupérée avec ID: " + jvnSentence.jvnGetObjectId());
        } catch (JvnException e) {
            if (!e.getMessage().contains("non trouvé")) {
                throw e;
            }
            // Si l'objet n'existe pas, le créer et l'enregistrer
            Sentence sentence = new Sentence();
            jvnSentence = jvnServer.jvnCreateObject(sentence);
            jvnServer.jvnRegisterObject("IRC_SENTENCE", jvnSentence);
            System.out.println("Nouvelle Sentence JVN créée et enregistrée avec ID: " + jvnSentence.jvnGetObjectId());
        }
    }
    @Write
    public void write(String text) throws JvnException {
        System.out.println("CLIENT: Demande écriture - '" + text + "'");
        
        // Version 1 : Gestion explicite des verrous
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            sentence.write(text);
            System.out.println("CLIENT: Écriture effectuée");
    }
    @Read
    public String read() throws JvnException {
        System.out.println("CLIENT: Demande lecture");
        
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            String result = sentence.read();
            System.out.println("CLIENT: Lecture effectuée - '" + result + "'");
            return result;
    }
    
    /**
     * Simule une opération d'écriture longue qui garde le verrou pendant toute la durée
     */
    public void simulateLongWriteOperation(String text, long durationMs) throws JvnException {
        System.out.println("CLIENT: Début opération longue avec verrou - '" + text + "'");
        
        // Prendre le verrou d'écriture et le garder pendant toute l'opération
        jvnSentence.jvnLockWrite();
        try {
            Sentence sentence = (Sentence) jvnSentence.jvnGetSharedObject();
            sentence.write(text);
            System.out.println("CLIENT: Écriture effectuée, traitement en cours...");
            
            // Simuler un traitement long AVEC le verrou
            try {
                Thread.sleep(durationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException("Interruption pendant le traitement long", e);
            }
            
            System.out.println("CLIENT: Traitement long terminé, libération du verrou");
        } finally {
            jvnSentence.jvnUnLock();
        }
    }
}