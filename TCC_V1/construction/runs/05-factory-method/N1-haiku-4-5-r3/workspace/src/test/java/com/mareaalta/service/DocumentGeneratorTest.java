package com.mareaalta.service;

import com.mareaalta.domain.Reservation;
import com.mareaalta.domain.Room;
import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DocumentGeneratorTest {
    @Test
    public void testComprovanteFormat() {
        Reservation res = new Reservation();
        res.setId(7);
        res.setGuestName("Carla Dias");
        res.setGuestCpf("111.222.333-44");
        res.setRoomNumber(201);
        res.setCheckIn(LocalDate.of(2027, 3, 10));
        res.setCheckOut(LocalDate.of(2027, 3, 12));
        res.setNights(2);
        res.setSubtotal(new BigDecimal("779.80"));
        res.setDiscount(null);
        res.setTotal(new BigDecimal("779.80"));

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String document = DocumentGenerator.generateComprovante(res, room);

        assertTrue(document.contains("Comprovante de reserva nº 7"));
        assertTrue(document.contains("Carla Dias (CPF 111.222.333-44)"));
        assertTrue(document.contains("Quarto: 201 (DELUXE)"));
        assertTrue(document.contains("R$ 779,80"));
    }

    @Test
    public void testFaturaFormat() {
        Reservation res = new Reservation();
        res.setId(3);
        res.setGuestName("Ana Souza");
        res.setCompanyName("Tecnosul Ltda");
        res.setCompanyCnpj("12.345.678/0001-90");
        res.setRoomNumber(201);
        res.setCheckIn(LocalDate.of(2027, 3, 10));
        res.setCheckOut(LocalDate.of(2027, 3, 12));
        res.setNights(2);
        res.setSubtotal(new BigDecimal("779.80"));
        res.setDiscount(null);
        res.setTotal(new BigDecimal("779.80"));

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String document = DocumentGenerator.generateFatura(res, room);

        assertTrue(document.contains("Fatura de hospedagem nº 3"));
        assertTrue(document.contains("Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(document.contains("Hóspede: Ana Souza"));
        assertTrue(document.contains("R$ 779,80"));
        assertTrue(document.contains("ISS retido (5%)"));
    }

    @Test
    public void testBookingReceiptFormat() {
        Reservation res = new Reservation();
        res.setId(4);
        res.setGuestName("John Smith");
        res.setGuestPassport("X1234567");
        res.setGuestCountry("United Kingdom");
        res.setRoomNumber(201);
        res.setCheckIn(LocalDate.of(2027, 3, 10));
        res.setCheckOut(LocalDate.of(2027, 3, 12));
        res.setNights(2);
        res.setSubtotal(new BigDecimal("779.80"));
        res.setDiscount(null);
        res.setTotal(new BigDecimal("779.80"));

        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));

        String document = DocumentGenerator.generateBookingReceipt(res, room);

        assertTrue(document.contains("Booking receipt no. 4"));
        assertTrue(document.contains("John Smith (passport X1234567, United Kingdom)"));
        assertTrue(document.contains("Room: 201 (DELUXE)"));
        assertTrue(document.contains("BRL 779.80"));
    }
}
