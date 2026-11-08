package pousada.service;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Message;
import pousada.domain.Money;
import pousada.domain.Receipt;
import pousada.domain.ReceiptText;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.plan.Pricing;
import pousada.plan.RatePlan;
import pousada.plan.RatePlans;
import pousada.plan.Stay;

import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public Reservation create(NewReservation request) {
        Room room = RoomCatalog.find(request.roomNumber())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente"));
        RatePlan plan = RatePlans.byCode(request.ratePlan());
        Stay stay = new Stay(room, request.checkIn(), request.checkOut(), request.companyCode());
        Pricing pricing = plan.quote(stay);
        if (request.guests() > room.type().capacity()) {
            throw new AppException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta até " + room.type().capacity() + " pessoas");
        }
        Reservation draft = new Reservation(0, room, request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), plan, stay.nights(),
                pricing.subtotal(), pricing.discount(), pricing.total(), ReservationStatus.PENDING,
                Money.ZERO, null);
        return repository.insert(draft);
    }

    public Reservation get(long id) {
        return repository.find(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente"));
    }

    public Reservation confirm(long id) {
        return transition(id, Reservation::confirm, r -> {
            Receipt receipt = r.receipt();
            return List.of(Message.email(r.guestEmail(), ReceiptText.CONFIRMATION_SUBJECT,
                    ReceiptText.confirmationEmail(receipt)));
        });
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::checkIn);
    }

    public Receipt checkOut(long id) {
        return transition(id, Reservation::checkOut).receipt();
    }

    public Receipt document(long id) {
        return get(id).receipt();
    }

    public List<Message> messages() {
        return repository.messages();
    }

    private Reservation transition(long id, Consumer<Reservation> action) {
        return transition(id, action, r -> List.of());
    }

    private Reservation transition(long id, Consumer<Reservation> action,
                                   java.util.function.Function<Reservation, List<Message>> outgoing) {
        Reservation reservation = get(id);
        ReservationStatus before = reservation.status();
        action.accept(reservation);
        repository.update(reservation, before, outgoing.apply(reservation));
        return reservation;
    }
}
