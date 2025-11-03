package irc;

import annotation.Read;
import annotation.Write;

public class Sentence implements AnnotationSentence, java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public Sentence() {
		data = new String("");
	}
	@Write
	public void write(String text) {
		data = text;
	}
	
	@Write
	public void writeSlow(String text, int seconds) {
		data = text;
		System.out.println("⏱️  Sentence.writeSlow: Garde le verrou pendant " + seconds + "s...");
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("⚠️  Sentence.writeSlow: Interrompu");
		}
		System.out.println("✅ Sentence.writeSlow: Sleep terminé, le verrou va être libéré");
	}
	
	@Read
	public String read() {
		return data;	
	}
	
	@Read
	public String readSlow(int seconds) {
		String result = data;
		System.out.println("⏱️  Sentence.readSlow: Garde le verrou pendant " + seconds + "s...");
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("⚠️  Sentence.readSlow: Interrompu");
		}
		System.out.println("✅ Sentence.readSlow: Sleep terminé, le verrou va être libéré");
		return result;
	}
	
}