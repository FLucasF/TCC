package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.enums.FormaPagamento;
import com.loja.enums.ModalidadeEntrega;
import com.loja.util.ArredondadorMeioParaPar;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CheckoutService {

    private final ValidadorCheckout validador;

    public CheckoutService(ValidadorCheckout validador) {
        this.validador = validador;
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validador.validar(request);

        BigDecimal subtotal = CalculadorSubtotal.calcular(request.getItens());
        BigDecimal pesoTotal = CalculadorPeso.calcular(request.getItens());

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        validador.validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal frete = CalculadorFrete.calcular(modalidade, pesoTotal);
        BigDecimal desconto = CalculadorDesconto.calcular(request.getCupom(), subtotal, frete, request.getItens());

        BigDecimal totalPedido = subtotal
            .subtract(desconto)
            .add(frete);
        totalPedido = ArredondadorMeioParaPar.arredondar(totalPedido);

        FormaPagamento forma = FormaPagamento.fromString(request.getFormaPagamento());
        validador.validarFormaDisponivel(forma, totalPedido);

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        CalculadorAjustePagamento.ResultadoPagamento resultado =
            CalculadorAjustePagamento.calcular(forma, totalPedido, parcelas);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotal);
        response.setDescontoCupom(desconto);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazo());
        response.setAjustePagamento(resultado.ajustePagamento);
        response.setTotalFinal(resultado.totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(resultado.valorParcela);

        return response;
    }
}
