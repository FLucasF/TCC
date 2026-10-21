package com.pousada.domain;

public record PassportIdentification(String passport, String country) implements GuestIdentification {
}
