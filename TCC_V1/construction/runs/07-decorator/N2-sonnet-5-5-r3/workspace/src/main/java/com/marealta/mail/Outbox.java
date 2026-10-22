package com.marealta.mail;

/** Sends a message and returns it as it was recorded (with its id and final status). */
public interface Outbox {
    Message send(Message message);
}
