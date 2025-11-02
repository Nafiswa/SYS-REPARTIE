package irc;
import jvn.JvnException;
import annotation.*;

public interface AnnotationSentence {

    @Read
    String read()throws JvnException;

    @Write
    void write(String text)throws JvnException;
    @Read
    public String simulateLongReadOperation(long durationMs) throws JvnException ;
    @Write
    public void simulateLongWriteOperation(String text, long durationMs) throws JvnException;
}