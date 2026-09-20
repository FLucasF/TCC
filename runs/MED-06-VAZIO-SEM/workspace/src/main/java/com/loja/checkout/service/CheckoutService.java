package com.loja.checkout.service;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) throws CheckoutException {
        validarPedido(request);
        validarModalidade(request);
        validarCupom(request);
        validarFormaPagamento(request);

        BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
        BigDecimal desconto = calcularDescontoCupom(request.getItens(), subtotal, request.getCupom());
        BigDecimal frete = calcularFrete(request.getItens(), request.getModalidadeEntrega());
        Integer prazo = calcularPrazo(request.getModalidadeEntrega());

        BigDecimal totalSemAjuste = subtotal.subtract(desconto).add(frete);

        BigDecimal ajuste = BigDecimal.ZERO;
        BigDecimal totalFinal = totalSemAjuste;

        if (request.getFormaPagamento().equals("PIX")) {
            ajuste = arredondar(totalSemAjuste.multiply(new BigDecimal("0.05")).negate());
            totalFinal = arredondar(totalSemAjuste.add(ajuste));
        } else if (request.getFormaPagamento().equals("BOLETO")) {
            ajuste = arredondar(new BigDecimal("3.49"));
            totalFinal = arredondar(totalSemAjuste.add(ajuste));
        } else if (request.getFormaPagamento().equals("CARTAO") && request.getParcelas() > 3) {
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal um = BigDecimal.ONE;
            BigDecimal umMaisTaxa = um.add(taxaMensal);
            BigDecimal fator = umMaisTaxa.pow(request.getParcelas());
            BigDecimal numerador = taxaMensal.multiply(fator);
            BigDecimal denominador = fator.subtract(um);
            BigDecimal taxa = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);

            BigDecimal valorParcela = arredondar(totalSemAjuste.multiply(taxa));
            BigDecimal totalComParcelas = valorParcela.multiply(new BigDecimal(request.getParcelas()));
            ajuste = arredondar(totalComParcelas.subtract(totalSemAjuste));
            totalFinal = arredondar(totalSemAjuste.add(ajuste));
        }

        BigDecimal valorParcela = calcularValorParcela(totalFinal, request.getFormaPagamento(), request.getParcelas());

        return new CheckoutResponse(
            subtotal,
            desconto,
            frete,
            prazo,
            ajuste,
            totalFinal,
            request.getParcelas(),
            valorParcela
        );
    }

    private void validarPedido(CheckoutRequest request) throws CheckoutException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidade(CheckoutRequest request) throws CheckoutException {
        if (request.getModalidadeEntrega() == null || request.getModalidadeEntrega().isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            Modalidade.valueOf(request.getModalidadeEntrega());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        double pesoTotal = calcularPesoTotal(request.getItens());
        if (request.getModalidadeEntrega().equals("MOTOBOY") && pesoTotal > 5.0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(CheckoutRequest request) throws CheckoutException {
        String cupom = request.getCupom();
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        try {
            Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (cupom.equals("MENOS50")) {
            BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
            if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarFormaPagamento(CheckoutRequest request) throws CheckoutException {
        if (request.getFormaPagamento() == null || request.getFormaPagamento().isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            FormaPagamento.valueOf(request.getFormaPagamento());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        String forma = request.getFormaPagamento();
        int parcelas = request.getParcelas();

        if (forma.equals("PIX") || forma.equals("BOLETO")) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (forma.equals("CARTAO")) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if (forma.equals("BOLETO")) {
            BigDecimal subtotal = calcularSubtotalProdutos(request.getItens());
            BigDecimal desconto = calcularDescontoCupom(request.getItens(), subtotal, request.getCupom());
            BigDecimal frete = calcularFrete(request.getItens(), request.getModalidadeEntrega());
            BigDecimal total = subtotal.subtract(desconto).add(frete);
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(List<Item> itens, BigDecimal subtotal, String cupom) {
        if (cupom == null || cupom.isEmpty()) {
            return arredondar(BigDecimal.ZERO);
        }

        if (cupom.equals("BEMVINDO10")) {
            return arredondar(subtotal.multiply(new BigDecimal("0.10")));
        } else if (cupom.equals("MENOS50")) {
            return arredondar(new BigDecimal("50.00"));
        } else if (cupom.equals("FRETEGRATIS")) {
            return arredondar(BigDecimal.ZERO);
        } else if (cupom.equals("LEVE3PAGUE2")) {
            BigDecimal totalPago = aplicarLeve3Pague2(itens);
            return arredondar(subtotal.subtract(totalPago));
        }

        return arredondar(BigDecimal.ZERO);
    }

    private BigDecimal aplicarLeve3Pague2(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            int quantidade = item.getQuantidade();

            int gruposde3 = quantidade / 3;
            int itensPagos = quantidade - gruposde3;

            total = total.add(preco.multiply(new BigDecimal(itensPagos)));
        }
        return arredondar(total);
    }

    private BigDecimal calcularFrete(List<Item> itens, String modalidade) {
        double pesoTotal = calcularPesoTotal(itens);

        switch (modalidade) {
            case "ECONOMICA":
                return arredondar(new BigDecimal("12.00").add(
                    new BigDecimal(pesoTotal).multiply(new BigDecimal("2.00"))
                ));
            case "EXPRESSA":
                return arredondar(new BigDecimal("25.00").add(
                    new BigDecimal(pesoTotal).multiply(new BigDecimal("4.50"))
                ));
            case "RETIRADA_LOJA":
                return arredondar(BigDecimal.ZERO);
            case "MOTOBOY":
                return arredondar(new BigDecimal("18.00"));
            default:
                return arredondar(BigDecimal.ZERO);
        }
    }

    private double calcularPesoTotal(List<Item> itens) {
        double peso = 0;
        for (Item item : itens) {
            peso += item.getPesoKg() * item.getQuantidade();
        }
        return peso;
    }

    private Integer calcularPrazo(String modalidade) {
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

    private BigDecimal calcularValorParcela(BigDecimal total, String forma, int parcelas) {
        if (forma.equals("CARTAO") && parcelas > 3) {
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal um = BigDecimal.ONE;
            BigDecimal umMaisTaxa = um.add(taxaMensal);
            BigDecimal fator = umMaisTaxa.pow(parcelas);
            BigDecimal numerador = taxaMensal.multiply(fator);
            BigDecimal denominador = fator.subtract(um);
            BigDecimal taxa = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);

            BigDecimal parcela = total.multiply(taxa);
            return arredondar(parcela);
        } else {
            return arredondar(total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
        }
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    enum Modalidade {
        ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY
    }

    enum Cupom {
        BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2
    }

    enum FormaPagamento {
        PIX, CARTAO, BOLETO
    }
}
