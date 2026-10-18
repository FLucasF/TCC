package com.loja.orderservice.service;

import com.loja.orderservice.dto.NotificationResponse;
import com.loja.orderservice.entity.Notification;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.model.NotificationType;
import com.loja.orderservice.repository.CustomerRepository;
import com.loja.orderservice.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               CustomerRepository customerRepository) {
        this.notificationRepository = notificationRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public void notifyCustomer(String cpf, NotificationType type, Long orderId) {
        Notification notification = new Notification(cpf, type, orderId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCustomerNotifications(String cpf) {
        if (!customerRepository.existsById(cpf)) {
            throw new BusinessException("CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        List<Notification> notifications = notificationRepository.findByCustomerCpfOrderByIdAsc(cpf);

        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        notification.markAsRead();
        notificationRepository.save(notification);

        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setOrderId(notification.getOrderId());
        response.setStatus(notification.getStatus());
        return response;
    }
}
