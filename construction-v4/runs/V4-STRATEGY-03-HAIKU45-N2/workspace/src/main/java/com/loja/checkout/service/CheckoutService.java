package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.validator.CheckoutValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {
    private final CheckoutValidator validator = new CheckoutValidator();

    public CheckoutResponse calcular(CheckoutRequest request) {
        String erroValidacao = validator.validar(request);
        if (erroValidacao != null) {
            CheckoutResponse response = new CheckoutResponse();
            response.setErro(erroValidacao);
            return response;
        }

        CheckoutResponse response = new CheckoutResponse();

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request);
        response.setSubtotalProdutos(arredondar(subtotalProdutos));

        BigDecimal descontoCupom = calcularDescontoCupom(request, subtotalProdutos);
        response.setDescontoCupom(arredondar(descontoCupom));

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        BigDecimal frete = calcularFrete(request, modalidade);
        response.setFrete(arredondar(frete));
        response.setPrazoEntregaDias(modalidade.getPrazo());

        BigDecimal seguro = calcularSeguro(request, subtotalProdutos);
        response.setSeguro(arredondar(seguro));

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal ajustePagamento = forma.calcularAjuste(totalPedido, parcelas);
        response.setAjustePagamento(arredondar(ajustePagamento));

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        response.setTotalFinal(arredondar(totalFinal));

        response.setParcelas(parcelas);
        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas, forma, request);
        response.setValorParcela(arredondar(valorParcela));

        BigDecimal credito = calcularCredito(request, subtotalProdutos);
        response.setCreditoProximaCompra(arredondar(credito));

        boolean temBrinde = temBrinde(request, subtotalProdutos);
        response.setBrinde(temBrinde);

        return response;
    }

    private BigDecimal calcularSubtotalProdutos(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : request.getItens()) {
            BigDecimal itemTotal = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }

    private BigDecimal calcularDescontoCupom(CheckoutRequest request, BigDecimal subtotalProdutos) {
        if (request.getCupom() == null) {
            return BigDecimal.ZERO;
        }

        Cupom cupom = Cupom.valueOf(request.getCupom());

        if (cupom == Cupom.FRETEGRATIS) {
            BigDecimal frete = calcularFreteSemGratis(request);
            return arredondar(frete);
        }

        if (cupom == Cupom.LEVE3PAGUE2) {
            return calcularDescontoLeve3Pague2(request);
        }

        return arredondar(cupom.calcularDesconto(subtotalProdutos));
    }

    private BigDecimal calcularFrete(CheckoutRequest request, ModalidadeEntrega modalidade) {
        NivelClube nivel = NivelClube.valueOf(request.getNivelClube());

        if (nivel.temFreteGratis()) {
            return BigDecimal.ZERO;
        }

        if (request.getCupom() != null && Cupom.valueOf(request.getCupom()) == Cupom.FRETEGRATIS) {
            return BigDecimal.ZERO;
        }

        BigDecimal pesoTotal = calcularPesoTotal(request);
        return modalidade.calcularFrete(pesoTotal);
    }

    private BigDecimal calcularFreteSemGratis(CheckoutRequest request) {
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        NivelClube nivel = NivelClube.valueOf(request.getNivelClube());

        if (nivel.temFreteGratis()) {
            return BigDecimal.ZERO;
        }

        BigDecimal pesoTotal = calcularPesoTotal(request);
        return modalidade.calcularFrete(pesoTotal);
    }

    private BigDecimal calcularSeguro(CheckoutRequest request, BigDecimal subtotalProdutos) {
        Regiao regiao = Regiao.valueOf(request.getRegiao());
        return subtotalProdutos.multiply(regiao.getPercentualSeguro());
    }

    private BigDecimal calcularCredito(CheckoutRequest request, BigDecimal subtotalProdutos) {
        NivelClube nivel = NivelClube.valueOf(request.getNivelClube());
        return subtotalProdutos.multiply(nivel.getPercentualCredito());
    }

    private boolean temBrinde(CheckoutRequest request, BigDecimal subtotalProdutos) {
        NivelClube nivel = NivelClube.valueOf(request.getNivelClube());
        return nivel == NivelClube.OURO && subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (Item item : request.getItens()) {
            BigDecimal itemPeso = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(itemPeso);
        }
        return pesoTotal;
    }

    private BigDecimal calcularDescontoLeve3Pague2(CheckoutRequest request) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : request.getItens()) {
            int quantidade = item.getQuantidade();
            int itensGratis = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(itensGratis));
            desconto = desconto.add(descontoItem);
        }

        return desconto;
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas, FormaPagamento forma, CheckoutRequest request) {
        if (parcelas == 1) {
            return totalFinal;
        }

        if (forma == FormaPagamento.CARTAO && parcelas <= 3) {
            return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        if (forma == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal totalPedido = calcularTotalPedido(request);
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal fatorParcela = BigDecimal.ONE.add(taxaMensal);
            BigDecimal fatorPowerN = fatorParcela.pow(parcelas);
            BigDecimal denominador = fatorPowerN.subtract(BigDecimal.ONE);
            BigDecimal parcelaComJuros = totalPedido.multiply(taxaMensal).multiply(fatorPowerN).divide(denominador, 2, RoundingMode.HALF_EVEN);
            return parcelaComJuros;
        }

        return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularTotalPedido(CheckoutRequest request) {
        BigDecimal subtotal = calcularSubtotalProdutos(request);
        BigDecimal desconto = calcularDescontoCupom(request, subtotal);
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        BigDecimal frete = calcularFrete(request, modalidade);
        BigDecimal seguro = calcularSeguro(request, subtotal);
        return subtotal.subtract(desconto).add(frete).add(seguro);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
