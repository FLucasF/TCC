package br.com.mareaalta.service;

import br.com.mareaalta.domain.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ReservationService {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room " + roomNumber + " not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room " + roomNumber + " has capacity for " + room.getCapacity() + " guests");
        }

        DocumentGenerator documentGenerator = determineDocumentGenerator(guestCpf, companyCnpj, companyName, guestPassport, guestCountry);

        Reservation reservation = new Reservation(
            roomNumber, room, guestName, guestEmail,
            guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
            checkIn, checkOut, guests, ratePlan, documentGenerator
        );

        documentGenerator.validate(reservation);

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room " + roomNumber + " is not available for the selected dates");
        }

        reservation.setId(idGenerator.getAndIncrement());
        reservations.add(reservation);
        return reservation;
    }

    private DocumentGenerator determineDocumentGenerator(String guestCpf, String companyCnpj,
                                                         String companyName, String guestPassport,
                                                         String guestCountry) {
        int providedTypes = 0;
        DocumentGenerator generator = null;

        if (guestCpf != null && !guestCpf.isBlank()) {
            providedTypes++;
            generator = new CpfDocumentGenerator();
        }

        if ((companyCnpj != null && !companyCnpj.isBlank()) ||
            (companyName != null && !companyName.isBlank())) {
            providedTypes++;
            generator = new CnpjDocumentGenerator();
        }

        if ((guestPassport != null && !guestPassport.isBlank()) ||
            (guestCountry != null && !guestCountry.isBlank())) {
            providedTypes++;
            generator = new PassportDocumentGenerator();
        }

        if (providedTypes != 1) {
            throw new InvalidRequestException("Exactly one guest identification type must be provided (CPF, CNPJ+Company, or Passport+Country)");
        }

        return generator;
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId))
            .filter(Reservation::isActive)
            .anyMatch(r -> hasOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean hasOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public Optional<Reservation> getReservation(Long id) {
        return reservations.stream()
            .filter(r -> r.getId().equals(id))
            .findFirst();
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.confirm();

        DocumentData document = reservation.generateDocument();
        messageService.sendConfirmationEmail(reservation, document);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        reservation.cancel();
        return reservation;
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check in reservation in status " + reservation.getStatus());
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOutReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check out reservation in status " + reservation.getStatus());
        }

        reservation.checkOut();
        return reservation;
    }

    public DocumentData getDocument(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        return reservation.generateDocument();
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> r.occupiesNight(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getRoom().getType()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn()).append(",");
            csv.append(r.getCheckOut()).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Reservation> relevantReservations = reservations.stream()
            .filter(r -> !r.getCheckIn().isBefore(monthStart) && !r.getCheckIn().isAfter(monthEnd))
            .filter(r -> r.getNetRevenue().compareTo(java.math.BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;

        for (Reservation r : relevantReservations) {
            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn()).append(",");
            csv.append(r.getCheckOut()).append(",");
            csv.append(r.getStatus()).append(",");
            csv.append(formatCsvAmount(r.getNetRevenue())).append("\n");
            totalRevenue = totalRevenue.add(r.getNetRevenue());
        }

        csv.append("total,").append(formatCsvAmount(totalRevenue));

        return csv.toString();
    }

    private String formatCsvAmount(java.math.BigDecimal amount) {
        return amount.toString();
    }
}
