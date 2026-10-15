package com.loja.roupas.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final Map<String, Integer> DELIVERY_DAYS = Map.of(
        "ECONOMICA", 7,
        "EXPRESSA", 2,
        "RETIRADA_LOJA", 1,
        "MOTOBOY", 0
    );

    private static final Map<String, String> REGION_TAX_MAP = Map.of(
        "SUDESTE", "12",
        "SUL", "11",
        "CENTRO_OESTE", "9",
        "NORTE", "7",
        "NORDESTE", "7"
    );

    public CheckoutResponse calcularResumo(CheckoutRequest request) throws Exception {
        validarRequisicao(request);

        List<ItemRequest> itens = request.getItens();
        String modalidade = request.getModalidadeEntrega();
        String cupom = request.getCupom();
        String formaPagamento = request.getFormaPagamento();
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        String nivelClube = request.getNivelClube();
        String regiao = request.getRegiao();

        // 1. Subtotal dos produtos
        BigDecimal subtotal = calcularSubtotal(itens);

        // 3. Frete (precisa ser calculado antes do cupom para FRETEGRATIS)
        double pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);
        int prazoEntrega = DELIVERY_DAYS.get(modalidade);

        // 2. Desconto do cupom (precisa saber o frete para FRETEGRATIS)
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotal, itens, frete);

        // 4. Imposto (sobre produtos com desconto)
        BigDecimal imposto = ZERO.setScale(2, RoundingMode.HALF_EVEN);
        if (regiao != null) {
            BigDecimal impostoPercentual = new BigDecimal(REGION_TAX_MAP.get(regiao));
            imposto = calcularImposto(subtotal, descontoCupom, impostoPercentual);
        }

        // 5. Total antes do ajuste de pagamento
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(imposto);

        // 6, 7. Calcular parcela, ajuste e total final
        BigDecimal totalFinal;
        BigDecimal valorParcela;
        BigDecimal ajustePagamento;

        if (formaPagamento.equals("PIX")) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            totalFinal = totalPedido.subtract(desconto);
            valorParcela = totalFinal;
            ajustePagamento = desconto.negate();
        } else if (formaPagamento.equals("BOLETO")) {
            totalFinal = totalPedido.add(new BigDecimal("3.49")).setScale(2, RoundingMode.HALF_EVEN);
            valorParcela = totalFinal;
            ajustePagamento = new BigDecimal("3.49");
        } else if (formaPagamento.equals("CARTAO")) {
            if (parcelas <= 3) {
                totalFinal = totalPedido;
                valorParcela = totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                ajustePagamento = ZERO.setScale(2, RoundingMode.HALF_EVEN);
            } else {
                double taxaMensal = 0.0199;
                double fatorJuros = Math.pow(1 + taxaMensal, -parcelas);
                double multiplicador = taxaMensal / (1 - fatorJuros);
                valorParcela = totalPedido.multiply(new BigDecimal(multiplicador)).setScale(2, RoundingMode.HALF_EVEN);
                totalFinal = valorParcela.multiply(new BigDecimal(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
                ajustePagamento = totalFinal.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
            }
        } else {
            totalFinal = totalPedido;
            valorParcela = totalPedido;
            ajustePagamento = ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        // 8. Crédito do clube
        BigDecimal creditoClube = calcularCreditoClube(nivelClube, subtotal);

        // 9. Brinde
        boolean temBrinde = nivelClube.equals("OURO") && subtotal.compareTo(new BigDecimal("500")) > 0;

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotal);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntrega);
        response.setImposto(imposto);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoClube);
        response.setBrinde(temBrinde);

        return response;
    }

    private void validarRequisicao(CheckoutRequest request) throws Exception {
        // 1. Validar carrinho
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ValidationException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getQuantidade() == null || item.getPesoKg() == null ||
                item.getPrecoUnitario().doubleValue() <= 0 || item.getQuantidade() <= 0 || item.getPesoKg() < 0) {
                throw new ValidationException("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar nível do clube
        if (request.getNivelClube() == null || !isValidNivelClube(request.getNivelClube())) {
            throw new ValidationException("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Validar região
        if (request.getRegiao() != null && !REGION_TAX_MAP.containsKey(request.getRegiao())) {
            throw new ValidationException("REGIAO_INVALIDA");
        }

        // 4. Validar modalidade de entrega
        if (request.getModalidadeEntrega() == null || !isValidModalidade(request.getModalidadeEntrega())) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }

        // 5. Validar se modalidade atende o pedido
        double pesoTotal = request.getItens().stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
        if (request.getModalidadeEntrega().equals("MOTOBOY") && pesoTotal > 5) {
            throw new ValidationException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Validar cupom
        if (request.getCupom() != null && !isValidCupom(request.getCupom())) {
            throw new ValidationException("CUPOM_INVALIDO");
        }

        // 7. Validar se cupom é aplicável
        if (request.getCupom() != null) {
            BigDecimal subtotal = calcularSubtotal(request.getItens());
            if (request.getCupom().equals("MENOS50") && subtotal.compareTo(new BigDecimal("300")) < 0) {
                throw new ValidationException("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Validar forma de pagamento
        if (request.getFormaPagamento() == null || !isValidFormaPagamento(request.getFormaPagamento())) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Validar número de parcelas
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!isValidParcelamento(request.getFormaPagamento(), parcelas)) {
            throw new ValidationException("PARCELAMENTO_INVALIDO");
        }

        // 10. Validar se forma de pagamento atende o pedido
        BigDecimal subtotal2 = calcularSubtotal(request.getItens());
        double pesoTotal2 = request.getItens().stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
        BigDecimal frete2 = calcularFrete(request.getModalidadeEntrega(), pesoTotal2, request.getNivelClube());
        BigDecimal descontoCupom2 = calcularDescontoCupom(request.getCupom(), subtotal2, request.getItens(), frete2);
        BigDecimal imposto2 = ZERO;
        if (request.getRegiao() != null) {
            BigDecimal impostoPercentual2 = new BigDecimal(REGION_TAX_MAP.get(request.getRegiao()));
            imposto2 = calcularImposto(subtotal2, descontoCupom2, impostoPercentual2);
        }
        BigDecimal totalPedido = subtotal2.subtract(descontoCupom2).add(frete2).add(imposto2);

        if (request.getFormaPagamento().equals("BOLETO") && totalPedido.compareTo(new BigDecimal("1000")) > 0) {
            throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private boolean isValidNivelClube(String nivel) {
        return nivel.equals("BRONZE") || nivel.equals("PRATA") || nivel.equals("OURO");
    }

    private boolean isValidModalidade(String modalidade) {
        return DELIVERY_DAYS.containsKey(modalidade);
    }

    private boolean isValidCupom(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private boolean isValidFormaPagamento(String forma) {
        return forma.equals("PIX") || forma.equals("CARTAO") || forma.equals("BOLETO");
    }

    private boolean isValidParcelamento(String forma, int parcelas) {
        if (forma.equals("PIX") || forma.equals("BOLETO")) {
            return parcelas == 1;
        }
        if (forma.equals("CARTAO")) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> new BigDecimal(item.getPrecoUnitario()).multiply(new BigDecimal(item.getQuantidade())))
            .reduce(ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private double calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        if (cupom == null) {
            return ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        if (cupom.equals("BEMVINDO10")) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (cupom.equals("MENOS50")) {
            return new BigDecimal("50").setScale(2, RoundingMode.HALF_EVEN);
        }

        if (cupom.equals("FRETEGRATIS")) {
            return frete;
        }

        if (cupom.equals("LEVE3PAGUE2")) {
            BigDecimal desconto = ZERO;
            for (ItemRequest item : itens) {
                int quantidade = item.getQuantidade();
                int itensGratis = quantidade / 3;
                BigDecimal precoUnitario = new BigDecimal(item.getPrecoUnitario());
                desconto = desconto.add(precoUnitario.multiply(new BigDecimal(itensGratis)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }

        return ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularFrete(String modalidade, double pesoTotal, String nivelClube) {
        if (nivelClube.equals("OURO")) {
            return ZERO;
        }

        if (modalidade.equals("RETIRADA_LOJA")) {
            return ZERO;
        }

        if (modalidade.equals("ECONOMICA")) {
            BigDecimal base = new BigDecimal("12");
            BigDecimal porKg = new BigDecimal("2").multiply(new BigDecimal(pesoTotal));
            return base.add(porKg).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (modalidade.equals("EXPRESSA")) {
            BigDecimal base = new BigDecimal("25");
            BigDecimal porKg = new BigDecimal("4.50").multiply(new BigDecimal(pesoTotal));
            return base.add(porKg).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (modalidade.equals("MOTOBOY")) {
            return new BigDecimal("18").setScale(2, RoundingMode.HALF_EVEN);
        }

        return ZERO;
    }

    private BigDecimal calcularImposto(BigDecimal subtotal, BigDecimal desconto, BigDecimal percentual) {
        BigDecimal base = subtotal.subtract(desconto);
        return base.multiply(percentual).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularValorParcela(String forma, int parcelas, BigDecimal totalPedido) {
        if (forma.equals("PIX") || forma.equals("BOLETO")) {
            return totalPedido;
        }

        if (forma.equals("CARTAO")) {
            if (parcelas <= 3) {
                return totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            }
            double taxaMensal = 0.0199;
            double fatorJuros = Math.pow(1 + taxaMensal, -parcelas);
            double multiplicador = taxaMensal / (1 - fatorJuros);
            return totalPedido.multiply(new BigDecimal(multiplicador)).setScale(2, RoundingMode.HALF_EVEN);
        }

        return totalPedido;
    }

    private BigDecimal calcularTotalFinal(String forma, int parcelas, BigDecimal valorParcela, BigDecimal totalPedido) {
        if (forma.equals("PIX")) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            return totalPedido.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (forma.equals("BOLETO")) {
            return totalPedido.add(new BigDecimal("3.49")).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (forma.equals("CARTAO")) {
            return valorParcela.multiply(new BigDecimal(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
        }

        return totalPedido.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularCreditoClube(String nivel, BigDecimal subtotal) {
        if (nivel.equals("BRONZE")) {
            return ZERO;
        }

        if (nivel.equals("PRATA")) {
            return subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
        }

        if (nivel.equals("OURO")) {
            return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        }

        return ZERO;
    }

    public static class ValidationException extends Exception {
        private final String codigoErro;

        public ValidationException(String codigoErro) {
            super(codigoErro);
            this.codigoErro = codigoErro;
        }

        public String getCodigoErro() {
            return codigoErro;
        }
    }
}
