package irc;

import annotation.Read;
import annotation.Write;
import java.io.Serializable;
import jvn.JvnException;
import jvn.JvnObject;
import jvn.JvnServerImpl;

/**
 * Version JVN de Sentence configurable avec nom d'objet personnalisé utilisant les annotations et proxy
 */
public class SentenceJvnCustom implements ISentenceJvnCustom, Serializable {
    private static final long serialVersionUID = 1L;
    private String data;
    
    public SentenceJvnCustom() {
        this.data = "";
    }

    public static ISentenceJvnCustom createInstance(String objectName) throws JvnException {
        try {
            // Essayer d'abord de récupérer l'objet existant
            JvnObject obj = JvnServerImpl.jvnGetServer().jvnLookupObject(objectName);
            if (obj != null) {
                Serializable sharedObj = obj.jvnGetSharedObject();
                if (sharedObj instanceof ISentenceJvnCustom) {
                    return (ISentenceJvnCustom) sharedObj;
                }
            }
            
            // L'objet n'existe pas ou n'est pas du bon type, on le crée
            SentenceJvnCustom sentence = new SentenceJvnCustom();
            obj = JvnServerImpl.jvnGetServer().jvnCreateObject(sentence);
            JvnServerImpl.jvnGetServer().jvnRegisterObject(objectName, obj);
            return sentence;
        } catch (JvnException e) {
            throw new JvnException("Erreur lors de la création/récupération de l'objet JVN", e);
        }
    }

    @Write
    public void write(String text) {
        this.data = text;
        System.out.println("📝 CLIENT: Écrit '" + text + "'");
    }

    @Read
    public String read() {
        System.out.println("📖 CLIENT: Lu '" + data + "'");
        return data;
    }
    
    @Write
    public void writeSlow(String text, int seconds) throws JvnException {
        this.data = text;
        System.out.println("📝 CLIENT: Écrit '" + text + "' - Garde verrou " + seconds + "s");
        try {
            Thread.sleep(seconds * 1000L);
            System.out.println("✅ CLIENT: Écriture longue TERMINÉE");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JvnException("Interruption pendant l'écriture longue", e);
        }
    }
    
    @Read
    public String readSlow(int seconds) throws JvnException {
        System.out.println("📖 CLIENT: Lu '" + data + "' - Garde verrou " + seconds + "s");
        try {
            Thread.sleep(seconds * 1000L);
            System.out.println("✅ CLIENT: Lecture longue TERMINÉE");
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JvnException("Interruption pendant la lecture longue", e);
        }
    }

    @Write
    public void simulateLongWriteOperation(String text, long durationMs) throws JvnException {
        System.out.println("⏳ CLIENT: Début traitement LONG (" + durationMs/1000 + "s)");
        write(text);
        System.out.println("📝 CLIENT: Écrit '" + text + "' - Simulation longue opération...");
        
        try {
            Thread.sleep(durationMs);
            System.out.println("✅ CLIENT: Traitement long TERMINÉ");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JvnException("Interruption pendant le traitement long", e);
        }
    }

    @Read
    public String simulateLongReadOperation(long durationMs) throws JvnException {
        System.out.println("⏳ CLIENT: Début lecture LONGUE (" + durationMs/1000 + "s)");
        String result = read();
        System.out.println("📖 CLIENT: Lu '" + result + "' - Simulation longue opération...");
        
        try {
            Thread.sleep(durationMs);
            System.out.println("✅ CLIENT: Lecture longue TERMINÉE");
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JvnException("Interruption pendant la lecture longue", e);
        }
    }
}