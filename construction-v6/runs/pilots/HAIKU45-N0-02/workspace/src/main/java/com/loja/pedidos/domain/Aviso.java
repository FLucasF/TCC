package com.loja.pedidos.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "aviso")
public class Aviso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cpfCliente;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private String situacao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    public Aviso() {}

    public Aviso(String cpfCliente, String tipo, Long pedidoId) {
        this.cpfCliente = cpfCliente;
        this.tipo = tipo;
        this.pedidoId = pedidoId;
        this.situacao = "NOVO";
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpfCliente() {
        return cpfCliente;
    }

    public void setCpfCliente(String cpfCliente) {
        this.cpfCliente = cpfCliente;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
