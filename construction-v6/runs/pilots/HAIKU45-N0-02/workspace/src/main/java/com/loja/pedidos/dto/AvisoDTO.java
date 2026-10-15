package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AvisoDTO {
    @JsonProperty("id")
    public Long id;

    @JsonProperty("tipo")
    public String tipo;

    @JsonProperty("pedidoId")
    public Long pedidoId;

    @JsonProperty("situacao")
    public String situacao;

    public AvisoDTO() {}

    public AvisoDTO(Long id, String tipo, Long pedidoId, String situacao) {
        this.id = id;
        this.tipo = tipo;
        this.pedidoId = pedidoId;
        this.situacao = situacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
