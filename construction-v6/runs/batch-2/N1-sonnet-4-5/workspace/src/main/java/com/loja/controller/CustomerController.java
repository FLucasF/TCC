package com.loja.controller;

import com.loja.domain.Customer;
import com.loja.domain.Notification;
import com.loja.repository.CustomerRepository;
import com.loja.repository.NotificationRepository;
import com.loja.util.ApiException;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class CustomerController {
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;

    public CustomerController(CustomerRepository customerRepository, NotificationRepository notificationRepository) {
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
    }

    public void getCustomer(Context ctx) {
        try {
            String cpf = ctx.pathParam("cpf");
            Customer customer = customerRepository.findByCpf(cpf)
                .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", 404));
            ctx.status(200).json(customer);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void getCustomerNotifications(Context ctx) {
        try {
            String cpf = ctx.pathParam("cpf");
            if (customerRepository.findByCpf(cpf).isEmpty()) {
                throw new ApiException("CUSTOMER_NOT_FOUND", 404);
            }
            List<Notification> notifications = notificationRepository.findByCustomerCpf(cpf);
            ctx.status(200).json(notifications);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void markNotificationAsRead(Context ctx) {
        try {
            String notificationId = ctx.pathParam("id");
            Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("NOTIFICATION_NOT_FOUND", 404));

            notificationRepository.markAsRead(notificationId);
            notification.setStatus(com.loja.domain.NotificationStatus.READ);
            ctx.status(200).json(notification);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }
}
