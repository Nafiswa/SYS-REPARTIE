/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

import annotation.Read;
import annotation.Write;
import jvn.JvnException;

public class Sentence implements AnnotationSentence, java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public Sentence() {
		data = new String("");
	}
	@Override
	@Write
	public void write(String text) {
		data = text;
	}
	@Override
	@Read
	public String read() {
		return data;	
	}
	@Override
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
	@Override
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