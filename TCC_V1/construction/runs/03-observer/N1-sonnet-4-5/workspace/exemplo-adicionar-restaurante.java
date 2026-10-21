/*
 * Exemplo: Como adicionar notificações para o restaurante
 *
 * Seguindo o CLAUDE.md, quando quisermos adicionar o restaurante para ser notificado
 * sobre confirmações de reservas (para planejar o café da manhã), basta:
 *
 * 1. Criar um novo listener:
 */

package com.pousada.listener;

import com.pousada.domain.Reservation;
import com.pousada.event.EventListener;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEvent;
import com.pousada.service.NotificationService;

public class RestaurantNotificationListener implements EventListener {
    private final NotificationService notificationService;

    public RestaurantNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            sendBreakfastNotification(event.getReservation());
        }
    }

    private void sendBreakfastNotification(Reservation reservation) {
        notificationService.sendRestaurantConfirmationEmail(reservation);
    }
}

/*
 * 2. Adicionar o método no NotificationService:
 */

// public void sendRestaurantConfirmationEmail(Reservation reservation) {
//     String subject = String.format("Café da manhã - reserva %d", reservation.getId());
//     String body = String.format(
//         "Nova reserva confirmada:\n\n" +
//         "Reserva: %d\n" +
//         "Entrada: %s\n" +
//         "Hóspedes: %d\n" +
//         "Preparar café da manhã para %d pessoas.",
//         reservation.getId(),
//         DateFormatter.formatBR(reservation.getCheckIn()),
//         reservation.getGuests(),
//         reservation.getGuests()
//     );
//
//     messageRepository.save(MessageChannel.EMAIL, "restaurante@pousadamarealta.com.br", subject, body);
// }

/*
 * 3. Registrar o listener no Application.java:
 */

// eventPublisher.register(new RestaurantNotificationListener(notificationService));

/*
 * Pronto! Sem mexer na lógica de confirmação de reservas, sem adicionar if/else,
 * apenas criando um novo listener e registrando-o.
 *
 * Para remover o restaurante no futuro (quando o contrato terminar), basta comentar
 * ou remover a linha de registro. A lógica de confirmação continua intacta.
 *
 * O mesmo padrão serve para:
 * - Transfer do aeroporto (quer saber quem está chegando)
 * - Manutenção (quer saber quando os quartos ficam livres)
 * - Marketing (quer saber sobre cancelamentos para remarketing)
 * - Etc.
 *
 * Cada listener decide independentemente se deve reagir a um evento, e o que fazer.
 */
