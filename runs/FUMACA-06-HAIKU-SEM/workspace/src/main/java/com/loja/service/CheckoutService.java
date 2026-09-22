package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.Item;
import com.loja.dto.ResumoCheckoutRequest;
import com.loja.dto.ResumoCheckoutResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {
    private static final Map<String, Cupom> cupons = new HashMap<>();
    private static final Map<String, Modalidade> modalidades = new HashMap<>();

    static {
        cupons.put("BEMVINDO10", new CupomBemvindo10());
        cupons.put("MENOS50", new CupomMenos50());
        cupons.put("FRETEGRATIS", new CupomFreteGratis());
        cupons.put("LEVE3PAGUE2", new CupomLeve3Pague2());

        for (Modalidade m : Modalidade.values()) {
            modalidades.put(m.name(), m);
        }
    }

    public ResumoCheckoutResponse calcularResumo(ResumoCheckoutRequest request) throws CheckoutException {
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarPedido(request.getItens());
        validarModalidade(request.getModalidadeEntrega());
        validarCupom(request.getCupom());
        validarFormaPagamento(request.getFormaPagamento());

        Modalidade modalidade = modalidades.get(request.getModalidadeEntrega());
        Double subtotalProdutos = calcularSubtotal(request.getItens());

        Double frete = calcularFrete(modalidade, request.getItens());

        Double totalComFrete = Arredondamento.arredondar(subtotalProdutos + frete);

        validarModalidadeDisponibilidade(request.getModalidadeEntrega(), request.getItens(), totalComFrete);

        Cupom cupom = cupons.get(request.getCupom());
        Double descontoCupom = 0.0;

        if (cupom != null) {
            if (!cupom.ehAplicavel(subtotalProdutos, totalComFrete)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            Cupom.CupomResultado resultado = cupom.calcular(subtotalProdutos, frete, request.getItens());
            descontoCupom = resultado.desconto;
            frete = resultado.frete;
        }

        validarFormaPagamentoDisponibilidade(request.getFormaPagamento(), subtotalProdutos, descontoCupom, frete);
        validarParcelamento(request.getFormaPagamento(), parcelas);

        Double totalPedido = Arredondamento.arredondar(subtotalProdutos - descontoCupom + frete);

        Double ajustePagamento;
        Double totalFinal;
        Double valorParcela;

        if (request.getFormaPagamento().equals("CARTAO") && parcelas > 3) {
            valorParcela = calcularParcelaComJuros(totalPedido, 0.0199, parcelas);
            totalFinal = Arredondamento.arredondar(valorParcela * parcelas);
            ajustePagamento = Arredondamento.arredondar(totalFinal - totalPedido);
        } else {
            ajustePagamento = calcularAjustePagamento(request.getFormaPagamento(), totalPedido);
            totalFinal = Arredondamento.arredondar(totalPedido + ajustePagamento);
            valorParcela = Arredondamento.arredondar(totalFinal / parcelas);
        }

        return new ResumoCheckoutResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(java.util.List<Item> itens) throws CheckoutException {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidade(String modalidade) throws CheckoutException {
        if (modalidade == null || !modalidades.containsKey(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarCupom(String cupom) throws CheckoutException {
        if (cupom != null && !cupons.containsKey(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarFormaPagamento(String formaPagamento) throws CheckoutException {
        if (formaPagamento == null || (!formaPagamento.equals("PIX") && !formaPagamento.equals("CARTAO") && !formaPagamento.equals("BOLETO"))) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarModalidadeDisponibilidade(String modalidadeStr, java.util.List<Item> itens, Double totalComFrete) throws CheckoutException {
        if (modalidadeStr.equals("MOTOBOY")) {
            Double peso = 0.0;
            for (Item item : itens) {
                peso += item.getPesoKg() * item.getQuantidade();
            }
            if (peso > 5.0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarFormaPagamentoDisponibilidade(String formaPagamento, Double subtotalProdutos, Double descontoCupom, Double frete) throws CheckoutException {
        Double totalPedido = subtotalProdutos - descontoCupom + frete;

        if (formaPagamento.equals("BOLETO") && totalPedido > 1000.0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void validarParcelamento(String formaPagamento, Integer parcelas) throws CheckoutException {
        if (parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if ((formaPagamento.equals("PIX") || formaPagamento.equals("BOLETO")) && parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (formaPagamento.equals("CARTAO") && (parcelas < 1 || parcelas > 12)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private Double calcularSubtotal(java.util.List<Item> itens) {
        Double subtotal = 0.0;
        for (Item item : itens) {
            Double valorItem = Arredondamento.arredondar(item.getPrecoUnitario() * item.getQuantidade());
            subtotal = Arredondamento.arredondar(subtotal + valorItem);
        }
        return subtotal;
    }

    private Double calcularFrete(Modalidade modalidade, java.util.List<Item> itens) {
        if (modalidade.name().equals("RETIRADA_LOJA")) {
            return 0.0;
        }

        if (modalidade.name().equals("MOTOBOY")) {
            return modalidade.getBaseValue();
        }

        Double peso = 0.0;
        for (Item item : itens) {
            peso += item.getPesoKg() * item.getQuantidade();
        }

        Double frete = modalidade.getBaseValue() + (peso * modalidade.getPerKg());
        return Arredondamento.arredondar(frete);
    }

    private Double calcularAjustePagamento(String formaPagamento, Double totalPedido) {
        if (formaPagamento.equals("PIX")) {
            return Arredondamento.arredondar(-totalPedido * 0.05);
        }

        if (formaPagamento.equals("BOLETO")) {
            return 3.49;
        }

        return 0.0;
    }

    private Double calcularParcelaComJuros(Double total, Double taxaMensal, Integer parcelas) {
        BigDecimal t = new BigDecimal(total);
        BigDecimal taxa = new BigDecimal(taxaMensal);

        BigDecimal um = new BigDecimal(1);
        BigDecimal taxa_mais_um = um.add(taxa);
        BigDecimal denominador = um.subtract(taxa_mais_um.pow(-parcelas.intValue(), new java.math.MathContext(10)));

        BigDecimal parcela = t.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
        return parcela.doubleValue();
    }

    public static class CheckoutException extends Exception {
        private String codigo;

        public CheckoutException(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
