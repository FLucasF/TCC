"""Scenario 05 (Factory Method): the pattern solution (one DocumentFactory picks the writer by kind of guest; the three
places ask it) and the naive one (the if/else on the kind of guest repeated in the three places)."""

from kit import HTTP_API, MIGRATIONS, RECEIPT, SERVICE, STORE, Solution

GUEST = "src/main/java/hotel/reservations/Guest.java"
REQUEST = "src/main/java/hotel/web/CreateReservationRequest.java"
RESPONSE = "src/main/java/hotel/web/ReservationResponse.java"

GUEST_RECORD = """package hotel.reservations;

/**
 * Who stays and how they are identified: a CPF, or a company (CNPJ and name), or a passport and country. Exactly
 * one of the three is filled; the other fields are null.
 */
public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    public Guest(String name, String email, String cpf) {
        this(name, email, cpf, null, null, null, null);
    }

    public boolean isCompany() {
        return companyCnpj != null;
    }

    public boolean isForeigner() {
        return passport != null;
    }
}
"""

RECEIPT_RECORD = """package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The document the guest gets when the reservation is confirmed, at check-out, and on request. */
public record ReservationReceipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String text) {

    public ReservationReceipt(String title, Reservation reservation, Room room, String text) {
        this(
                title,
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().passport(),
                reservation.guest().country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                text);
    }
}
"""

# The three texts, written once; the pattern solution puts each in its writer, the naive one in DocumentTexts.
STAY_LINES_PT = """        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\\n");
        int nights = reservation.price().nights();
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\\n');
"""
PERSON_TEXT = """        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\\n');
        text.append("Hóspede: ").append(reservation.guest().name())
                .append(" (CPF ").append(reservation.guest().cpf()).append(")\\n");
""" + STAY_LINES_PT + """        return text.toString();
"""
COMPANY_TEXT = """        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\\n');
        text.append("Empresa: ").append(reservation.guest().companyName())
                .append(" (CNPJ ").append(reservation.guest().companyCnpj()).append(")\\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\\n');
""" + STAY_LINES_PT + """        BigDecimal withheldTax = reservation.price().total().multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
        text.append("ISS retido (5%): ").append(TextFormat.money(withheldTax)).append('\\n');
        return text.toString();
"""
FOREIGN_TEXT = """        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\\n');
        text.append("Guest: ").append(reservation.guest().name()).append(" (passport ")
                .append(reservation.guest().passport()).append(", ").append(reservation.guest().country())
                .append(")\\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\\n");
        int nights = reservation.price().nights();
        text.append("Check-in: ").append(reservation.checkIn()).append(" | Check-out: ")
                .append(reservation.checkOut()).append(" | ").append(nights)
                .append(nights == 1 ? " night" : " nights").append('\\n');
        text.append("Nights: ").append(brl(reservation.price().subtotal())).append('\\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Discount: ").append(brl(reservation.price().discount())).append('\\n');
        }
        text.append("Total: ").append(brl(reservation.price().total())).append('\\n');
        return text.toString();
"""
BRL = """    private static String brl(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }
"""
RESERVATION_AND_ROOM = """import hotel.reservations.Reservation;
import hotel.rooms.Room;
"""
PERSON_IMPORTS = RESERVATION_AND_ROOM
COMPANY_IMPORTS = RESERVATION_AND_ROOM + """import java.math.BigDecimal;
import java.math.RoundingMode;
"""
FOREIGN_IMPORTS = RESERVATION_AND_ROOM + """import java.math.BigDecimal;
import java.util.Locale;
"""
ALL_TEXT_IMPORTS = COMPANY_IMPORTS + """import java.util.Locale;
"""


IDENTIFICATION_MIGRATION = """-- a guest is identified by a CPF, or by a company (CNPJ and name), or by a passport and country: exactly one
ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL,
    ADD COLUMN company_cnpj text,
    ADD COLUMN company_name text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text,
    ADD CONSTRAINT reservations_one_identification CHECK (
        (guest_cpf IS NOT NULL)::int
        + (company_cnpj IS NOT NULL AND company_name IS NOT NULL)::int
        + (guest_passport IS NOT NULL AND guest_country IS NOT NULL)::int = 1);
"""


def store_identification(solution: Solution) -> None:
    """The four new fields of the guest: a new migration, and the store writes and reads them."""
    solution.write(MIGRATIONS + "V2__guest_identification.sql", IDENTIFICATION_MIGRATION)
    solution.replace(STORE, """status, amount_paid, refund";""",
                     """status, amount_paid, refund, "
            + "company_cnpj, company_name, guest_passport, guest_country";""")
    solution.replace(STORE, """status, amount_paid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";""",
                     """status, amount_paid, company_cnpj, company_name, guest_passport, guest_country) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";""")
    solution.replace(STORE, """            statement.setBigDecimal(14, PriceCalculator.cents(BigDecimal.ZERO));
""", """            statement.setBigDecimal(14, PriceCalculator.cents(BigDecimal.ZERO));
            statement.setString(15, request.guest().companyCnpj());
            statement.setString(16, request.guest().companyName());
            statement.setString(17, request.guest().passport());
            statement.setString(18, request.guest().country());
""")
    solution.replace(STORE, """        Guest guest = new Guest(row.getString("guest_name"), row.getString("guest_email"), row.getString("guest_cpf"));""",
                     """        Guest guest = new Guest(row.getString("guest_name"), row.getString("guest_email"), row.getString("guest_cpf"),
                row.getString("company_cnpj"), row.getString("company_name"), row.getString("guest_passport"),
                row.getString("guest_country"));""")


def shared_changes(solution: Solution) -> None:
    store_identification(solution)
    solution.replace("src/test/java/hotel/web/HttpApiTest.java", """    @Test
    void servesTheReportsAsCsv() throws Exception {""", IDENTIFICATION_API_TEST)
    solution.write(GUEST, GUEST_RECORD)
    solution.write(RECEIPT, RECEIPT_RECORD)
    solution.replace(REQUEST, """/** The JSON body of POST /reservations. Every field is required. */""",
                     """/**
 * The JSON body of POST /reservations. Every field is required, and the guest is identified by exactly one of:
 * guestCpf; companyCnpj with companyName; guestPassport with guestCountry.
 */""")
    solution.replace(REQUEST, """        String guestCpf,
        LocalDate checkIn,""", """        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        LocalDate checkIn,""")
    solution.replace(RESPONSE, """        String guestCpf,
        LocalDate checkIn,""", """        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        LocalDate checkIn,""")
    solution.replace(RESPONSE, """                reservation.guest().cpf(),
                reservation.checkIn(),""", """                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().passport(),
                reservation.guest().country(),
                reservation.checkIn(),""")
    solution.replace(HTTP_API, """                || isBlank(body.guestCpf())
""", "")
    solution.replace(HTTP_API, """        Guest guest = new Guest(body.guestName(), body.guestEmail(), body.guestCpf());""",
                     """        int identifications = (isBlank(body.guestCpf()) ? 0 : 1)
                + (isBlank(body.companyCnpj()) && isBlank(body.companyName()) ? 0 : 1)
                + (isBlank(body.guestPassport()) && isBlank(body.guestCountry()) ? 0 : 1);
        boolean companyIncomplete = isBlank(body.companyCnpj()) != isBlank(body.companyName());
        boolean passportIncomplete = isBlank(body.guestPassport()) != isBlank(body.guestCountry());
        if (identifications != 1 || companyIncomplete || passportIncomplete) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the guest needs a CPF, or a company's CNPJ and name, or a passport and country (only one)");
        }
        Guest guest = new Guest(body.guestName(), body.guestEmail(), nullIfBlank(body.guestCpf()),
                nullIfBlank(body.companyCnpj()), nullIfBlank(body.companyName()),
                nullIfBlank(body.guestPassport()), nullIfBlank(body.guestCountry()));""")
    solution.replace(HTTP_API, """    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }""", """    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static String nullIfBlank(String text) {
        return isBlank(text) ? null : text;
    }""")


def documents_test() -> str:
    return """package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DocumentsByGuestTest {

    private static final Guest COMPANY = new Guest("Ana Souza", "ana@example.com", null,
            "12.345.678/0001-90", "Tecnosul Ltda", null, null);
    private static final Guest FOREIGNER = new Guest("John Smith", "john@example.com", null, null, null,
            "X1234567", "United Kingdom");
    private static final Guest PERSON = new Guest("Carla Dias", "carla@example.com", "111.222.333-44");

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service = new ReservationService(new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()), new PriceCalculator(), new EmailSender(sentMessages));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation bookAndPay(Guest guest, int room, String checkIn, String checkOut, RatePlan plan) {
        Reservation reservation = service.create(new NewReservation(
                room, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan));
        service.confirm(reservation.id());
        return reservation;
    }

    private Reservation bookAndPay(Guest guest) {
        return bookAndPay(guest, 201, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
    }

    @Test
    void aCompanyGetsAnInvoiceWithTheWithheldTax() {
        Reservation reservation = bookAndPay(COMPANY);

        ReservationReceipt invoice = service.receiptCopy(reservation.id());

        assertEquals("Fatura de hospedagem", invoice.title());
        assertEquals("12.345.678/0001-90", invoice.companyCnpj());
        assertEquals("Tecnosul Ltda", invoice.companyName());
        assertNull(invoice.guestCpf());
        assertNull(invoice.guestPassport());
        assertEquals(\"\"\"
                Fatura de hospedagem nº 1
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99
                \"\"\", invoice.text());
    }

    @Test
    void theInvoiceGoesInTheConfirmationEmailAndAtCheckOut() {
        Reservation reservation = bookAndPay(COMPANY);
        String email = sentMessages.all().get(0).body();
        service.checkIn(reservation.id());

        assertTrue(email.startsWith("Olá, Ana Souza!"));
        assertTrue(email.contains("Fatura de hospedagem nº 1"));
        assertTrue(email.contains("ISS retido (5%): R$ 38,99"));
        assertEquals("Fatura de hospedagem", service.checkOut(reservation.id()).title());
    }

    @Test
    void aForeignerGetsAnEnglishReceipt() {
        Reservation reservation = bookAndPay(FOREIGNER);

        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals("Booking receipt", receipt.title());
        assertEquals("X1234567", receipt.guestPassport());
        assertEquals("United Kingdom", receipt.guestCountry());
        assertNull(receipt.guestCpf());
        assertEquals(\"\"\"
                Booking receipt no. 1
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                \"\"\", receipt.text());
        assertTrue(sentMessages.all().get(0).body().contains("Booking receipt no. 1"));
    }

    @Test
    void theEnglishReceiptHasOneNightTheDiscountAndThousands() {
        Reservation oneNight = bookAndPay(FOREIGNER, 201, "2027-03-20", "2027-03-21", RatePlan.NON_REFUNDABLE);
        Reservation suite = bookAndPay(FOREIGNER, 301, "2027-01-05", "2027-01-09", RatePlan.STANDARD);

        String oneNightText = service.receiptCopy(oneNight.id()).text();
        assertTrue(oneNightText.contains("| 1 night\\n"));
        assertTrue(oneNightText.contains("Discount: BRL 58.48\\nTotal: BRL 331.42"));
        assertTrue(service.receiptCopy(suite.id()).text().contains("Total: BRL 2,783.52"));
    }

    @Test
    void aPersonKeepsTheReceipt() {
        Reservation reservation = bookAndPay(PERSON);

        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals("Comprovante de reserva", receipt.title());
        assertEquals("111.222.333-44", receipt.guestCpf());
        assertNull(receipt.companyCnpj());
        assertTrue(receipt.text().contains("Hóspede: Carla Dias (CPF 111.222.333-44)"));
    }
}
"""


IDENTIFICATION_API_TEST = """    @Test
    void theGuestIsIdentifiedInExactlyOneWay() throws Exception {
        String cpf = "\\"guestCpf\\": \\"111.222.333-44\\",";
        String company = "\\"companyCnpj\\": \\"12.345.678/0001-90\\", \\"companyName\\": \\"Tecnosul Ltda\\",";
        String passport = "\\"guestPassport\\": \\"X1234567\\", \\"guestCountry\\": \\"United Kingdom\\",";

        for (String wrong : new String[] {"", cpf + company, cpf + passport, "\\"companyCnpj\\": \\"1\\",",
                "\\"guestPassport\\": \\"X1\\","}) {
            HttpResponse<String> refused = post("/reservations", RESERVATION_JSON.replace(cpf, wrong));
            assertEquals(400, refused.statusCode());
            assertEquals("INVALID_REQUEST", json.readTree(refused.body()).get("error").asText());
        }

        HttpResponse<String> created = post("/reservations", RESERVATION_JSON.replace(cpf, passport));
        assertEquals(201, created.statusCode());
        JsonNode reservation = json.readTree(created.body());
        assertTrue(reservation.get("guestCpf").isNull());
        assertTrue(reservation.get("companyCnpj").isNull());
        assertEquals("X1234567", reservation.get("guestPassport").asText());
        assertEquals("United Kingdom", reservation.get("guestCountry").asText());
    }

    @Test
    void servesTheReportsAsCsv() throws Exception {"""


# ---------------------------------------------------------------- the pattern: one factory, three writers
pattern = Solution("05-pattern")
shared_changes(pattern)
DOCUMENTS = "src/main/java/hotel/documents/"
pattern.write(DOCUMENTS + "DocumentWriter.java", """package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** One kind of document: its title and its text. */
interface DocumentWriter {

    String title();

    String text(Reservation reservation, Room room);
}
""")
pattern.write(DOCUMENTS + "DocumentFactory.java", """package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Issues the right document for the guest: the only place that knows which kind of guest gets which document. */
public final class DocumentFactory {

    private final DocumentWriter personReceipt = new PersonReceiptWriter();
    private final DocumentWriter companyInvoice = new CompanyInvoiceWriter();
    private final DocumentWriter foreignReceipt = new ForeignReceiptWriter();

    public ReservationReceipt issue(Reservation reservation, Room room) {
        DocumentWriter writer = writerFor(reservation.guest());
        return new ReservationReceipt(writer.title(), reservation, room, writer.text(reservation, room));
    }

    private DocumentWriter writerFor(Guest guest) {
        if (guest.isCompany()) {
            return companyInvoice;
        }
        if (guest.isForeigner()) {
            return foreignReceipt;
        }
        return personReceipt;
    }
}
""")
pattern.write(DOCUMENTS + "PersonReceiptWriter.java", """package hotel.documents;

""" + PERSON_IMPORTS + """
/** Today's receipt, for a guest with a CPF. */
final class PersonReceiptWriter implements DocumentWriter {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String text(Reservation reservation, Room room) {
""" + PERSON_TEXT + """    }
}
""")
pattern.write(DOCUMENTS + "CompanyInvoiceWriter.java", """package hotel.documents;

""" + COMPANY_IMPORTS + """
/** The invoice for a company, with the 5% ISS withheld. */
final class CompanyInvoiceWriter implements DocumentWriter {

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    public String text(Reservation reservation, Room room) {
""" + COMPANY_TEXT + """    }
}
""")
pattern.write(DOCUMENTS + "ForeignReceiptWriter.java", """package hotel.documents;

""" + FOREIGN_IMPORTS + """
/** The receipt in English, for a guest with a passport. */
final class ForeignReceiptWriter implements DocumentWriter {

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    public String text(Reservation reservation, Room room) {
""" + FOREIGN_TEXT + """    }

""" + BRL + """}
""")
pattern.replace(SERVICE, "import hotel.documents.ReservationReceipt;\n",
                "import hotel.documents.DocumentFactory;\nimport hotel.documents.ReservationReceipt;\n")
pattern.replace(SERVICE, """    private final EmailSender emailSender;
""", """    private final EmailSender emailSender;
    private final DocumentFactory documents = new DocumentFactory();
""")
pattern.replace(SERVICE, "new ReservationReceipt(reservation, roomOf(reservation))",
                "documents.issue(reservation, roomOf(reservation))", count=3)
pattern.write("src/test/java/hotel/documents/DocumentsByGuestTest.java", documents_test())
pattern.build_and_save()

# ---------------------------------------------------------------- naive: the if/else in the three places
naive = Solution("05-naive")
shared_changes(naive)
naive.write(DOCUMENTS + "DocumentTexts.java", """package hotel.documents;

""" + ALL_TEXT_IMPORTS + """
/** The texts of the three documents. */
public final class DocumentTexts {

    private DocumentTexts() {
    }

    public static String personReceipt(Reservation reservation, Room room) {
""" + PERSON_TEXT + """    }

    public static String companyInvoice(Reservation reservation, Room room) {
""" + COMPANY_TEXT + """    }

    public static String foreignReceipt(Reservation reservation, Room room) {
""" + FOREIGN_TEXT + """    }

""" + BRL + """}
""")
naive.replace(SERVICE, "import hotel.documents.ReservationReceipt;\n",
              "import hotel.documents.DocumentTexts;\nimport hotel.documents.ReservationReceipt;\n")
CHOOSE = """        Room room = roomOf(reservation);
        ReservationReceipt {name};
        if (reservation.guest().isCompany()) {{
            {name} = new ReservationReceipt("Fatura de hospedagem", reservation, room,
                    DocumentTexts.companyInvoice(reservation, room));
        }} else if (reservation.guest().isForeigner()) {{
            {name} = new ReservationReceipt("Booking receipt", reservation, room,
                    DocumentTexts.foreignReceipt(reservation, room));
        }} else {{
            {name} = new ReservationReceipt("Comprovante de reserva", reservation, room,
                    DocumentTexts.personReceipt(reservation, room));
        }}
"""
naive.replace(SERVICE, """        ReservationReceipt receipt = new ReservationReceipt(reservation, roomOf(reservation));
""", CHOOSE.format(name="receipt"))
naive.replace(SERVICE, """        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
        store.update(reservation);
        return new ReservationReceipt(reservation, roomOf(reservation));""",
              """        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
        store.update(reservation);
""" + CHOOSE.format(name="finalReceipt") + """        return finalReceipt;""")
naive.replace(SERVICE, """            throw notAllowed(reservation, "issue a receipt for");
        }
        return new ReservationReceipt(reservation, roomOf(reservation));""",
              """            throw notAllowed(reservation, "issue a receipt for");
        }
""" + CHOOSE.format(name="copy") + """        return copy;""")
naive.write("src/test/java/hotel/documents/DocumentsByGuestTest.java", documents_test())
naive.build_and_save()
