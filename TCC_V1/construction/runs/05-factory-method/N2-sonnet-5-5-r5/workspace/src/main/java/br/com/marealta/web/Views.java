package br.com.marealta.web;

import br.com.marealta.domain.*;

import java.math.BigDecimal;

/** Formatos JSON combinados com o site. */
final class Views {
    private Views() {
    }

    record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        static RoomView of(Room r) {
            return new RoomView(r.number(), r.type(), r.capacity(), r.nightlyRate());
        }
    }

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail,
                           String guestCpf, String companyCnpj, String companyName,
                           String guestPassport, String guestCountry,
                           String checkIn, String checkOut, int guests, RatePlan ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                           ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            IdentityFields f = r.identity().fields();
            return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(),
                    f.guestCpf(), f.companyCnpj(), f.companyName(), f.guestPassport(), f.guestCountry(),
                    r.checkIn().toString(), r.checkOut().toString(), r.guests(), r.ratePlan(), r.nights(),
                    r.subtotal(), r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
        }
    }

    record DocumentView(String title, long reservationId, String guestName,
                        String guestCpf, String companyCnpj, String companyName,
                        String guestPassport, String guestCountry,
                        int roomNumber, RoomType roomType, String checkIn, String checkOut, int nights,
                        BigDecimal subtotal, BigDecimal discount, BigDecimal total, String text) {
        static DocumentView of(ReservationDocument d) {
            Reservation r = d.reservation();
            IdentityFields f = r.identity().fields();
            return new DocumentView(d.title(), r.id(), r.guestName(),
                    f.guestCpf(), f.companyCnpj(), f.companyName(), f.guestPassport(), f.guestCountry(),
                    r.room().number(), r.room().type(), r.checkIn().toString(), r.checkOut().toString(),
                    r.nights(), r.subtotal(), r.discount(), r.total(), d.text());
        }
    }

    record MessageView(long id, Message.Channel channel, String to, String subject, String body) {
        static MessageView of(Message m) {
            return new MessageView(m.id(), m.channel(), m.to(), m.subject(), m.body());
        }
    }

    record ErrorView(String error, String message) {
    }
}
