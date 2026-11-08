package com.pousada.model;

public class Message {
  public enum Channel {
    EMAIL
  }

  private long id;
  private Channel channel;
  private String to;
  private String subject;
  private String body;

  public Message(Channel channel, String to, String subject, String body) {
    this.channel = channel;
    this.to = to;
    this.subject = subject;
    this.body = body;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Channel getChannel() {
    return channel;
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }
}
