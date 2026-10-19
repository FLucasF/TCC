package br.com.mareaalta.domain;

public interface DocumentGenerator {
    DocumentData generateDocument(Reservation reservation);
    void validate(Reservation reservation);
}
