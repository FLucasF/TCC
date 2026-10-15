package com.loja.checkout.resumo;

import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.pedido.ItemPedido;
import com.loja.checkout.pedido.Pedido;
import com.loja.checkout.pedido.Regiao;
import com.loja.checkout.resumo.SolicitacaoResumo.ItemSolicitado;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Confere o carrinho, o nivel do clube e a regiao, nessa ordem. */
@Component
public class ValidadorPedido {

    private final CatalogoNiveisClube niveisClube;

    public ValidadorPedido(CatalogoNiveisClube niveisClube) {
        this.niveisClube = niveisClube;
    }

    public Pedido validar(SolicitacaoResumo solicitacao) {
        List<ItemPedido> itens = validarItens(solicitacao.itens());
        return new Pedido(itens, niveisClube.buscar(solicitacao.nivelClube()),
                Regiao.de(solicitacao.regiao()));
    }

    private List<ItemPedido> validarItens(List<ItemSolicitado> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private ItemPedido validarItem(ItemSolicitado item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
