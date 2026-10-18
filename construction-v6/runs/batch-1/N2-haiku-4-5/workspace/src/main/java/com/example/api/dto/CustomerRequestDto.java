package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerRequestDto {
    @JsonProperty
    private String cpf;

    @JsonProperty
    private String email;

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
