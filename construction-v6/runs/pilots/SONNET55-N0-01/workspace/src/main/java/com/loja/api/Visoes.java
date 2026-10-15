package com.loja.api;

import java.math.BigDecimal;
import java.util.List;

import com.loja.dominio.Aviso;
import com.loja.dominio.Cliente;
import com.loja.dominio.Item;
import com.loja.dominio.Pedido;
import com.loja.dominio.Produto;
import com.loja.dominio.Resumo;

/** Formato das respostas da API. */
final class Visoes {

    private Visoes() {
    }

    record PedidoVisao(String id, String situacao, String criadoEm, Resumo resumo, List<Item> itens,
                       String codigoRastreio) {
        static PedidoVisao de(Pedido p) {
            return new PedidoVisao(p.id(), p.situacao().name(), p.criadoEm().toString(), p.resumo(), p.itens(),
                    p.codigoRastreio());
        }
    }

    record ProdutoVisao(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque,
                        boolean foraDeLinha) {
        static ProdutoVisao de(Produto p) {
            return new ProdutoVisao(p.sku(), p.nome(), p.preco(), p.pesoKg(), p.estoque(), p.foraDeLinha());
        }
    }

    record ClienteVisao(String cpf, BigDecimal saldoCredito) {
        static ClienteVisao de(Cliente c) {
            return new ClienteVisao(c.cpf(), c.saldoCredito());
        }
    }

    record AvisoVisao(String id, String tipo, String pedidoId, String situacao) {
        static AvisoVisao de(Aviso a) {
            return new AvisoVisao(a.id(), a.tipo().name(), a.pedidoId(), a.lido() ? "LIDO" : "NOVO");
        }
    }
}
