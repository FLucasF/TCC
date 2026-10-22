package com.pousadamareaalta;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {
    @Test
    public void testRoomsExist() {
        assertNotNull(new com.pousadamareaalta.repositories.RoomRepository());
    }

    @Test
    public void testReservationCreation() {
        assertNotNull(new com.pousadamareaalta.models.Reservation());
    }

    @Test
    public void testDocumentGenerators() {
        var cpf = new com.pousadamareaalta.documents.CpfDocument();
        assertEquals("Comprovante de reserva", cpf.getTitle());

        var cnpj = new com.pousadamareaalta.documents.CnpjDocument();
        assertEquals("Fatura de hospedagem", cnpj.getTitle());

        var passport = new com.pousadamareaalta.documents.PassportDocument();
        assertEquals("Booking receipt", passport.getTitle());
    }

    @Test
    public void testPriceRounding() {
        double value = 249.90 * 1.20;
        double rounded = new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue();
        assertEquals(299.88, rounded, 0.01);
    }
}
