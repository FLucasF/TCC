"""Scenario 05 (Factory Method): the pattern solution (one DocumentFactory picks the writer by kind of guest; the three
places ask it) and the naive one (the if/else on the kind of guest repeated in the three places)."""

from kit import HTTP_API, RECEIPT, SERVICE, Solution

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


def shared_changes(solution: Solution) -> None:
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

class DocumentsByGuestTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    private Reservation bookAndPay(Guest guest) {
        Reservation reservation = service.create(new NewReservation(
                201, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, RatePlan.STANDARD));
        service.confirm(reservation.id());
        return reservation;
    }

    @Test
    void aCompanyGetsAnInvoiceWithTheWithheldTax() {
        Reservation reservation = bookAndPay(new Guest("Ana Souza", "ana@example.com", null,
                "12.345.678/0001-90", "Tecnosul Ltda", null, null));

        ReservationReceipt invoice = service.receiptCopy(reservation.id());

        assertEquals("Fatura de hospedagem", invoice.title());
        assertTrue(invoice.text().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(invoice.text().contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void aForeignerGetsAnEnglishReceipt() {
        Reservation reservation = bookAndPay(new Guest("John Smith", "john@example.com", null, null, null,
                "X1234567", "United Kingdom"));

        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals("Booking receipt", receipt.title());
        assertTrue(receipt.text().contains("Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights"));
        assertTrue(receipt.text().contains("Total: BRL 779.80"));
    }

    @Test
    void aPersonKeepsTheReceipt() {
        Reservation reservation = bookAndPay(new Guest("Ana Souza", "ana@example.com", "123.456.789-09"));

        assertEquals("Comprovante de reserva", service.receiptCopy(reservation.id()).title());
    }
}
"""


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
        return new ReservationReceipt(reservation, roomOf(reservation));""",
              """        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
""" + CHOOSE.format(name="finalReceipt") + """        return finalReceipt;""")
naive.replace(SERVICE, """            throw notAllowed(reservation, "issue a receipt for");
        }
        return new ReservationReceipt(reservation, roomOf(reservation));""",
              """            throw notAllowed(reservation, "issue a receipt for");
        }
""" + CHOOSE.format(name="copy") + """        return copy;""")
naive.write("src/test/java/hotel/documents/DocumentsByGuestTest.java", documents_test())
naive.build_and_save()
