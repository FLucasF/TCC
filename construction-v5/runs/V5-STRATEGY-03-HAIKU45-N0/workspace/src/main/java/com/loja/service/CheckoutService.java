package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.Item;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal SCALE_2 = new BigDecimal("0.01");

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        // Validação 1: Pedido inválido
        if (!validarPedido(request)) {
            return new CheckoutResponse("PEDIDO_INVALIDO");
        }

        // Validação 2: Nível do clube
        if (!validarNivelClube(request.getNivelClube())) {
            return new CheckoutResponse("NIVEL_CLUBE_INVALIDO");
        }

        // Validação 3: Região
        if (!validarRegiao(request.getRegiao())) {
            return new CheckoutResponse("REGIAO_INVALIDA");
        }

        // Validação 4 e 5: Modalidade de entrega
        if (!validarModalidadeEntrega(request.getModalidadeEntrega())) {
            return new CheckoutResponse("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        if (!validarModalidadeDisponivel(request.getModalidadeEntrega(), pesoTotal)) {
            return new CheckoutResponse("MODALIDADE_INDISPONIVEL");
        }

        // Validação 6 e 7: Cupom
        String cupom = request.getCupom();
        if (cupom != null && !cupom.isEmpty()) {
            if (!validarCupom(cupom)) {
                return new CheckoutResponse("CUPOM_INVALIDO");
            }

            BigDecimal subtotalProdutos = calcularSubtotalProdutosNormal(request.getItens());
            if (!validarAplicabilidadeCupom(cupom, subtotalProdutos)) {
                return new CheckoutResponse("CUPOM_NAO_APLICAVEL");
            }
        }

        // Validação 8 e 9 e 10: Forma de pagamento
        if (!validarFormaPagamento(request.getFormaPagamento())) {
            return new CheckoutResponse("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!validarParcelamento(request.getFormaPagamento(), parcelas)) {
            return new CheckoutResponse("PARCELAMENTO_INVALIDO");
        }

        // Cálculo do resumo
        CheckoutResponse response = new CheckoutResponse();

        // 1. Subtotal dos produtos
        BigDecimal subtotalProdutos = calcularSubtotalProdutosNormal(request.getItens());
        response.setSubtotalProdutos(arredondar(subtotalProdutos));

        // 3. Frete (calculado antes do desconto do cupom para FRETEGRATIS)
        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), pesoTotal, request.getNivelClube());
        response.setFrete(arredondar(frete));
        response.setPrazoEntregaDias(obterPrazoEntrega(request.getModalidadeEntrega()));

        // 2. Desconto do cupom
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, request.getItens(), subtotalProdutos, frete);
        response.setDescontoCupom(arredondar(descontoCupom));

        // 4. Seguro
        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());
        response.setSeguro(arredondar(seguro));

        // 5. Total do pedido (antes do ajuste de pagamento)
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
        totalPedido = arredondar(totalPedido);

        // Validação 10: Forma de pagamento disponível para o valor
        if (!validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalPedido)) {
            return new CheckoutResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // 6. Ajuste de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(request.getFormaPagamento(), totalPedido, parcelas);
        response.setAjustePagamento(arredondar(ajustePagamento));

        // Total final
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = arredondar(totalFinal);
        response.setTotalFinal(totalFinal);

        // Parcelamento
        response.setParcelas(parcelas);
        BigDecimal valorParcela = calcularValorParcela(totalFinal, request.getFormaPagamento(), parcelas);
        response.setValorParcela(arredondar(valorParcela));

        // Crédito da próxima compra
        BigDecimal credito = calcularCreditoClube(subtotalProdutos, request.getNivelClube());
        response.setCreditoProximaCompra(arredondar(credito));

        // Brinde
        boolean brinde = verificarBrinde(subtotalProdutos, request.getNivelClube());
        response.setBrinde(brinde);

        return response;
    }

    private boolean validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return false;
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return false;
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
        }

        return true;
    }

    private boolean validarNivelClube(String nivelClube) {
        return nivelClube != null && (nivelClube.equals("BRONZE") || nivelClube.equals("PRATA") || nivelClube.equals("OURO"));
    }

    private boolean validarRegiao(String regiao) {
        return regiao != null && (regiao.equals("SUDESTE") || regiao.equals("SUL") || regiao.equals("CENTRO_OESTE") || regiao.equals("NORTE") || regiao.equals("NORDESTE"));
    }

    private boolean validarModalidadeEntrega(String modalidade) {
        return modalidade != null && (modalidade.equals("ECONOMICA") || modalidade.equals("EXPRESSA") || modalidade.equals("RETIRADA_LOJA") || modalidade.equals("MOTOBOY"));
    }

    private boolean validarModalidadeDisponivel(String modalidade, BigDecimal pesoTotal) {
        if (modalidade.equals("MOTOBOY")) {
            return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
        }
        return true;
    }

    private boolean validarCupom(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") || cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private boolean validarAplicabilidadeCupom(String cupom, BigDecimal subtotalProdutos) {
        if (cupom.equals("MENOS50")) {
            return subtotalProdutos.compareTo(new BigDecimal("300")) >= 0;
        }
        return true;
    }

    private boolean validarFormaPagamento(String formaPagamento) {
        return formaPagamento != null && (formaPagamento.equals("PIX") || formaPagamento.equals("CARTAO") || formaPagamento.equals("BOLETO"));
    }

    private boolean validarParcelamento(String formaPagamento, Integer parcelas) {
        if (formaPagamento.equals("PIX") || formaPagamento.equals("BOLETO")) {
            return parcelas == 1;
        }
        if (formaPagamento.equals("CARTAO")) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean validarFormaPagamentoDisponivel(String formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento.equals("BOLETO")) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }
        return true;
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private BigDecimal calcularSubtotalProdutosNormal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return subtotal;
    }

    private BigDecimal calcularDescontoCupom(String cupom, List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (cupom.equals("BEMVINDO10")) {
            return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }

        if (cupom.equals("MENOS50")) {
            return new BigDecimal("50.00");
        }

        if (cupom.equals("FRETEGRATIS")) {
            return frete;
        }

        if (cupom.equals("LEVE3PAGUE2")) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : itens) {
                int quantidadeGratuita = item.getQuantidade() / 3;
                BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratuita));
                desconto = desconto.add(descontoItem);
            }
            return arredondar(desconto);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal pesoTotal, String nivelClube) {
        if (nivelClube.equals("OURO")) {
            return BigDecimal.ZERO;
        }

        if (modalidade.equals("ECONOMICA")) {
            BigDecimal baseEconomica = new BigDecimal("12.00");
            BigDecimal pesoEconomica = pesoTotal.multiply(new BigDecimal("2.00"));
            return arredondar(baseEconomica.add(pesoEconomica));
        }

        if (modalidade.equals("EXPRESSA")) {
            BigDecimal baseExpressa = new BigDecimal("25.00");
            BigDecimal pesoExpressa = pesoTotal.multiply(new BigDecimal("4.50"));
            return arredondar(baseExpressa.add(pesoExpressa));
        }

        if (modalidade.equals("RETIRADA_LOJA")) {
            return BigDecimal.ZERO;
        }

        if (modalidade.equals("MOTOBOY")) {
            return new BigDecimal("18.00");
        }

        return BigDecimal.ZERO;
    }

    private Integer obterPrazoEntrega(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> 0;
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, String regiao) {
        BigDecimal percentualSeguro = switch (regiao) {
            case "SUDESTE" -> new BigDecimal("0.01");
            case "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> BigDecimal.ZERO;
        };

        return arredondar(subtotalProdutos.multiply(percentualSeguro));
    }

    private BigDecimal calcularAjustePagamento(String formaPagamento, BigDecimal totalPedido, Integer parcelas) {
        if (formaPagamento.equals("PIX")) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
            return arredondar(desconto).negate();
        }

        if (formaPagamento.equals("BOLETO")) {
            return new BigDecimal("3.49");
        }

        if (formaPagamento.equals("CARTAO")) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO;
            }

            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
            BigDecimal umMaisTaxaAlaN = umMaisTaxa.pow(parcelas);

            BigDecimal denominador = umMaisTaxaAlaN.subtract(BigDecimal.ONE);
            BigDecimal numerador = taxaMensal.multiply(umMaisTaxaAlaN);

            BigDecimal valorParcela = totalPedido.multiply(numerador).divide(denominador, 20, RoundingMode.HALF_EVEN);
            valorParcela = arredondar(valorParcela);

            BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
            totalComJuros = arredondar(totalComJuros);

            return arredondar(totalComJuros.subtract(totalPedido));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, String formaPagamento, Integer parcelas) {
        if (parcelas == null || parcelas <= 1) {
            return totalFinal;
        }

        return arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularCreditoClube(BigDecimal subtotalProdutos, String nivelClube) {
        if (nivelClube.equals("BRONZE")) {
            return BigDecimal.ZERO;
        }

        if (nivelClube.equals("PRATA")) {
            return arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }

        if (nivelClube.equals("OURO")) {
            return arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }

        return BigDecimal.ZERO;
    }

    private boolean verificarBrinde(BigDecimal subtotalProdutos, String nivelClube) {
        return nivelClube.equals("OURO") && subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
