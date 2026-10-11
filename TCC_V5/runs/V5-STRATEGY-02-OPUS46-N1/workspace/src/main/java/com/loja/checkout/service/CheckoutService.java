package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarItens(request);

        NivelClube nivelClube = NivelClube.buscar(request.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = Regiao.buscar(request.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        ModalidadeEntrega modalidade = ModalidadeEntrega.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        BigDecimal subtotalProdutos = calcularSubtotal(request);
        BigDecimal pesoTotal = calcularPesoTotal(request);

        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteBruto = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.temFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteBruto;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = Cupom.buscar(request.cupom())
                    .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
            if (!cupom.isAplicavel(subtotalProdutos, request.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, request.itens(), frete);
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        FormaPagamento formaPagamento = FormaPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        if (!formaPagamento.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getTaxaSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultado.totalFinal().subtract(totalPedido);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajustePagamento,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivelClube.calcularCredito(subtotalProdutos),
                nivelClube.temBrinde(subtotalProdutos)
        );
    }

    private void validarItens(CheckoutRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : request.itens()) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : request.itens()) {
            peso = peso.add(
                    item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade()))
            );
        }
        return peso;
    }
}
