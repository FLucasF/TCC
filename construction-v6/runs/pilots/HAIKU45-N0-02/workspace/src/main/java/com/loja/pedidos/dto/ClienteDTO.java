package com.loja.pedidos.dto;

import java.math.BigDecimal;

public class ClienteDTO {
    public String cpf;
    public String email;

    public ClienteDTO() {}

    public ClienteDTO(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
