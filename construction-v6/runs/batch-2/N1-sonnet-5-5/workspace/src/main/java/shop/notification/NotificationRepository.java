package shop.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    void insert(Notification notification);

    /** Oldest first. */
    List<Notification> findByCustomer(String cpf);

    Optional<Notification> find(String id);

    void markRead(String id);
}
