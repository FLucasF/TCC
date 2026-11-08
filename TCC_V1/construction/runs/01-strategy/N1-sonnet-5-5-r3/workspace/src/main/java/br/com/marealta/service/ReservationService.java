package br.com.marealta.service;

import java.util.List;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.DomainException.Code;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;

public class ReservationService {
    private final Database database;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database database) {
        this.database = database;
    }

    public Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw DomainException.invalid("A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw DomainException.invalid("A reserva precisa de pelo menos um hóspede");
        }
        RatePlan plan = RatePlans.find(request.ratePlan());
        if (plan == null) {
            throw DomainException.invalid("Plano de tarifa desconhecido: " + request.ratePlan());
        }
        Room room = room(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new DomainException(Code.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Stay stay = new Stay(room, request.checkIn(), request.checkOut(), request.companyCode());
        plan.validate(stay);
        Quote quote = plan.quote(stay);

        return database.tx(connection -> {
            reservations.lockRoom(connection, room.number());
            if (reservations.hasActiveReservation(connection, room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(Code.ROOM_UNAVAILABLE, "O quarto já está reservado em alguma das noites");
            }
            String companyCode = "CORPORATE".equals(plan.code()) ? request.companyCode() : null;
            Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                    request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), plan.code(),
                    companyCode, stay.nights(), quote.subtotal(), quote.discount(), ReservationStatus.PENDING,
                    Money.ZERO, null);
            return reservations.insert(connection, draft);
        });
    }

    public Reservation get(long id) {
        return database.tx(connection -> load(connection, id, false));
    }

    public Reservation confirm(long id) {
        return database.tx(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.confirm();
            reservations.update(connection, reservation);
            messages.insert(connection, Message.reservationConfirmed(reservation, receipt(reservation)));
            return reservation;
        });
    }

    public Reservation cancel(long id) {
        return database.tx(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.cancel(RatePlans.find(reservation.ratePlan())::refund);
            reservations.update(connection, reservation);
            return reservation;
        });
    }

    public Reservation checkIn(long id) {
        return database.tx(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.registerCheckIn();
            reservations.update(connection, reservation);
            return reservation;
        });
    }

    public Receipt checkOut(long id) {
        return database.tx(connection -> {
            Reservation reservation = load(connection, id, true);
            reservation.registerCheckOut();
            reservations.update(connection, reservation);
            return receipt(reservation);
        });
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw new DomainException(Code.OPERATION_NOT_ALLOWED,
                    "Só há comprovante para reserva paga (status " + reservation.status() + ")");
        }
        return receipt(reservation);
    }

    public List<Message> messages() {
        return database.tx(messages::findAll);
    }

    private Reservation load(java.sql.Connection connection, long id, boolean forUpdate) throws java.sql.SQLException {
        return reservations.find(connection, id, forUpdate)
                .orElseThrow(() -> new DomainException(Code.RESERVATION_NOT_FOUND, "Reserva não encontrada: " + id));
    }

    private static Room room(int number) {
        return Rooms.find(number)
                .orElseThrow(() -> new DomainException(Code.ROOM_NOT_FOUND, "Quarto não encontrado: " + number));
    }

    private static Receipt receipt(Reservation reservation) {
        return Receipt.of(reservation, room(reservation.roomNumber()));
    }
}
