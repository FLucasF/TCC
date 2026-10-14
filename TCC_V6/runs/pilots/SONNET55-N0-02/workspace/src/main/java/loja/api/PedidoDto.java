package loja.api;

import java.util.List;

import loja.dominio.ItemPedido;
import loja.dominio.Pedido;
import loja.dominio.Resumo;

public record PedidoDto(String id, String situacao, String criadoEm, Resumo resumo, List<ItemPedido> itens,
        String codigoRastreio) {

    public static PedidoDto de(Pedido p) {
        return new PedidoDto(p.id(), p.situacao().name(), p.criadoEm().toString(), p.resumo(), p.itens(),
                p.codigoRastreio());
    }
}
