package com.example.application.service;

import com.example.domain.entity.Notification;
import com.example.domain.enums.NotificationType;
import com.example.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(String cpf, NotificationType type, String orderId) {
        Notification notification = new Notification(cpf, type, orderId);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByCustomer(String cpf) {
        return notificationRepository.findByCustomerCpf(cpf);
    }

    public Notification getNotification(String id) {
        Optional<Notification> notification = notificationRepository.findById(id);
        return notification.orElse(null);
    }

    public void markAsRead(String id) {
        Optional<Notification> notification = notificationRepository.findById(id);
        if (notification.isPresent()) {
            notification.get().markAsRead();
            notificationRepository.save(notification.get());
        }
    }
}
