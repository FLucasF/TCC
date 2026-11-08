package com.pousada.marealta.domain.repository;

import com.pousada.marealta.domain.notification.PersistedNotification;
import java.util.List;

public interface NotificationRepository {
    void save(PersistedNotification notification);
    List<PersistedNotification> findAll();
    List<PersistedNotification> findByReservationId(long reservationId);
}
