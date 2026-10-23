package br.com.pousada.marealta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuestIdentificationTest {

    @Test
    void shouldCreateIndividualIdentification() {
        GuestIdentification.Individual individual = new GuestIdentification.Individual("12345678901");
        assertEquals("12345678901", individual.cpf());
    }

    @Test
    void shouldCreateCompanyIdentification() {
        GuestIdentification.Company company = new GuestIdentification.Company("12345678000190", "Empresa LTDA");
        assertEquals("12345678000190", company.cnpj());
        assertEquals("Empresa LTDA", company.companyName());
    }

    @Test
    void shouldCreateForeignIdentification() {
        GuestIdentification.Foreign foreign = new GuestIdentification.Foreign("X1234567", "United Kingdom");
        assertEquals("X1234567", foreign.passport());
        assertEquals("United Kingdom", foreign.country());
    }
}
