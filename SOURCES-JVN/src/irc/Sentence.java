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
	@Read
	public String read() {
		return data;	
	}
	
}