package br.com.marealta.web;

import br.com.marealta.dominio.Quarto;
import br.com.marealta.dominio.Reserva;
import br.com.marealta.servico.Comprovante;
import java.math.BigDecimal;

/** Formato JSON combinado com o site. */
final class Respostas {

    private Respostas() {
    }

    record QuartoJson(int number, String type, int capacity, BigDecimal nightlyRate) {
    }

    record ReservaJson(
            int id,
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            String checkIn,
            String checkOut,
            int guests,
            String ratePlan,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String status,
            BigDecimal amountPaid,
            BigDecimal refund) {
    }

    record ErroJson(String error, String message) {
    }

    static QuartoJson de(Quarto q) {
        return new QuartoJson(q.numero(), q.tipo().name(), q.capacidade(), q.diaria());
    }

    static ReservaJson de(Reserva r) {
        return new ReservaJson(
                r.id(),
                r.quarto().numero(),
                r.hospede().nome(),
                r.hospede().email(),
                r.hospede().cpf(),
                r.entrada().toString(),
                r.saida().toString(),
                r.hospedes(),
                r.plano().codigo(),
                r.noites(),
                r.diarias(),
                r.desconto(),
                r.total(),
                r.status().name(),
                r.valorPago(),
                r.devolvido());
    }

    record ComprovanteJson(
            String title,
            int reservationId,
            String guestName,
            String guestCpf,
            int roomNumber,
            String roomType,
            String checkIn,
            String checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
    }

    static ComprovanteJson de(Comprovante c) {
        return new ComprovanteJson(
                c.title(),
                c.reservationId(),
                c.guestName(),
                c.guestCpf(),
                c.roomNumber(),
                c.roomType(),
                c.checkIn().toString(),
                c.checkOut().toString(),
                c.nights(),
                c.subtotal(),
                c.discount(),
                c.total());
    }
}
