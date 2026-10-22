package br.com.marealta.messaging;

/** Where a message goes. The real one stores it; the rules wrap it and change what reaches it. */
public interface MessageSink {
    void deliver(Message message);
}
