package com.loja.checkout.service;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.ResumoResponse;
import com.loja.checkout.strategy.*;
import com.loja.checkout.util.ArredondadorMeio;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotal(pedido);
        NivelClube nivelClube = NivelClubeFactory.criar(pedido.getNivelClube());
        Entrega entrega = EntregaFactory.criar(pedido.getModalidadeEntrega());
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.getFormaPagamento());
        Regiao regiao = Regiao.fromString(pedido.getRegiao());

        BigDecimal descontoCupom = ArredondadorMeio.zero();
        boolean ehFreteGratis = false;

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            Cupom cupom = CupomFactory.criar(pedido.getCupom());
            if (cupom != null && cupom.ehAplicavel(subtotalProdutos)) {
                descontoCupom = cupom.calcularDesconto(subtotalProdutos, pedido.getItens());
                ehFreteGratis = cupom.ofereceFretGratis();
            }
        }

        descontoCupom = ArredondadorMeio.arredondar(descontoCupom);

        Double pesoTotal = calcularPesoTotal(pedido);
        BigDecimal frete = ArredondadorMeio.zero();

        if (!ehFreteGratis && !nivelClube.temFreteGratis()) {
            frete = ArredondadorMeio.arredondar(entrega.calcularCusto(pesoTotal));
        }

        BigDecimal seguro = ArredondadorMeio.arredondar(
            subtotalProdutos.multiply(regiao.getPercentualSeguro())
        );

        BigDecimal totalAntesAjuste = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalAntesAjuste = ArredondadorMeio.arredondar(totalAntesAjuste);

        Integer parcelasEfetivas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalAntesAjuste, parcelasEfetivas);
        ajustePagamento = ArredondadorMeio.arredondar(ajustePagamento);

        BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);
        totalFinal = ArredondadorMeio.arredondar(totalFinal);

        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalAntesAjuste, parcelasEfetivas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);

        Boolean temBrinde = nivelClube.temBrinde(subtotalProdutos);

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(entrega.prazo());
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(ajustePagamento);
        resposta.setTotalFinal(totalFinal);
        resposta.setParcelas(parcelasEfetivas);
        resposta.setValorParcela(valorParcela);
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(temBrinde);

        return resposta;
    }

    private BigDecimal calcularSubtotal(PedidoRequest pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : pedido.getItens()) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return ArredondadorMeio.arredondar(subtotal);
    }

    private Double calcularPesoTotal(PedidoRequest pedido) {
        double pesoTotal = 0;
        for (ItemRequest item : pedido.getItens()) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ErroCheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ErroCheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (pedido.getNivelClube() == null || pedido.getNivelClube().isEmpty()) {
            throw new ErroCheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (NivelClubeFactory.criar(pedido.getNivelClube()) == null) {
            throw new ErroCheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (pedido.getRegiao() == null || pedido.getRegiao().isEmpty()) {
            throw new ErroCheckoutException("REGIAO_INVALIDA");
        }

        if (Regiao.fromString(pedido.getRegiao()) == null) {
            throw new ErroCheckoutException("REGIAO_INVALIDA");
        }

        if (pedido.getModalidadeEntrega() == null || pedido.getModalidadeEntrega().isEmpty()) {
            throw new ErroCheckoutException("MODALIDADE_INVALIDA");
        }

        Entrega entrega = EntregaFactory.criar(pedido.getModalidadeEntrega());
        if (entrega == null) {
            throw new ErroCheckoutException("MODALIDADE_INVALIDA");
        }

        Double pesoTotal = calcularPesoTotal(pedido);
        if (!entrega.podeAtender(pesoTotal)) {
            throw new ErroCheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            Cupom cupom = CupomFactory.criar(pedido.getCupom());
            if (cupom == null) {
                throw new ErroCheckoutException("CUPOM_INVALIDO");
            }

            BigDecimal subtotalProdutos = calcularSubtotal(pedido);
            if (!cupom.ehAplicavel(subtotalProdutos)) {
                throw new ErroCheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        if (pedido.getFormaPagamento() == null || pedido.getFormaPagamento().isEmpty()) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.getFormaPagamento());
        if (formaPagamento == null) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
        if (!formaPagamento.parcelaPermitida(parcelas)) {
            throw new ErroCheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(pedido);
        BigDecimal totalEstimado = subtotalProdutos;
        if (!formaPagamento.podeAtender(totalEstimado)) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

}
