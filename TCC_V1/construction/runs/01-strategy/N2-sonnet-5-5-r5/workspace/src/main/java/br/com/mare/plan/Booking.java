package br.com.mare.plan;

import br.com.mare.domain.Room;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano de tarifa precisa saber do pedido para validar e calcular o preço. */
public record Booking(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() { return (int) ChronoUnit.DAYS.between(checkIn, checkOut); }
}
