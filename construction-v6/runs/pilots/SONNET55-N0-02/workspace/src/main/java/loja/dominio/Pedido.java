package loja.dominio;

import java.time.Instant;
import java.util.List;

public record Pedido(
        String id,
        String cpf,
        String email,
        Situacao situacao,
        Instant criadoEm,
        Resumo resumo,
        List<ItemPedido> itens,
        String codigoRastreio,
        String idTransacao) {
}
