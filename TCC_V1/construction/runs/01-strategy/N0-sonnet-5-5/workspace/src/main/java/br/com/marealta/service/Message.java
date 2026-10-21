package br.com.marealta.service;

public record Message(int id, String channel, String to, String subject, String body) {}
