package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.util.DateUtil;
import java.util.*;

public class NotificationService {
    private final MessageRepository messageRepository;
    private final List<NotificationHandler> handlers = new ArrayList<>();

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers() {
        handlers.add(new GuestConfirmationHandler(messageRepository));
        handlers.add(new GovernanceConfirmationHandler(messageRepository));
        handlers.add(new FinanceConfirmationHandler(messageRepository));
        handlers.add(new GovernanceCancellationHandler(messageRepository));
        handlers.add(new FinanceCancellationHandler(messageRepository));
    }

    public void notifyReservationConfirmed(Reservation reservation, RoomType roomType) {
        for (NotificationHandler handler : handlers) {
            if (handler.shouldHandle(ReservationEvent.CONFIRMED)) {
                handler.handle(reservation, roomType, null);
            }
        }
    }

    public void notifyReservationCancelled(Reservation reservation, RoomType roomType, Double refundAmount) {
        for (NotificationHandler handler : handlers) {
            if (handler.shouldHandle(ReservationEvent.CANCELLED)) {
                handler.handle(reservation, roomType, refundAmount);
            }
        }
    }

    public void registerHandler(NotificationHandler handler) {
        handlers.add(handler);
    }

    interface NotificationHandler {
        boolean shouldHandle(ReservationEvent event);
        void handle(Reservation reservation, RoomType roomType, Double refundAmount);
    }

    enum ReservationEvent {
        CONFIRMED, CANCELLED
    }

    static class GuestConfirmationHandler implements NotificationHandler {
        private final MessageRepository messageRepository;

        GuestConfirmationHandler(MessageRepository messageRepository) {
            this.messageRepository = messageRepository;
        }

        @Override
        public boolean shouldHandle(ReservationEvent event) {
            return event == ReservationEvent.CONFIRMED;
        }

        @Override
        public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
            String document = formatDocument(reservation, roomType);
            String body = """
                    Olá, %s!

                    Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                    %s

                    A entrada é a partir das 14h, e a saída, até as 12h.
                    Até breve!
                    """.formatted(reservation.getGuestName(), document);

            messageRepository.save(new Message(
                    0,
                    MessageChannel.EMAIL,
                    reservation.getGuestEmail(),
                    "Reserva confirmada - Pousada Maré Alta",
                    body
            ));
        }

        private String formatDocument(Reservation reservation, RoomType roomType) {
            String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
            String document = "Comprovante de reserva nº " + reservation.getId() + "\n" +
                    "Hóspede: " + reservation.getGuestName() + " (CPF " + formatCpf(reservation.getGuestCpf()) + ")\n" +
                    "Quarto: " + reservation.getRoomNumber() + " (" + roomType.getDisplayName() + ")\n" +
                    "Entrada: " + DateUtil.format(reservation.getCheckIn()) + " | Saída: " + DateUtil.format(reservation.getCheckOut()) + " | " + nightsText + "\n" +
                    "Diárias: R$ " + formatMoney(reservation.getSubtotal());
            if (reservation.getDiscount() > 0) {
                document += "\nDesconto: R$ " + formatMoney(reservation.getDiscount());
            }
            document += "\nTotal: R$ " + formatMoney(reservation.getTotal());
            return document;
        }

        private String formatCpf(String cpf) {
            if (cpf.length() == 11) {
                return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
            }
            return cpf;
        }

        private String formatMoney(double amount) {
            return String.format("%.2f", amount).replace(".", ",");
        }
    }

    static class GovernanceConfirmationHandler implements NotificationHandler {
        private final MessageRepository messageRepository;

        GovernanceConfirmationHandler(MessageRepository messageRepository) {
            this.messageRepository = messageRepository;
        }

        @Override
        public boolean shouldHandle(ReservationEvent event) {
            return event == ReservationEvent.CONFIRMED;
        }

        @Override
        public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
            String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
            String body = "Entrada: " + DateUtil.format(reservation.getCheckIn()) + "\n" +
                    "Saída: " + DateUtil.format(reservation.getCheckOut()) + "\n" +
                    "Hóspedes: " + reservation.getGuests();

            messageRepository.save(new Message(
                    0,
                    MessageChannel.EMAIL,
                    "governanca@pousadamarealta.com.br",
                    subject,
                    body
            ));
        }
    }

    static class FinanceConfirmationHandler implements NotificationHandler {
        private final MessageRepository messageRepository;

        FinanceConfirmationHandler(MessageRepository messageRepository) {
            this.messageRepository = messageRepository;
        }

        @Override
        public boolean shouldHandle(ReservationEvent event) {
            return event == ReservationEvent.CONFIRMED;
        }

        @Override
        public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
            String subject = "Pagamento recebido - reserva " + reservation.getId();
            String body = "Valor: R$ " + formatMoney(reservation.getAmountPaid());

            messageRepository.save(new Message(
                    0,
                    MessageChannel.EMAIL,
                    "financeiro@pousadamarealta.com.br",
                    subject,
                    body
            ));
        }

        private String formatMoney(double amount) {
            return String.format("%.2f", amount).replace(".", ",");
        }
    }

    static class GovernanceCancellationHandler implements NotificationHandler {
        private final MessageRepository messageRepository;

        GovernanceCancellationHandler(MessageRepository messageRepository) {
            this.messageRepository = messageRepository;
        }

        @Override
        public boolean shouldHandle(ReservationEvent event) {
            return event == ReservationEvent.CANCELLED;
        }

        @Override
        public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                return;
            }

            String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
            String body = "Entrada: " + DateUtil.format(reservation.getCheckIn()) + "\n" +
                    "Saída: " + DateUtil.format(reservation.getCheckOut());

            messageRepository.save(new Message(
                    0,
                    MessageChannel.EMAIL,
                    "governanca@pousadamarealta.com.br",
                    subject,
                    body
            ));
        }
    }

    static class FinanceCancellationHandler implements NotificationHandler {
        private final MessageRepository messageRepository;

        FinanceCancellationHandler(MessageRepository messageRepository) {
            this.messageRepository = messageRepository;
        }

        @Override
        public boolean shouldHandle(ReservationEvent event) {
            return event == ReservationEvent.CANCELLED;
        }

        @Override
        public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                return;
            }

            if (refundAmount == null || refundAmount == 0) {
                return;
            }

            String subject = "Reembolso - reserva " + reservation.getId();
            String body = "Valor: R$ " + formatMoney(refundAmount);

            messageRepository.save(new Message(
                    0,
                    MessageChannel.EMAIL,
                    "financeiro@pousadamarealta.com.br",
                    subject,
                    body
            ));
        }

        private String formatMoney(double amount) {
            return String.format("%.2f", amount).replace(".", ",");
        }
    }
}
