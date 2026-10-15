package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest request) {
        List<Item> itens = request.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(request.getModalidadeEntrega());
        NivelClube clube = NivelClube.fromString(request.getNivelClube());
        Regiao regiao = Regiao.fromString(request.getRegiao());
        Cupom cupom = Cupom.fromString(request.getCupom());
        FormaPagamento forma = FormaPagamento.fromString(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        CheckoutResponse response = new CheckoutResponse();

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        response.setSubtotalProdutos(Arredondador.arredondar(subtotalProdutos));

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (cupom != null) {
            BigDecimal frete = calcularFrete(modalidade, itens, clube);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, itens, frete);
        }
        response.setDescontoCupom(Arredondador.arredondar(descontoCupom));

        BigDecimal frete = calcularFrete(modalidade, itens, clube);
        response.setFrete(Arredondador.arredondar(frete));
        response.setPrazoEntregaDias(modalidade.getPrazoEntregaDias());

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        response.setSeguro(Arredondador.arredondar(seguro));

        BigDecimal total = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        total = Arredondador.arredondar(total);

        BigDecimal ajustePagamento = forma.calcularAjuste(total, parcelas);
        response.setAjustePagamento(Arredondador.arredondar(ajustePagamento));

        BigDecimal totalFinal = total.add(ajustePagamento);
        response.setTotalFinal(Arredondador.arredondar(totalFinal));

        response.setParcelas(parcelas);
        BigDecimal valorParcela = calcularValorParcela(total, totalFinal, parcelas, forma);
        response.setValorParcela(Arredondador.arredondar(valorParcela));

        BigDecimal credito = calcularCredito(subtotalProdutos, clube);
        response.setCreditoProximaCompra(Arredondador.arredondar(credito));

        boolean temBrinde = clube.temBrindePeloValor() && subtotalProdutos.compareTo(BigDecimal.valueOf(500)) > 0;
        response.setBrinde(temBrinde);

        return response;
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<Item> itens, NivelClube clube) {
        if (clube.isDeFrete()) {
            return BigDecimal.ZERO;
        }
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return modalidade.calcularFrete(pesoTotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        return subtotal.multiply(regiao.getPercentualSeguro());
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube clube) {
        return subtotal.multiply(clube.getPercentualCredito());
    }

    private BigDecimal calcularValorParcela(BigDecimal total, BigDecimal totalFinal, int parcelas, FormaPagamento forma) {
        if (parcelas <= 1) {
            return totalFinal;
        }

        if (forma == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
            BigDecimal base = BigDecimal.ONE.add(taxaMensal);
            BigDecimal basePow = base.pow(parcelas);
            BigDecimal basePowNeg = BigDecimal.ONE.divide(basePow, 20, java.math.RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(basePowNeg);
            return total.multiply(taxaMensal).divide(denominador, 20, java.math.RoundingMode.HALF_EVEN);
        }

        return totalFinal.divide(BigDecimal.valueOf(parcelas), 20, java.math.RoundingMode.HALF_EVEN);
    }
}
