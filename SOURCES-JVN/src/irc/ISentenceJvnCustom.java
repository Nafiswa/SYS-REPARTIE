package irc;

import jvn.JvnException;

public interface ISentenceJvnCustom {
    void write(String text);
    String read();
    void writeSlow(String text, int seconds) throws JvnException;
    String readSlow(int seconds) throws JvnException;
    void simulateLongWriteOperation(String text, long durationMs) throws JvnException;
    String simulateLongReadOperation(long durationMs) throws JvnException;
}