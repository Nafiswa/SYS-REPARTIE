package irc;
import jvn.JvnException;
import annotation.*;

public interface AnnotationSentence {

    @Read
    String read()throws JvnException;

    @Write
    void write(String text)throws JvnException;
}