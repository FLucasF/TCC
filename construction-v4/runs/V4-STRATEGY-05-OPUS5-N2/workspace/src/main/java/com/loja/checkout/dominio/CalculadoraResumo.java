package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.Clube;
import com.loja.checkout.dominio.clube.Vantagens;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.Entrega;
import com.loja.checkout.dominio.entrega.Frete;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.Pagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monta o resumo da compra. As etapas e a ordem delas sao as mesmas para qualquer
 * pedido; o que cada opcao escolhida faz em cada etapa fica na propria opcao.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(Pedido pedido) {
        List<Item> itens = pedido.itensValidos();
        Clube clube = Clube.CATALOGO.exigir(pedido.nivelClube());
        Regiao regiao = Regiao.CATALOGO.exigir(pedido.regiao());
        Entrega entrega = Entrega.CATALOGO.exigir(pedido.modalidadeEntrega());

        BigDecimal pesoKg = pedido.pesoKg();
        if (!entrega.atende(pesoKg)) {
            throw new PedidoRecusado(Erro.MODALIDADE_INDISPONIVEL);
        }
        Frete frete = entrega.calcular(pesoKg);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        Vantagens vantagens = clube.vantagens(subtotalProdutos, frete.valor());
        BigDecimal freteDevido = vantagens.freteDevido();

        Cupom cupom = cupomEscolhido(pedido);
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, freteDevido);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusado(Erro.CUPOM_NAO_APLICAVEL);
        }
        BigDecimal descontoCupom = cupom.desconto(contexto);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.emCentavos(
                subtotalProdutos.subtract(descontoCupom).add(freteDevido).add(seguro));

        FormaPagamento formaPagamento = FormaPagamento.CATALOGO.exigir(pedido.formaPagamento());
        int parcelas = pedido.parcelasEscolhidas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusado(Erro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusado(Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new Resumo(
                subtotalProdutos,
                descontoCupom,
                freteDevido,
                frete.prazoDias(),
                seguro,
                Dinheiro.emCentavos(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                vantagens.creditoProximaCompra(),
                vantagens.brinde());
    }

    private Cupom cupomEscolhido(Pedido pedido) {
        String codigo = pedido.cupom();
        return codigo == null || codigo.isBlank() ? Cupom.NENHUM : Cupom.CATALOGO.exigir(codigo);
    }
}
