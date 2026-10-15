package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.*;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest request) {
        validarPedido(request.itens());

        NivelClube nivelClube = NivelClube.fromString(request.nivelClube());
        Regiao regiao = Regiao.fromString(request.regiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(request.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        modalidade.validarDisponibilidade(pesoTotal);

        BigDecimal frete = nivelClube.isentaFrete()
            ? BigDecimal.ZERO.setScale(2)
            : modalidade.calcularFrete(pesoTotal);

        Cupom cupom = Cupom.fromString(request.cupom());
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            cupom.validarAplicabilidade(subtotalProdutos, request.itens(), frete);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, request.itens(), frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro)
            .setScale(2, java.math.RoundingMode.HALF_EVEN);

        FormaPagamento formaPagamento = FormaPagamento.fromString(request.formaPagamento());
        formaPagamento.validarParcelas(request.parcelas());
        formaPagamento.validarDisponibilidade(totalPedido);

        int parcelas = formaPagamento.getParcelas(request.parcelas());
        BigDecimal totalFinal = formaPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalFinal, parcelas);

        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoEntregaDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private void validarPedido(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal valorItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(item.quantidade()))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(valorItem);
        }
        return subtotal.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal pesoItem = item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }
}
