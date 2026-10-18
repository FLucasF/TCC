package com.loja.service;

import com.loja.domain.Notification;
import com.loja.exception.ErrorCode;
import com.loja.repository.CustomerRepository;
import com.loja.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               CustomerRepository customerRepository) {
        this.notificationRepository = notificationRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Notification> getCustomerNotifications(String cpf) {
        if (!customerRepository.existsById(cpf)) {
            throw ErrorCode.CUSTOMER_NOT_FOUND.asException();
        }
        return notificationRepository.findByCustomerCpfOrderByIdAsc(cpf);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> ErrorCode.NOTIFICATION_NOT_FOUND.asException());

        notification.markAsRead();
        return notificationRepository.save(notification);
    }
}
