package com.loja.order.service;

import com.loja.order.dto.NotificationResponse;
import com.loja.order.model.Notification;
import com.loja.order.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public List<NotificationResponse> getNotifications(String cpf) throws NotFoundException {
    List<Notification> notifications = notificationRepository.findByCustomerCpf(cpf);
    if (notifications.isEmpty()) {
      throw new NotFoundException("CUSTOMER_NOT_FOUND");
    }
    return notifications.stream()
      .map(this::convertToResponse)
      .toList();
  }

  @Transactional
  public NotificationResponse markAsRead(Long notificationId) throws NotFoundException {
    Notification notification = notificationRepository.findById(notificationId)
      .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND"));

    notification.markAsRead();
    Notification saved = notificationRepository.save(notification);
    return convertToResponse(saved);
  }

  private NotificationResponse convertToResponse(Notification notification) {
    return new NotificationResponse(
      notification.getId(),
      notification.getType().name(),
      notification.getOrderId(),
      notification.getStatus().name()
    );
  }

  public static class NotFoundException extends Exception {
    public NotFoundException(String message) {
      super(message);
    }
  }
}
