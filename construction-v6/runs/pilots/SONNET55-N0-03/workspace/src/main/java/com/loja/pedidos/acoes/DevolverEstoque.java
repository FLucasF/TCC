package com.loja.pedidos.acoes;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.persistencia.ProdutoRepositorio;

@Component
public class DevolverEstoque implements AcaoCancelamento {

    private final ProdutoRepositorio produtos;

    public DevolverEstoque(ProdutoRepositorio produtos) {
        this.produtos = produtos;
    }

    @Override
    public void executar(Pedido pedido, boolean estavaPago) {
        if (estavaPago) {
            produtos.devolverEstoque(pedido.itens());
        }
    }
}
