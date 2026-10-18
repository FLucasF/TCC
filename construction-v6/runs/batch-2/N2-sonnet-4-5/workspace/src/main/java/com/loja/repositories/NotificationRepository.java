package com.loja.repositories;

import com.loja.domain.entities.Notification;

import java.util.List;

public interface NotificationRepository {
    void save(String customerCpf, Notification notification);

    List<Notification> findByCustomerCpf(String customerCpf);

    Notification findById(long id);

    void update(Notification notification);
}
