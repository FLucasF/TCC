package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomFactory;
import com.loja.checkout.domain.cupom.ResultadoCupom;
import com.loja.checkout.domain.modalidade.ModalidadeEntrega;
import com.loja.checkout.domain.modalidade.ModalidadeFactory;
import com.loja.checkout.domain.modalidade.ResultadoEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoFactory;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.validation.PedidoValidator;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        PedidoValidator.validarItens(pedido);
        NivelClube nivelClube = PedidoValidator.validarNivelClube(pedido.nivelClube());
        Regiao regiao = PedidoValidator.validarRegiao(pedido.regiao());

        double subtotalProdutos = calcularSubtotalProdutos(pedido);
        double pesoTotal = calcularPesoTotal(pedido);

        ModalidadeEntrega modalidade = ModalidadeFactory.criar(pedido.modalidadeEntrega());
        if (!modalidade.aceita(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        ResultadoEntrega entrega = modalidade.calcular(pesoTotal);
        double frete = nivelClube.isentaFrete() ? 0.0 : entrega.valor();
        int prazo = entrega.prazo();

        double descontoCupom = 0.0;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            Cupom cupom = CupomFactory.criar(pedido.cupom());
            if (!cupom.aplicavel(subtotalProdutos, pedido.itens())) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            ResultadoCupom resultadoCupom = cupom.calcular(subtotalProdutos, entrega.valor(), pedido.itens());
            descontoCupom = resultadoCupom.desconto();
            if (resultadoCupom.freteOriginal() > 0) {
                frete = entrega.valor();
            }
        }

        double seguro = regiao.calcularSeguro(subtotalProdutos);
        double totalPedido = arredondar(subtotalProdutos - descontoCupom + frete + seguro);

        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        double creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazo,
            seguro,
            resultadoPagamento.ajuste(),
            resultadoPagamento.totalFinal(),
            parcelas,
            resultadoPagamento.valorParcela(),
            creditoProximaCompra,
            brinde
        );
    }

    private double calcularSubtotalProdutos(PedidoRequest pedido) {
        double subtotal = 0.0;
        for (ItemPedido item : pedido.itens()) {
            subtotal += item.precoUnitario() * item.quantidade();
        }
        return arredondar(subtotal);
    }

    private double calcularPesoTotal(PedidoRequest pedido) {
        double peso = 0.0;
        for (ItemPedido item : pedido.itens()) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
