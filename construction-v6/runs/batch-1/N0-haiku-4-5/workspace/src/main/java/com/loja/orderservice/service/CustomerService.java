package com.loja.orderservice.service;

import com.loja.orderservice.domain.entity.Customer;
import com.loja.orderservice.domain.entity.Notification;
import com.loja.orderservice.domain.enums.NotificationStatus;
import com.loja.orderservice.dto.CustomerResponse;
import com.loja.orderservice.dto.NotificationResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.repository.CustomerRepository;
import com.loja.orderservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String cpf) {
        Optional<Customer> customerOpt = customerRepository.findById(cpf);

        if (customerOpt.isEmpty()) {
            throw new OrderServiceException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND);
        }

        Customer customer = customerOpt.get();

        return CustomerResponse.builder()
                .cpf(customer.getCpf())
                .creditBalance(customer.getCreditBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String cpf) {
        List<Notification> notifications = notificationRepository.findByCustomerCpfOrderByCreatedAtAsc(cpf);

        if (notifications.isEmpty()) {
            Optional<Customer> customerOpt = customerRepository.findById(cpf);
            if (customerOpt.isEmpty()) {
                throw new OrderServiceException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND);
            }
        }

        return notifications.stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .orderId(n.getOrderId())
                        .status(n.getStatus())
                        .build())
                .toList();
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(String notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);

        if (notificationOpt.isEmpty()) {
            throw new OrderServiceException("NOTIFICATION_NOT_FOUND", "Notification not found", HttpStatus.NOT_FOUND);
        }

        Notification notification = notificationOpt.get();
        notification.setStatus(NotificationStatus.READ);
        notification = notificationRepository.save(notification);

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .orderId(notification.getOrderId())
                .status(notification.getStatus())
                .build();
    }
}
