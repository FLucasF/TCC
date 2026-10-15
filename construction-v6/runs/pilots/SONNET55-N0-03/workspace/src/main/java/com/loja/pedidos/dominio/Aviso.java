package com.loja.pedidos.dominio;

public record Aviso(String id, TipoAviso tipo, String pedidoId, String situacao) {
}
