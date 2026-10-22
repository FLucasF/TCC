package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.DomainException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.NewReservation;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Rooms;
import br.com.marealta.pousada.messaging.Message;
import br.com.marealta.pousada.messaging.MessagePipeline;
import br.com.marealta.pousada.messaging.ReservationEmails;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.Tx;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ReservationService {
    private final Database database;
    private final MessagePipeline pipeline;

    public ReservationService(Database database, MessagePipeline pipeline) {
        this.database = database;
        this.pipeline = pipeline;
    }

    public Reservation create(NewReservation request) {
        return database.transaction(tx -> {
            var room = Rooms.find(request.roomNumber()).orElseThrow(() ->
                    new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + request.roomNumber() + " não existe"));
            Reservation reservation = Reservation.open(request, room);
            tx.reservations().lockRoom(room.number());
            if (tx.reservations().isRoomTaken(room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE, "O quarto já está reservado em alguma das noites");
            }
            return tx.reservations().insert(reservation);
        });
    }

    public Reservation get(long id) {
        return database.transaction(tx -> tx.reservations().find(id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (tx, reservation) ->
                tx.messages().insertAll(pipeline.process(ReservationEmails.confirmation(reservation))));
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, (tx, reservation) -> { });
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::startStay, (tx, reservation) -> { });
    }

    public Receipt checkOut(long id) {
        return Receipt.of(change(id, Reservation::endStay, (tx, reservation) -> { }));
    }

    public Receipt document(long id) {
        return get(id).receipt();
    }

    public List<Message> messages() {
        return database.transaction(tx -> tx.messages().findAll());
    }

    /** Sequência comum às operações: trava, aplica a mudança, grava e executa o que decorre dela. */
    private Reservation change(long id, Consumer<Reservation> operation, BiConsumer<Tx, Reservation> afterwards) {
        return database.transaction(tx -> {
            Reservation reservation = tx.reservations().findForUpdate(id).orElseThrow(() -> notFound(id));
            operation.accept(reservation);
            tx.reservations().update(reservation);
            afterwards.accept(tx, reservation);
            return reservation;
        });
    }

    private static DomainException notFound(long id) {
        return new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
