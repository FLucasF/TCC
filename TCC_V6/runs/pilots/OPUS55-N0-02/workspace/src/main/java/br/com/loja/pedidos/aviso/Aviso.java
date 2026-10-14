package br.com.loja.pedidos.aviso;

public record Aviso(long id, String cpf, TipoAviso tipo, long pedidoId, SituacaoAviso situacao) {
}
