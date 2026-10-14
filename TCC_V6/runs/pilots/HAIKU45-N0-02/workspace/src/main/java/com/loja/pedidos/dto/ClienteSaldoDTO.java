package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ClienteSaldoDTO {
    @JsonProperty("cpf")
    public String cpf;

    @JsonProperty("saldoCredito")
    public BigDecimal saldoCredito;

    public ClienteSaldoDTO() {}

    public ClienteSaldoDTO(String cpf, BigDecimal saldoCredito) {
        this.cpf = cpf;
        this.saldoCredito = saldoCredito;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public BigDecimal getSaldoCredito() {
        return saldoCredito;
    }

    public void setSaldoCredito(BigDecimal saldoCredito) {
        this.saldoCredito = saldoCredito;
    }
}
