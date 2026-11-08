package br.com.pousadamarealta.domain.reservation;

import br.com.pousadamarealta.domain.message.Message;
import br.com.pousadamarealta.domain.message.MessageChannel;
import br.com.pousadamarealta.domain.message.MessageRepository;
import br.com.pousadamarealta.domain.pricing.PricingCalculator;
import br.com.pousadamarealta.domain.room.Room;
import br.com.pousadamarealta.domain.room.RoomRepository;
import br.com.pousadamarealta.util.DateUtils;
import br.com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation create(String roomNumber, String guestName, String guestEmail, String guestCpf,
                             java.time.LocalDate checkIn, java.time.LocalDate checkOut,
                             int guests, RatePlan ratePlan) throws SQLException {

        // Validar quarto
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Validar capacidade
        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("Capacity exceeded");
        }

        // Validar disponibilidade
        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new IllegalArgumentException("Room unavailable");
        }

        // Calcular valores
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        // Criar reserva
        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirm(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.confirm();
        reservationRepository.update(reservation);

        // Enviar e-mail de confirmação
        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancel(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.cancel();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.checkIn();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) throws SQLException {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.checkOut();
        reservationRepository.update(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) throws SQLException {
        String document = generateDocument(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        Message message = new Message();
        message.setChannel(MessageChannel.EMAIL);
        message.setTo(reservation.getGuestEmail());
        message.setSubject("Reserva confirmada - Pousada Maré Alta");
        message.setBody(body);

        messageRepository.save(message);
    }

    public String generateDocument(Reservation reservation) throws SQLException {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        String cpfFormatted = formatCpf(reservation.getGuestCpf());
        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(cpfFormatted).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(room.getType().name()).append(")\n");
        sb.append("Entrada: ").append(DateUtils.formatDisplay(reservation.getCheckIn()))
          .append(" | Saída: ").append(DateUtils.formatDisplay(reservation.getCheckOut()))
          .append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(MoneyUtils.formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyUtils.formatCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyUtils.formatCurrency(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        // Remove tudo que não é número
        String numbers = cpf.replaceAll("\\D", "");
        // Formata: 111.222.333-44
        return numbers.substring(0, 3) + "." +
               numbers.substring(3, 6) + "." +
               numbers.substring(6, 9) + "-" +
               numbers.substring(9, 11);
    }
}
