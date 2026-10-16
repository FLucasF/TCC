"""Scenario 06 (negative control): the contained solution (a field) and the over-engineered one (a builder for the
reservation request and a child-pricing policy that charges nothing)."""

from kit import CALCULATOR, HTTP_API, RECEIPT, REPORTS_TEST, SERVICE, SERVICE_TEST, Solution

NEW_RESERVATION = "src/main/java/hotel/reservations/NewReservation.java"
RESERVATION = "src/main/java/hotel/reservations/Reservation.java"
REQUEST = "src/main/java/hotel/web/CreateReservationRequest.java"
RESPONSE = "src/main/java/hotel/web/ReservationResponse.java"


def shared_changes(solution: Solution) -> None:
    """Everything but how the request is built: the field everywhere, the capacity, the receipt line."""
    solution.replace(RESERVATION, """    private final int guests;
""", """    private final int guests;
    private final int children;
""")
    solution.replace(RESERVATION, """        this.guests = request.guests();
""", """        this.guests = request.guests();
        this.children = request.children();
""")
    solution.replace(RESERVATION, """    public RatePlan ratePlan() {""", """    /** Children up to 12 years old: they do not pay, but take a place in the room. */
    public int children() {
        return children;
    }

    public RatePlan ratePlan() {""")
    solution.replace(SERVICE, """        if (request.guests() < 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "a reservation needs at least one guest");
        }""", """        if (request.guests() < 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "a reservation needs at least one guest");
        }
        if (request.children() < 0) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the number of children cannot be negative");
        }""")
    solution.replace(SERVICE, """        if (request.guests() > room.capacity()) {""",
                     """        if (request.guests() + request.children() > room.capacity()) {""")
    solution.replace(SERVICE, """"room " + room.number() + " takes at most " + room.capacity() + " guests");""",
                     """"room " + room.number() + " takes at most " + room.capacity() + " people, children included");""")
    solution.replace(RECEIPT, """        RoomType roomType,
""", """        RoomType roomType,
        int children,
""")
    solution.replace(RECEIPT, """                room.type(),
""", """                room.type(),
                reservation.children(),
""")
    solution.replace(RECEIPT, """        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\\n");
""", """        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\\n");
        if (children > 0) {
            text.append("Crianças: ").append(children).append('\\n');
        }
""")
    solution.replace(REQUEST, """        Integer guests,
""", """        Integer guests,
        Integer children,
""")
    solution.replace(REQUEST, """/** The JSON body of POST /reservations. Every field is required. */""",
                     """/** The JSON body of POST /reservations. Every field is required, but children (zero when missing). */""")
    solution.replace(RESPONSE, """        int guests,
""", """        int guests,
        int children,
""")
    solution.replace(RESPONSE, """                reservation.guests(),
""", """                reservation.guests(),
                reservation.children(),
""")


contained = Solution("06-contained")
shared_changes(contained)
contained.replace(NEW_RESERVATION, """        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {""",
                  """        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
        int children) {""")
contained.replace(HTTP_API, """                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());""",
                  """                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan(),
                body.children() == null ? 0 : body.children());""")
contained.replace(SERVICE_TEST, """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);""",
                  """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan, 0);""")
contained.replace(REPORTS_TEST, """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan));""",
                  """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, 0));""")
contained.replace(SERVICE_TEST, """    @Test
    void aRoomCannotBeBookedTwiceForTheSameNight() {""", """    @Test
    void childrenDoNotPayButTakeAPlace() {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        Reservation family = service.create(new NewReservation(
                301, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 2, RatePlan.STANDARD, 2));

        assertEquals(new BigDecimal("579.90"), family.price().total());
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, errorOf(() -> service.create(new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 2, RatePlan.STANDARD, 1))));
    }

    @Test
    void aRoomCannotBeBookedTwiceForTheSameNight() {""")
contained.build_and_save()


over = Solution("06-overengineered")
shared_changes(over)
over.replace(NEW_RESERVATION, """public record NewReservation(
        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}""", """public record NewReservation(
        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
        int children) {

    public NewReservation(
            int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this(roomNumber, guest, checkIn, checkOut, guests, ratePlan, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builds a reservation request step by step; more about the party (ages, pets...) can come here later. */
    public static final class Builder {

        private int roomNumber;
        private Guest guest;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private int guests;
        private int children;
        private RatePlan ratePlan;

        public Builder roomNumber(int value) {
            this.roomNumber = value;
            return this;
        }

        public Builder guest(Guest value) {
            this.guest = value;
            return this;
        }

        public Builder stay(LocalDate from, LocalDate to) {
            this.checkIn = from;
            this.checkOut = to;
            return this;
        }

        public Builder adults(int value) {
            this.guests = value;
            return this;
        }

        public Builder children(int value) {
            this.children = value;
            return this;
        }

        public Builder ratePlan(RatePlan value) {
            this.ratePlan = value;
            return this;
        }

        public NewReservation build() {
            return new NewReservation(roomNumber, guest, checkIn, checkOut, guests, ratePlan, children);
        }
    }
}""")
over.write("src/main/java/hotel/reservations/ChildPricingPolicy.java", """package hotel.reservations;

import java.math.BigDecimal;

/** What a child costs per night. Today children are free; older children may pay something one day. */
public interface ChildPricingPolicy {

    BigDecimal pricePerChildPerNight();
}
""")
over.write("src/main/java/hotel/reservations/FreeChildren.java", """package hotel.reservations;

import java.math.BigDecimal;

/** Children stay for free. */
public final class FreeChildren implements ChildPricingPolicy {

    @Override
    public BigDecimal pricePerChildPerNight() {
        return BigDecimal.ZERO;
    }
}
""")
over.replace(SERVICE, """        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());""",
             """        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan(),
                request.children());""")
over.replace(CALCULATOR, """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }""", """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private final ChildPricingPolicy childPricing = new FreeChildren();

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return calculate(room, checkIn, checkOut, ratePlan, 0);
    }

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, int children) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        BigDecimal childrenPerNight = childPricing.pricePerChildPerNight().multiply(BigDecimal.valueOf(children));
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night)).add(cents(childrenPerNight));
            nights++;
        }""")
over.replace(HTTP_API, """        return new NewReservation(
                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());""",
             """        return NewReservation.builder()
                .roomNumber(body.roomNumber())
                .guest(guest)
                .stay(body.checkIn(), body.checkOut())
                .adults(body.guests())
                .children(body.children() == null ? 0 : body.children())
                .ratePlan(body.ratePlan())
                .build();""")
over.build_and_save()
