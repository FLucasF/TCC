package br.com.mealta.domain;

import br.com.mealta.store.MessageLog;
import br.com.mealta.store.ReservationRepository;
import br.com.mealta.store.Transactor;

import java.util.function.Consumer;

public class ReservationService {
    private final ReservationRepository reservations;
    private final MessageLog messages;
    private final Transactor tx;
    private final RoomCatalog rooms;
    private final Pricing pricing;

    public ReservationService(ReservationRepository reservations, MessageLog messages, Transactor tx,
                              RoomCatalog rooms, Pricing pricing) {
        this.reservations = reservations;
        this.messages = messages;
        this.tx = tx;
        this.rooms = rooms;
        this.pricing = pricing;
    }

    public Reservation create(CreateReservation cmd) {
        if (!cmd.checkOut().isAfter(cmd.checkIn())) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (cmd.guests() < 1) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
        Room room = rooms.require(cmd.roomNumber());
        if (cmd.guests() > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " hóspedes");
        }
        Quote quote = pricing.quote(room, cmd.checkIn(), cmd.checkOut(), cmd.ratePlan());
        return tx.run(() -> {
            reservations.lockRoom(room.number());
            if (reservations.hasActiveOverlap(room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma dessas noites");
            }
            return reservations.save(Reservation.pending(room.number(), cmd.guestName(), cmd.guestEmail(),
                    cmd.guestCpf(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan(), quote));
        });
    }

    public Reservation get(long id) {
        return reservations.findById(id).orElseThrow(() -> notFound(id));
    }

    public Reservation confirm(long id) {
        return change(id, reservation -> {
            reservation.confirm();
            messages.add(ReservationMessages.confirmation(reservation, receiptOf(reservation)));
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::checkIn);
    }

    public Receipt checkOut(long id) {
        return receiptOf(change(id, Reservation::checkOut));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só há comprovante para reserva paga");
        }
        return receiptOf(reservation);
    }

    private Reservation change(long id, Consumer<Reservation> operation) {
        return tx.run(() -> {
            Reservation reservation = reservations.lockById(id).orElseThrow(() -> notFound(id));
            operation.accept(reservation);
            return reservations.save(reservation);
        });
    }

    private Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, rooms.require(reservation.roomNumber()));
    }

    private static PousadaException notFound(long id) {
        return new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
