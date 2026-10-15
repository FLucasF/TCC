package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinhoRequest;
import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.dto.ResumoCheckoutResponse;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.Entrega;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CalculadoraResumoService {

    public ResumoCheckoutResponse calcular(ResumoCheckoutRequest request) throws ValidacaoException {
        ValidadorPedidoService.validar(request);

        List<ItemCarrinhoRequest> itens = request.getItens();
        String cupomCodigo = request.getCupom();
        String modalidadeEntrega = request.getModalidadeEntrega();
        String formaPagamentoCodigo = request.getFormaPagamento();
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        double subtotal = calcularSubtotalProdutos(itens);
        double pesoTotal = calcularPesoTotal(itens);
        double desconto = calcularDescontoCupom(subtotal, itens, cupomCodigo);
        double frete = calcularFrete(modalidadeEntrega, pesoTotal);
        int prazo = obterPrazo(modalidadeEntrega);

        double freteFinal = "FRETEGRATIS".equals(cupomCodigo) ? 0.0 : frete;
        double descontoFrete = "FRETEGRATIS".equals(cupomCodigo) ? frete : 0.0;
        double descontoTotal = desconto + descontoFrete;

        double totalIntermediario = Arredondador.arredondar(subtotal - descontoTotal + freteFinal);

        FormaPagamento formaPagamento = FormaPagamento.obter(formaPagamentoCodigo);
        double ajuste = formaPagamento.calcularAjuste(totalIntermediario, parcelas);
        double totalFinal = Arredondador.arredondar(totalIntermediario + ajuste);

        double valorParcela = calcularValorParcela(totalFinal, parcelas);

        return new ResumoCheckoutResponse(
            subtotal, Arredondador.arredondar(descontoTotal), frete, prazo, ajuste, totalFinal, parcelas, valorParcela
        );
    }

    private double calcularSubtotalProdutos(List<ItemCarrinhoRequest> itens) {
        double subtotal = 0.0;

        for (ItemCarrinhoRequest item : itens) {
            double precoItem = item.getPrecoUnitario() * item.getQuantidade();
            subtotal += precoItem;
        }

        return Arredondador.arredondar(subtotal);
    }

    private double calcularPesoTotal(List<ItemCarrinhoRequest> itens) {
        double pesoTotal = 0.0;
        for (ItemCarrinhoRequest item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private double calcularDescontoCupom(double subtotal, List<ItemCarrinhoRequest> itens, String cupomCodigo) {
        if (cupomCodigo == null || "FRETEGRATIS".equals(cupomCodigo)) {
            return 0.0;
        }

        if ("LEVE3PAGUE2".equals(cupomCodigo)) {
            double desconto = 0.0;
            for (ItemCarrinhoRequest item : itens) {
                int unidadesGratis = item.getQuantidade() / 3;
                desconto += unidadesGratis * item.getPrecoUnitario();
            }
            return Arredondador.arredondar(desconto);
        }

        Cupom cupom = Cupom.obter(cupomCodigo);
        if (cupom == null) {
            return 0.0;
        }

        return Arredondador.arredondar(cupom.calcularDesconto(subtotal));
    }

    private double calcularFrete(String modalidadeEntrega, double pesoTotal) {
        Entrega entrega = Entrega.obter(modalidadeEntrega);
        if (entrega == null) {
            return 0.0;
        }
        return Arredondador.arredondar(entrega.calcularFrete(pesoTotal));
    }

    private int obterPrazo(String modalidadeEntrega) {
        Entrega entrega = Entrega.obter(modalidadeEntrega);
        if (entrega == null) {
            return 0;
        }
        return entrega.getPrazo();
    }

    private double calcularValorParcela(double totalFinal, int parcelas) {
        if (parcelas <= 0) {
            parcelas = 1;
        }
        return Arredondador.arredondar(totalFinal / parcelas);
    }

}
