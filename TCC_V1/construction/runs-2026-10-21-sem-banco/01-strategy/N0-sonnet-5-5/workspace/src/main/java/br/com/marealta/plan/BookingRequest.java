package br.com.marealta.plan;

import br.com.marealta.domain.Room;
import java.time.LocalDate;

/** Dados da reserva que um plano precisa para validar e calcular o preço. */
public record BookingRequest(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
