package com.loja.pedidos.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pagamento")
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false, unique = true)
    private String idTransacao;

    @Column(nullable = false)
    private Instant pagoEm;

    @Column(nullable = false, updatable = false)
    private Instant registradoEm;

    public Pagamento() {}

    public Pagamento(Long pedidoId, String idTransacao, Instant pagoEm) {
        this.pedidoId = pedidoId;
        this.idTransacao = idTransacao;
        this.pagoEm = pagoEm;
        this.registradoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(String idTransacao) {
        this.idTransacao = idTransacao;
    }

    public Instant getPagoEm() {
        return pagoEm;
    }

    public void setPagoEm(Instant pagoEm) {
        this.pagoEm = pagoEm;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(Instant registradoEm) {
        this.registradoEm = registradoEm;
    }
}
