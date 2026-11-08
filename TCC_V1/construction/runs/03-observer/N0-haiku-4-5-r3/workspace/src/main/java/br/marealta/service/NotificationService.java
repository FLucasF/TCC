package br.marealta.service;

import br.marealta.db.MessageDAO;
import br.marealta.model.Message;
import br.marealta.model.MessageChannel;
import br.marealta.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final MessageDAO messageDAO;
    private final List<NotificationHandler> handlers = new ArrayList<>();

    public NotificationService(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public void registerHandler(NotificationHandler handler) {
        handlers.add(handler);
    }

    public void send(Notification notification) {
        Message message = new Message(
            notification.getChannel(),
            notification.getTo(),
            notification.getSubject(),
            notification.getBody()
        );
        messageDAO.save(message);
        logger.info("Notification sent to {} with subject '{}'", notification.getTo(), notification.getSubject());
    }

    public void notifyReservationConfirmed(Reservation reservation) {
        for (NotificationHandler handler : handlers) {
            try {
                handler.onReservationConfirmed(reservation);
            } catch (Exception e) {
                logger.error("Error notifying handler on reservation confirmed", e);
            }
        }
    }

    public void notifyReservationCancelled(Reservation reservation) {
        for (NotificationHandler handler : handlers) {
            try {
                handler.onReservationCancelled(reservation);
            } catch (Exception e) {
                logger.error("Error notifying handler on reservation cancelled", e);
            }
        }
    }
}
