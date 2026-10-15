package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CheckoutService {
    private static final BigDecimal TAXA_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_CARTAO_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal TAXA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal LIMITE_MOTOBOY = new BigDecimal("5");

    public ResumoResponse calcularResumo(PedidoRequest request) throws ErroCheckout {
        validarPedido(request);

        List<ItemPedido> itens = request.getItens();
        String modalidade = request.getModalidadeEntrega();
        String cupom = request.getCupom();
        String formaPagamento = request.getFormaPagamento();
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal subtotal = calcularSubtotal(itens);

        BigDecimal frete = calcularFrete(modalidade, itens);

        BigDecimal desconto = calcularDesconto(cupom, subtotal, frete, itens);

        BigDecimal totalAntesPagamento = subtotal.subtract(desconto).add(frete);

        BigDecimal ajuste = calcularAjustePagamento(formaPagamento, totalAntesPagamento, parcelas);

        BigDecimal totalFinal = totalAntesPagamento.add(ajuste);

        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        int prazo = getPrazoEntrega(modalidade);

        return new ResumoResponse(
            subtotal.doubleValue(),
            desconto.doubleValue(),
            frete.doubleValue(),
            prazo,
            ajuste.doubleValue(),
            totalFinal.doubleValue(),
            parcelas,
            valorParcela.doubleValue()
        );
    }

    private void validarPedido(PedidoRequest request) throws ErroCheckout {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : request.getItens()) {
            if (item.getPrecoUnitario() <= 0 || item.getQuantidade() <= 0 || item.getPesoKg() < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            if (item.getPrecoUnitario() == 0 || item.getQuantidade() == 0 ||
                Double.isNaN(item.getPesoKg()) || Double.isInfinite(item.getPesoKg())) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }

        String modalidade = request.getModalidadeEntrega();
        if (modalidade == null || modalidade.isEmpty()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        if (!isModalidadeValida(modalidade)) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        if (!isModalidadeDisponivel(modalidade, pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        String cupom = request.getCupom();
        if (cupom != null && !cupom.isEmpty()) {
            if (!isCupomValido(cupom)) {
                throw new ErroCheckout("CUPOM_INVALIDO");
            }

            BigDecimal subtotal = calcularSubtotal(request.getItens());
            if (!isCupomAplicavel(cupom, subtotal)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }

        String formaPagamento = request.getFormaPagamento();
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!isFormaPagamentoValida(formaPagamento)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!isParcelamentoValido(formaPagamento, parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(request.getItens());
        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens());
        BigDecimal desconto = calcularDesconto(cupom, subtotal, frete, request.getItens());
        BigDecimal totalAntesPagamento = subtotal.subtract(desconto).add(frete);

        if (!isFormaPagamentoDisponivel(formaPagamento, totalAntesPagamento)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal preco = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDesconto(String cupom, BigDecimal subtotal, BigDecimal frete, List<ItemPedido> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case "BEMVINDO10":
                BigDecimal desconto10 = subtotal.multiply(new BigDecimal("0.10"));
                return arredondar(desconto10);
            case "MENOS50":
                return arredondar(new BigDecimal("50.00"));
            case "FRETEGRATIS":
                return frete;
            case "LEVE3PAGUE2":
                BigDecimal descontoLeve = BigDecimal.ZERO;
                for (ItemPedido item : itens) {
                    int quantidadeGratis = item.getQuantidade() / 3;
                    if (quantidadeGratis > 0) {
                        BigDecimal preco = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
                        descontoLeve = descontoLeve.add(preco.multiply(new BigDecimal(quantidadeGratis)));
                    }
                }
                return arredondar(descontoLeve);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularFrete(String modalidade, List<ItemPedido> itens) throws ErroCheckout {
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        if ("FRETEGRATIS".equals(modalidade)) {
            return BigDecimal.ZERO;
        }

        switch (modalidade) {
            case "ECONOMICA":
                BigDecimal freteEconomica = new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotal));
                return arredondar(freteEconomica);
            case "EXPRESSA":
                BigDecimal freteExpressa = new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotal));
                return arredondar(freteExpressa);
            case "RETIRADA_LOJA":
                return BigDecimal.ZERO;
            case "MOTOBOY":
                return arredondar(new BigDecimal("18.00"));
            default:
                throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal pesoItem = new BigDecimal(String.valueOf(item.getPesoKg()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            peso = peso.add(pesoItem.multiply(quantidade));
        }
        return peso;
    }

    private BigDecimal calcularAjustePagamento(String formaPagamento, BigDecimal total, int parcelas) throws ErroCheckout {
        switch (formaPagamento) {
            case "PIX":
                BigDecimal desconto = total.multiply(TAXA_PIX);
                return arredondar(desconto).negate();
            case "CARTAO":
                if (parcelas <= 3) {
                    return BigDecimal.ZERO;
                }
                return calcularJurosCartao(total, parcelas);
            case "BOLETO":
                return arredondar(TAXA_BOLETO);
            default:
                throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal total, int parcelas) {
        BigDecimal taxa = TAXA_CARTAO_MENSAL;
        BigDecimal n = new BigDecimal(parcelas);

        BigDecimal divisor = BigDecimal.ONE.add(taxa).pow(parcelas, new java.math.MathContext(10));
        divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(divisor, 10, RoundingMode.HALF_EVEN));

        BigDecimal parcela = total.multiply(taxa).divide(divisor, 10, RoundingMode.HALF_EVEN);
        parcela = arredondar(parcela);

        BigDecimal totalJuros = parcela.multiply(n).subtract(total);
        return totalJuros;
    }

    private BigDecimal calcularValorParcela(String formaPagamento, BigDecimal total, int parcelas) throws ErroCheckout {
        switch (formaPagamento) {
            case "PIX":
            case "BOLETO":
                return total;
            case "CARTAO":
                if (parcelas <= 3) {
                    BigDecimal parcela = total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN);
                    return arredondar(parcela);
                }
                BigDecimal taxa = TAXA_CARTAO_MENSAL;
                BigDecimal divisor = BigDecimal.ONE.add(taxa).pow(parcelas, new java.math.MathContext(10));
                divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(divisor, 10, RoundingMode.HALF_EVEN));

                BigDecimal parcela = total.multiply(taxa).divide(divisor, 10, RoundingMode.HALF_EVEN);
                return arredondar(parcela);
            default:
                throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private int getPrazoEntrega(String modalidade) {
        switch (modalidade) {
            case "ECONOMICA":
                return 7;
            case "EXPRESSA":
                return 2;
            case "RETIRADA_LOJA":
                return 1;
            case "MOTOBOY":
                return 0;
            default:
                return 0;
        }
    }

    private boolean isModalidadeValida(String modalidade) {
        return modalidade.equals("ECONOMICA") ||
               modalidade.equals("EXPRESSA") ||
               modalidade.equals("RETIRADA_LOJA") ||
               modalidade.equals("MOTOBOY");
    }

    private boolean isModalidadeDisponivel(String modalidade, BigDecimal pesoTotal) {
        if (modalidade.equals("MOTOBOY")) {
            return pesoTotal.compareTo(LIMITE_MOTOBOY) <= 0;
        }
        return true;
    }

    private boolean isCupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") ||
               cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") ||
               cupom.equals("LEVE3PAGUE2");
    }

    private boolean isCupomAplicavel(String cupom, BigDecimal subtotal) {
        if (cupom.equals("MENOS50")) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        return true;
    }

    private boolean isFormaPagamentoValida(String formaPagamento) {
        return formaPagamento.equals("PIX") ||
               formaPagamento.equals("CARTAO") ||
               formaPagamento.equals("BOLETO");
    }

    private boolean isParcelamentoValido(String formaPagamento, int parcelas) {
        if (formaPagamento.equals("PIX") || formaPagamento.equals("BOLETO")) {
            return parcelas == 1;
        }
        if (formaPagamento.equals("CARTAO")) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean isFormaPagamentoDisponivel(String formaPagamento, BigDecimal total) {
        if (formaPagamento.equals("BOLETO")) {
            return total.compareTo(LIMITE_BOLETO) <= 0;
        }
        return true;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
