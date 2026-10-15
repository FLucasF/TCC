package br.com.loja.pedidos.aviso;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Aviso(long id, TipoAviso tipo, long pedidoId, SituacaoAviso situacao, @JsonIgnore String cpf) {
}
