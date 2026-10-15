package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.PedidoRecusado;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A ordem do cálculo e a ordem em que o pedido é recusado: é o que não muda de
 * caso para caso. Quanto custa cada parte fica em cada opção de entrega, cupom,
 * nível do clube, região e forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(PedidoRecebido pedido) {
        Carrinho carrinho = pedido.carrinho();
        NivelClube clube = Resolucao.obrigatoria(
                NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Resolucao.obrigatoria(
                Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega entrega = Resolucao.obrigatoria(
                ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoKg = carrinho.pesoKg();
        if (!entrega.atende(pesoKg)) {
            throw new PedidoRecusado(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal frete = clube.freteDevido(entrega.frete(pesoKg));
        Optional<Cupom> cupom = Resolucao.opcional(
                Cupom.class, pedido.cupom(), CodigoErro.CUPOM_INVALIDO);
        ContextoCupom contexto = new ContextoCupom(carrinho.itens(), subtotalProdutos, frete);
        BigDecimal descontoCupom = cupom
                .map(aplicado -> aplicado.desconto(contexto))
                .orElse(Dinheiro.ZERO);

        FormaPagamento pagamento = Resolucao.obrigatoria(
                FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelasEscolhidas();
        if (!pagamento.permiteParcelas(parcelas)) {
            throw new PedidoRecusado(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusado(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.resultado(totalPedido, parcelas);
        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                Dinheiro.arredondar(frete),
                entrega.prazoDias(),
                seguro,
                resultado.ajusteSobre(totalPedido),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                clube.creditoProximaCompra(subtotalProdutos),
                clube.brinde(subtotalProdutos));
    }
}
