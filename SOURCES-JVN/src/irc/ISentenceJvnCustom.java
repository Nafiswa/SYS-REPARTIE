package irc;

import jvn.JvnException;

public interface ISentenceJvnCustom {
    void write(String text);
    String read();
    void simulateLongWriteOperation(String text, long durationMs) throws JvnException;
    String simulateLongReadOperation(long durationMs) throws JvnException;
}