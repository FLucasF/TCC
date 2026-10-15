package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public class PedidoResponseDTO {
    @JsonProperty("id")
    public Long id;

    @JsonProperty("situacao")
    public String situacao;

    @JsonProperty("criadoEm")
    public Instant criadoEm;

    @JsonProperty("resumo")
    public JsonNode resumo;

    @JsonProperty("itens")
    public List<ItemDTO> itens;

    @JsonProperty("codigoRastreio")
    public String codigoRastreio;

    public PedidoResponseDTO() {}

    public PedidoResponseDTO(Long id, String situacao, Instant criadoEm, JsonNode resumo, List<ItemDTO> itens, String codigoRastreio) {
        this.id = id;
        this.situacao = situacao;
        this.criadoEm = criadoEm;
        this.resumo = resumo;
        this.itens = itens;
        this.codigoRastreio = codigoRastreio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<ItemDTO> getItens() {
        return itens;
    }

    public void setItens(List<ItemDTO> itens) {
        this.itens = itens;
    }

    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    public void setCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }
}
