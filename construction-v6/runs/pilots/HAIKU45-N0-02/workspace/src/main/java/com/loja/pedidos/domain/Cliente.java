package com.loja.pedidos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private BigDecimal saldoCredito;

    @Column(nullable = false)
    private String nivelClube;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    public Cliente() {}

    public Cliente(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
        this.saldoCredito = BigDecimal.ZERO;
        this.nivelClube = "BRONZE";
        this.criadoEm = Instant.now();
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

    public BigDecimal getSaldoCredito() {
        return saldoCredito;
    }

    public void setSaldoCredito(BigDecimal saldoCredito) {
        this.saldoCredito = saldoCredito;
    }

    public String getNivelClube() {
        return nivelClube;
    }

    public void setNivelClube(String nivelClube) {
        this.nivelClube = nivelClube;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
