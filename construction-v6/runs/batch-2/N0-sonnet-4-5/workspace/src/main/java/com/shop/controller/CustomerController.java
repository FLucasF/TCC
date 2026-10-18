package com.shop.controller;

import com.shop.dto.CustomerResponse;
import com.shop.dto.ErrorResponse;
import com.shop.dto.NotificationResponse;
import com.shop.exception.BusinessException;
import com.shop.model.Customer;
import com.shop.model.Notification;
import com.shop.repository.CustomerRepository;
import com.shop.repository.NotificationRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

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
            Customer customer = customerRepository.findByCpf(cpf);

            if (customer == null) {
                throw new BusinessException("CUSTOMER_NOT_FOUND", 404);
            }

            CustomerResponse response = new CustomerResponse(customer.getCpf(), customer.getCreditBalance());
            ctx.status(200).json(response);
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void getNotifications(Context ctx) {
        try {
            String cpf = ctx.pathParam("cpf");
            Customer customer = customerRepository.findByCpf(cpf);

            if (customer == null) {
                throw new BusinessException("CUSTOMER_NOT_FOUND", 404);
            }

            List<Notification> notifications = notificationRepository.findByCustomerCpf(cpf);
            List<NotificationResponse> responses = notifications.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            ctx.status(200).json(responses);
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void markNotificationAsRead(Context ctx) {
        try {
            Long notificationId = Long.parseLong(ctx.pathParam("id"));
            Notification notification = notificationRepository.findById(notificationId);

            if (notification == null) {
                throw new BusinessException("NOTIFICATION_NOT_FOUND", 404);
            }

            notificationRepository.markAsRead(notificationId);
            notification = notificationRepository.findById(notificationId);

            ctx.status(200).json(toResponse(notification));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("NOTIFICATION_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
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
