package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.model.FormaPagamento;
import com.loja.model.ItemCarrinho;
import com.loja.model.ModalidadeEntrega;
import com.loja.model.NivelClube;
import com.loja.model.Regiao;
import com.loja.model.cupom.CalculadoraDesconto;
import com.loja.model.cupom.CupomRegistry;
import com.loja.service.estrategia.AjustePagamento;
import com.loja.service.estrategia.AjusteBoletoPagamento;
import com.loja.service.estrategia.AjusteCartaoPagamento;
import com.loja.service.estrategia.AjustePixPagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CalculadoraCheckout {
    public CheckoutResponse calcular(CheckoutRequest request) {
        List<ItemCarrinho> itens = converterItens(request.getItens());
        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        NivelClube nivelClube = NivelClube.valueOf(request.getNivelClube());
        Regiao regiao = Regiao.valueOf(request.getRegiao());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.getFormaPagamento());

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        subtotalProdutos = Arredondador.arredondar(subtotalProdutos);

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, itens);
        descontoCupom = Arredondador.arredondar(descontoCupom);

        BigDecimal frete = calcularFrete(modalidadeEntrega, itens, nivelClube, request.getCupom());
        frete = Arredondador.arredondar(frete);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        seguro = Arredondador.arredondar(seguro);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int parcelas = obterParcelas(request.getParcelas(), formaPagamento);
        AjustePagamento ajustadorPagamento = obterAjustadorPagamento(formaPagamento);
        BigDecimal ajustePagamento = ajustadorPagamento.calcularAjuste(totalPedido, parcelas);
        ajustePagamento = Arredondador.arredondar(ajustePagamento);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = Arredondador.arredondar(totalFinal);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas);
        valorParcela = Arredondador.arredondar(valorParcela);

        BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);
        creditoProximaCompra = Arredondador.arredondar(creditoProximaCompra);

        boolean brinde = deveBrinde(nivelClube, subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidadeEntrega.getPrazo());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private List<ItemCarrinho> converterItens(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> new ItemCarrinho(i.getNome(), i.getPrecoUnitario(), i.getQuantidade(), i.getPesoKg()))
                .collect(Collectors.toList());
    }

    public static BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        return itens.stream()
                .map(ItemCarrinho::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemCarrinho> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CalculadoraDesconto calculadora = CupomRegistry.obter(cupom).orElse(null);
        if (calculadora == null) {
            return BigDecimal.ZERO;
        }

        return calculadora.calcular(subtotal, itens);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemCarrinho> itens,
                                     NivelClube nivelClube, String cupom) {
        if (nivelClube.isOuro()) {
            return BigDecimal.ZERO;
        }

        if (cupom != null && !cupom.isEmpty()) {
            CalculadoraDesconto calculadora = CupomRegistry.obter(cupom).orElse(null);
            if (calculadora != null && calculadora.ehFretegratis()) {
                return BigDecimal.ZERO;
            }
        }

        BigDecimal pesoTotal = itens.stream()
                .map(ItemCarrinho::getPesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return modalidade.calculateCost(pesoTotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        return subtotal.multiply(regiao.getSeguroPercent());
    }

    private int obterParcelas(Integer parcelasRequest, FormaPagamento formaPagamento) {
        AjustePagamento ajustador = obterAjustadorPagamento(formaPagamento);
        return ajustador.obterParcelas(parcelasRequest);
    }

    private AjustePagamento obterAjustadorPagamento(FormaPagamento formaPagamento) {
        return switch (formaPagamento) {
            case PIX -> new AjustePixPagamento();
            case BOLETO -> new AjusteBoletoPagamento();
            case CARTAO -> new AjusteCartaoPagamento();
        };
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotal) {
        if (!nivelClube.isMemberWithCredit()) {
            return BigDecimal.ZERO;
        }

        return subtotal.multiply(nivelClube.getCreditoPercent());
    }

    private boolean deveBrinde(NivelClube nivelClube, BigDecimal subtotal) {
        if (!nivelClube.isOuro()) {
            return false;
        }

        return subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }
}
