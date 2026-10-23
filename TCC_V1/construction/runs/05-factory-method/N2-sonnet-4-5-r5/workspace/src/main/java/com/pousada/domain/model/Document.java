package com.pousada.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Document {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final String companyCnpj;
    private final String companyName;
    private final String guestPassport;
    private final String guestCountry;
    private final String roomNumber;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String text;

    private Document(Builder builder) {
        this.title = builder.title;
        this.reservationId = builder.reservationId;
        this.guestName = builder.guestName;
        this.guestCpf = builder.guestCpf;
        this.companyCnpj = builder.companyCnpj;
        this.companyName = builder.companyName;
        this.guestPassport = builder.guestPassport;
        this.guestCountry = builder.guestCountry;
        this.roomNumber = builder.roomNumber;
        this.roomType = builder.roomType;
        this.checkIn = builder.checkIn;
        this.checkOut = builder.checkOut;
        this.nights = builder.nights;
        this.subtotal = builder.subtotal;
        this.discount = builder.discount;
        this.total = builder.total;
        this.text = builder.text;
    }

    public String title() { return title; }
    public long reservationId() { return reservationId; }
    public String guestName() { return guestName; }
    public String guestCpf() { return guestCpf; }
    public String companyCnpj() { return companyCnpj; }
    public String companyName() { return companyName; }
    public String guestPassport() { return guestPassport; }
    public String guestCountry() { return guestCountry; }
    public String roomNumber() { return roomNumber; }
    public String roomType() { return roomType; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public String text() { return text; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private long reservationId;
        private String guestName;
        private String guestCpf;
        private String companyCnpj;
        private String companyName;
        private String guestPassport;
        private String guestCountry;
        private String roomNumber;
        private String roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private int nights;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal total;
        private String text;

        public Builder title(String title) { this.title = title; return this; }
        public Builder reservationId(long id) { this.reservationId = id; return this; }
        public Builder guestName(String name) { this.guestName = name; return this; }
        public Builder guestCpf(String cpf) { this.guestCpf = cpf; return this; }
        public Builder companyCnpj(String cnpj) { this.companyCnpj = cnpj; return this; }
        public Builder companyName(String name) { this.companyName = name; return this; }
        public Builder guestPassport(String passport) { this.guestPassport = passport; return this; }
        public Builder guestCountry(String country) { this.guestCountry = country; return this; }
        public Builder roomNumber(String number) { this.roomNumber = number; return this; }
        public Builder roomType(String type) { this.roomType = type; return this; }
        public Builder checkIn(LocalDate date) { this.checkIn = date; return this; }
        public Builder checkOut(LocalDate date) { this.checkOut = date; return this; }
        public Builder nights(int nights) { this.nights = nights; return this; }
        public Builder subtotal(BigDecimal amount) { this.subtotal = amount; return this; }
        public Builder discount(BigDecimal amount) { this.discount = amount; return this; }
        public Builder total(BigDecimal amount) { this.total = amount; return this; }
        public Builder text(String text) { this.text = text; return this; }

        public Document build() {
            return new Document(this);
        }
    }
}
