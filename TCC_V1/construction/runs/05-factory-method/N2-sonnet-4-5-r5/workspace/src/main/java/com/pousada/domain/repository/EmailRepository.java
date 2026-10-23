package com.pousada.domain.repository;

import com.pousada.domain.model.Email;

import java.util.List;

public interface EmailRepository {
    Email save(Email email);
    List<Email> findAll();
}
