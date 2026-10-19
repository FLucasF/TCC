package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService {
    private static ReservationService instance;
    private Map<Integer, Room> rooms;
    private Map<Integer, Reservation> reservations;
    private Map<Integer, Message> messages;
    private int nextReservationId;
    private int nextMessageId;

    private ReservationService() {
        this.rooms = new LinkedHashMap<>();
        this.reservations = new LinkedHashMap<>();
        this.messages = new LinkedHashMap<>();
        this.nextReservationId = 1;
        this.nextMessageId = 1;
        initializeRooms();
    }

    public static ReservationService getInstance() {
        if (instance == null) {
            instance = new ReservationService();
        }
        return instance;
    }

    public static void reset() {
        instance = new ReservationService();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, RoomType.STANDARD, 2, 249.90));
        rooms.put(102, new Room(102, RoomType.STANDARD, 2, 249.90));
        rooms.put(201, new Room(201, RoomType.DELUXE, 3, 389.90));
        rooms.put(202, new Room(202, RoomType.DELUXE, 3, 389.90));
        rooms.put(301, new Room(301, RoomType.SUITE, 4, 579.90));
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) {
        Room room = rooms.get(roomNumber);
        if (room == null) {
            return null;
        }

        if (guests < 1 || guests > room.getCapacity()) {
            return null;
        }

        if (!checkOut.isAfter(checkIn)) {
            return null;
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut, null)) {
            return null;
        }

        double[] nightlyPrices = PricingService.calculateNightlyPrices(room.getNightlyRate(), checkIn, checkOut);
        int nights = nightlyPrices.length;
        double subtotal = PricingService.calculateSubtotal(nightlyPrices);
        double discount = PricingService.calculateDiscount(subtotal, ratePlan);
        double total = PricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            nextReservationId++,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            RatePlan.valueOf(ratePlan),
            nights,
            subtotal,
            discount,
            total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(int id) {
        return reservations.get(id);
    }

    public Reservation confirmReservation(int id, double amountPaid) {
        Reservation res = reservations.get(id);
        if (res == null || res.getStatus() != ReservationStatus.PENDING) {
            return null;
        }

        res.setStatus(ReservationStatus.CONFIRMED);
        res.setAmountPaid(amountPaid);

        sendConfirmationEmails(res);

        return res;
    }

    public Reservation checkIn(int id) {
        Reservation res = reservations.get(id);
        if (res == null || res.getStatus() != ReservationStatus.CONFIRMED) {
            return null;
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        return res;
    }

    public Reservation checkOut(int id) {
        Reservation res = reservations.get(id);
        if (res == null || res.getStatus() != ReservationStatus.CHECKED_IN) {
            return null;
        }

        res.setStatus(ReservationStatus.CHECKED_OUT);
        return res;
    }

    public Reservation cancelReservation(int id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            return null;
        }

        if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
            return null;
        }

        ReservationStatus oldStatus = res.getStatus();
        res.setStatus(ReservationStatus.CANCELLED);

        if (oldStatus == ReservationStatus.CONFIRMED) {
            sendCancellationEmails(res);
        }

        return res;
    }

    private void sendConfirmationEmails(Reservation res) {
        Room room = rooms.get(res.getRoomNumber());

        String guestCheckIn = FormatService.formatDateBR(res.getCheckIn());
        String guestCheckOut = FormatService.formatDateBR(res.getCheckOut());
        String checkInISO = FormatService.formatDateISO(res.getCheckIn());
        String checkOutISO = FormatService.formatDateISO(res.getCheckOut());

        String guestCpfFormatted = FormatService.formatCPF(res.getGuestCpf());
        String subtotalStr = FormatService.formatCurrency(res.getSubtotal());
        String totalStr = FormatService.formatCurrency(res.getTotal());
        String discountStr = FormatService.formatCurrency(res.getDiscount());

        String nightsText = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";

        String receiptText = String.format(
            "Comprovante de reserva nº %d\nHóspede: %s (CPF %s)\nQuarto: %d (%s)\nEntrada: %s | Saída: %s | %s\nDiárias: %s",
            res.getId(),
            res.getGuestName(),
            guestCpfFormatted,
            res.getRoomNumber(),
            room.getType(),
            guestCheckIn,
            guestCheckOut,
            nightsText,
            subtotalStr
        );

        if (res.getDiscount() > 0) {
            receiptText += "\nDesconto: " + discountStr;
        }

        receiptText += "\nTotal: " + totalStr;

        String emailBody = String.format(
            "Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!",
            res.getGuestName(),
            receiptText
        );

        sendMessage("EMAIL", res.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);

        String governancaSubject = String.format("Reserva confirmada - quarto %d", res.getRoomNumber());
        String governancaBody = String.format("Entrada: %s\nSaída: %s\nHóspedes: %d",
            guestCheckIn, guestCheckOut, res.getGuests());
        sendMessage("EMAIL", "governanca@pousadamarealta.com.br", governancaSubject, governancaBody);

        String financeSubject = String.format("Pagamento recebido - reserva %d", res.getId());
        String financeBody = String.format("Valor pago: %s", totalStr);
        sendMessage("EMAIL", "financeiro@pousadamarealta.com.br", financeSubject, financeBody);
    }

    private void sendCancellationEmails(Reservation res) {
        String guestCheckIn = FormatService.formatDateBR(res.getCheckIn());
        String guestCheckOut = FormatService.formatDateBR(res.getCheckOut());

        String governancaSubject = String.format("Reserva cancelada - quarto %d", res.getRoomNumber());
        String governancaBody = String.format("Entrada: %s\nSaída: %s", guestCheckIn, guestCheckOut);
        sendMessage("EMAIL", "governanca@pousadamarealta.com.br", governancaSubject, governancaBody);

        if (res.getAmountPaid() > 0) {
            double refundAmount = res.getRatePlan() == RatePlan.STANDARD ? res.getAmountPaid() : 0.0;
            if (refundAmount > 0) {
                res.setRefund(refundAmount);
                String financeSubject = String.format("Reembolso - reserva %d", res.getId());
                String financeBody = String.format("Valor a devolver: %s", FormatService.formatCurrency(refundAmount));
                sendMessage("EMAIL", "financeiro@pousadamarealta.com.br", financeSubject, financeBody);
            }
        }
    }

    public void sendMessage(String channel, String to, String subject, String body) {
        Message msg = new Message(nextMessageId++, channel, to, subject, body);
        messages.put(msg.getId(), msg);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages.values());
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservations.values().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED &&
                        !r.getCheckIn().isAfter(date) &&
                        r.getCheckOut().isAfter(date))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        for (Reservation res : occupiedReservations) {
            Room room = rooms.get(res.getRoomNumber());
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                res.getRoomNumber(),
                room.getType(),
                res.getId(),
                FormatService.formatDateISO(res.getCheckIn()),
                FormatService.formatDateISO(res.getCheckOut())));
        }

        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), rooms.size()));

        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0.0;
        List<Reservation> monthReservations = reservations.values().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month) &&
                        (r.getStatus() == ReservationStatus.CONFIRMED ||
                         r.getStatus() == ReservationStatus.CHECKED_IN ||
                         r.getStatus() == ReservationStatus.CHECKED_OUT ||
                         r.getStatus() == ReservationStatus.CANCELLED))
            .sorted(Comparator.comparingInt((Reservation r) -> r.getCheckIn().getDayOfMonth())
                            .thenComparingInt(Reservation::getId))
            .collect(Collectors.toList());

        for (Reservation res : monthReservations) {
            double amount = res.getAmountPaid() - (res.getRefund() != null ? res.getRefund() : 0.0);
            if (amount > 0 || res.getStatus() == ReservationStatus.CANCELLED) {
                csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                    res.getId(),
                    res.getRoomNumber(),
                    FormatService.formatDateISO(res.getCheckIn()),
                    FormatService.formatDateISO(res.getCheckOut()),
                    res.getStatus(),
                    PricingService.roundHalfToEven(amount)));
                totalAmount += amount;
            }
        }

        csv.append(String.format("total,%.2f\n", PricingService.roundHalfToEven(totalAmount)));

        return csv.toString();
    }

    private boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, Integer excludeReservationId) {
        for (Reservation res : reservations.values()) {
            if (excludeReservationId != null && res.getId() == excludeReservationId) {
                continue;
            }

            if (res.getRoomNumber() != roomNumber || res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            if (!checkOut.isAfter(res.getCheckIn()) || !res.getCheckOut().isAfter(checkIn)) {
                continue;
            }

            return false;
        }

        return true;
    }
}
