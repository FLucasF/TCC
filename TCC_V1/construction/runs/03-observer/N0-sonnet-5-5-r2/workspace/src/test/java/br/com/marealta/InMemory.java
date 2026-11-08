package br.com.marealta;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.events.FinanceNotifier;
import br.com.marealta.events.GuestNotifier;
import br.com.marealta.events.HousekeepingNotifier;
import br.com.marealta.events.MessageSender;
import br.com.marealta.events.ReservationEvents;
import br.com.marealta.events.ReservationListener;
import br.com.marealta.events.StoredMessageSender;
import br.com.marealta.service.MessageStore;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationRepository;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.RoomRepository;
import br.com.marealta.service.Transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

/** Implementações em memória das portas, para testar sem banco. */
final class InMemory {
    final List<Room> roomList = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    final TreeMap<Long, Reservation> stored = new TreeMap<>();
    final List<Message> sent = new ArrayList<>();

    final Transactions tx = new Transactions() {
        @Override
        public <T> T run(Supplier<T> work) {
            return work.get();
        }
    };
    final RoomRepository rooms = new RoomRepository() {
        public List<Room> findAll() {
            return roomList;
        }

        public Optional<Room> find(int number) {
            return roomList.stream().filter(r -> r.number() == number).findFirst();
        }

        public Optional<Room> findForUpdate(int number) {
            return find(number);
        }
    };
    final ReservationRepository reservations = new ReservationRepository() {
        public Reservation insert(Reservation r) {
            Reservation saved = r.withId(stored.size() + 1L);
            stored.put(saved.id(), saved);
            return saved;
        }

        public void update(Reservation r) {
            stored.put(r.id(), r);
        }

        public Optional<Reservation> find(long id) {
            return Optional.ofNullable(stored.get(id));
        }

        public Optional<Reservation> findForUpdate(long id) {
            return find(id);
        }

        public boolean existsOverlapping(int room, LocalDate in, LocalDate out) {
            return stored.values().stream().anyMatch(r -> r.roomNumber() == room
                    && r.status() != ReservationStatus.CANCELLED
                    && r.checkIn().isBefore(out) && r.checkOut().isAfter(in));
        }

        public List<Reservation> findPaidOccupying(LocalDate night) {
            return stored.values().stream()
                    .filter(r -> r.status().isPaid() && !r.checkIn().isAfter(night) && r.checkOut().isAfter(night))
                    .sorted(Comparator.comparingInt(Reservation::roomNumber)).toList();
        }

        public List<Reservation> findCheckingInBetween(LocalDate from, LocalDate to) {
            return stored.values().stream()
                    .filter(r -> !r.checkIn().isBefore(from) && r.checkIn().isBefore(to))
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id)).toList();
        }
    };
    final MessageStore messages = new MessageStore() {
        public Message save(OutgoingMessage m) {
            Message saved = new Message(sent.size() + 1L, m.channel(), m.to(), m.subject(), m.body());
            sent.add(saved);
            return saved;
        }

        public List<Message> findAll() {
            return List.copyOf(sent);
        }
    };

    final MessageSender sender = new StoredMessageSender(messages);
    final ReservationEvents events = new ReservationEvents(List.<ReservationListener>of(
            new GuestNotifier(sender), new HousekeepingNotifier(sender), new FinanceNotifier(sender)));
    final ReservationService service = new ReservationService(tx, rooms, reservations, messages, events);
    final ReportService reports = new ReportService(tx, rooms, reservations);
}
