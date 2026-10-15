package com.loja.pedidos.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cpfCliente;

    @Column(nullable = false)
    private String situacao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode resumo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id")
    private List<PedidoItem> itens = new ArrayList<>();

    @Column(name = "codigo_rastreio")
    private String codigoRastreio;

    @Column(name = "id_transacao")
    private String idTransacao;

    public Pedido() {}

    public Pedido(String cpfCliente, JsonNode resumo, List<PedidoItem> itens) {
        this.cpfCliente = cpfCliente;
        this.situacao = "AGUARDANDO_PAGAMENTO";
        this.criadoEm = Instant.now();
        this.resumo = resumo;
        this.itens = itens;
        this.codigoRastreio = "";
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

    public JsonNode getResumo() {
        return resumo;
    }

    public void setResumo(JsonNode resumo) {
        this.resumo = resumo;
    }

    public List<PedidoItem> getItens() {
        return itens;
    }

    public void setItens(List<PedidoItem> itens) {
        this.itens = itens;
    }

    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    public void setCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }

    public String getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(String idTransacao) {
        this.idTransacao = idTransacao;
    }
}
