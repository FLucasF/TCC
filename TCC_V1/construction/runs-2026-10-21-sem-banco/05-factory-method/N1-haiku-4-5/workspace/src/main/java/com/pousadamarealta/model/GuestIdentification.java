package com.pousadamarealta.model;

public sealed class GuestIdentification {
    public static final class CPF extends GuestIdentification {
        public final String value;

        public CPF(String value) {
            this.value = value;
        }
    }

    public static final class CNPJ extends GuestIdentification {
        public final String value;
        public final String companyName;

        public CNPJ(String value, String companyName) {
            this.value = value;
            this.companyName = companyName;
        }
    }

    public static final class Passport extends GuestIdentification {
        public final String number;
        public final String country;

        public Passport(String number, String country) {
            this.number = number;
            this.country = country;
        }
    }
}
