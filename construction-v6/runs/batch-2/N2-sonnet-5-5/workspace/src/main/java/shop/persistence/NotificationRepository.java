package shop.persistence;

import java.util.List;
import java.util.Optional;
import shop.domain.Notification;
import shop.domain.NotificationType;

public interface NotificationRepository {
    void add(String cpf, NotificationType type, String orderId);

    /** Do mais antigo para o mais novo. */
    List<Notification> findByCpf(String cpf);

    Optional<Notification> find(String id);

    void markRead(String id);
}
