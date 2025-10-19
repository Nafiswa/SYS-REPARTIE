package irc;
import jvn.JvnException;
public interface AnnotationSentence {

    String read()throws JvnException;

    void write(String text)throws JvnException;
}