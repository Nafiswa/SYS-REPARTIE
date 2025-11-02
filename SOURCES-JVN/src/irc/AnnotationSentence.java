package irc;
import annotation.*;
import jvn.JvnException;

public interface AnnotationSentence {

    @Read
    String read()throws JvnException;

    @Write
    void write(String text)throws JvnException;
    
    @Write
    void writeSlow(String text, int seconds) throws JvnException;
    
    @Read
    String readSlow(int seconds) throws JvnException;
}