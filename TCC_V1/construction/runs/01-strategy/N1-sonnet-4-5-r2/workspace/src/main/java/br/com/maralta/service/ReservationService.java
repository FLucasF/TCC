package br.com.maralta.service;

import br.com.maralta.domain.*;
import br.com.maralta.domain.rateplan.CorporateRatePlan;
import br.com.maralta.domain.rateplan.RatePlan;
import br.com.maralta.domain.rateplan.RatePlanFactory;
import br.com.maralta.repository.ReservationRepository;
import br.com.maralta.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlanType ratePlanType, String companyCode) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlanType);
        reservation.setCompanyCode(companyCode);
        reservation.calculateNights();

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);

        try {
            ratePlan.validate(checkIn, checkOut, reservation.getNights(), room.getType(), companyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        BigDecimal subtotal = ratePlan.calculateSubtotal(checkIn, checkOut, reservation.getNights(), room.getType());
        reservation.setSubtotal(subtotal);

        BigDecimal discount;
        if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof CorporateRatePlan corporatePlan) {
            discount = corporatePlan.calculateDiscount(subtotal, companyCode);
        } else {
            discount = ratePlan.calculateDiscount(subtotal);
        }
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
        reservation.setTotal(total);

        List<Reservation> overlapping = reservationRepository.findOverlapping(
            roomNumber, checkIn, checkOut, null);

        if (!overlapping.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING
            && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        RatePlan ratePlan = RatePlanFactory.create(reservation.getRatePlan());
        BigDecimal refund = ratePlan.calculateRefund(reservation.getAmountPaid());
        reservation.setRefund(refund);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public String generateDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING
            || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        return buildDocument(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String document = buildDocument(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            document);

        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String buildDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(room.getType().name()).append(")\n");
        doc.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
           .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public List<Reservation> getOccupancyByNight(LocalDate night) {
        return reservationRepository.findByNight(night);
    }

    public List<Reservation> getRevenueByMonth(YearMonth month) {
        return reservationRepository.findByMonth(month);
    }
}
