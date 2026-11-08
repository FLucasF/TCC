package com.pousadamarealta.domain;

public class Message {
  public final long id;
  public final MessageChannel channel;
  public final String to;
  public final String subject;
  public final String body;

  public Message(long id, MessageChannel channel, String to, String subject, String body) {
    this.id = id;
    this.channel = channel;
    this.to = to;
    this.subject = subject;
    this.body = body;
  }
}
