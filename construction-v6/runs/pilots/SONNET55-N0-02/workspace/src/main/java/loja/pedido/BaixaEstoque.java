package loja.pedido;

import java.util.Comparator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import loja.dominio.ItemPedido;
import loja.dominio.NegocioException;
import loja.dominio.Pedido;
import loja.infra.ProdutoRepository;

@Component
@Order(1)
public class BaixaEstoque implements AcaoPedido {

    private final ProdutoRepository produtos;

    public BaixaEstoque(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    @Override
    public void aoPagar(Pedido pedido) {
        // ordem fixa por SKU evita travamento mútuo entre pagamentos simultâneos
        for (ItemPedido i : pedido.itens().stream().sorted(Comparator.comparing(ItemPedido::sku)).toList()) {
            if (!produtos.baixarEstoque(i.sku(), i.quantidade())) {
                throw NegocioException.conflito("ESTOQUE_INSUFICIENTE");
            }
        }
    }

    @Override
    public void aoCancelar(Pedido pedido, boolean estavaPago) {
        if (estavaPago) {
            pedido.itens().forEach(i -> produtos.reporEstoque(i.sku(), i.quantidade()));
        }
    }
}
