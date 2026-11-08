package br.com.mare.pousada.service;

import br.com.mare.pousada.domain.BusinessException;
import br.com.mare.pousada.domain.ErrorCode;
import br.com.mare.pousada.domain.Message;
import br.com.mare.pousada.domain.Money;
import br.com.mare.pousada.domain.Quote;
import br.com.mare.pousada.domain.Receipt;
import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.domain.ReservationStatus;
import br.com.mare.pousada.domain.Room;
import br.com.mare.pousada.domain.RoomCatalog;
import br.com.mare.pousada.infra.Db;
import br.com.mare.pousada.infra.MessageRepository;
import br.com.mare.pousada.infra.ReservationRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    private final Db db;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Db db, ReservationRepository reservations, MessageRepository messages) {
        this.db = db;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Reservation create(NewReservation request) {
        Room room = RoomCatalog.require(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation pending = new Reservation(null, room.number(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
        return db.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.hasActiveReservation(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites");
            }
            return reservations.insert(c, pending);
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(c -> reservations.find(c, id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (c, r) -> {
            messages.save(c, Message.reservationConfirmed(r, Receipt.of(r)));
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, (c, r) -> { });
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, (c, r) -> { });
    }

    public Receipt checkOut(long id) {
        return Receipt.of(change(id, Reservation::registerCheckOut, (c, r) -> { }));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.getStatus().isPaid()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva " + id + " não tem pagamento, então não tem comprovante");
        }
        return Receipt.of(reservation);
    }

    public List<Message> sentMessages() {
        return db.inTransaction(messages::findAll);
    }

    @FunctionalInterface
    private interface SideEffect {
        void run(Connection connection, Reservation reservation) throws SQLException;
    }

    /** A sequência é a mesma para toda mudança de situação: trava, aplica, grava, efeitos, devolve. */
    private Reservation change(long id, Consumer<Reservation> transition, SideEffect sideEffect) {
        return db.inTransaction(c -> {
            Reservation reservation = reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id));
            transition.accept(reservation);
            reservations.saveState(c, reservation);
            sideEffect.run(c, reservation);
            return reservation;
        });
    }

    private static BusinessException notFound(long id) {
        return new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
