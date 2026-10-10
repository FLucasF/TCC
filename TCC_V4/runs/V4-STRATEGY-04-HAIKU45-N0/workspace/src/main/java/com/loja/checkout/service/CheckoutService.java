package com.loja.checkout.service;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Item;
import com.loja.checkout.model.dto.CheckoutRequest;
import com.loja.checkout.model.dto.CheckoutResponse;
import com.loja.checkout.model.enums.FormaPagamento;
import com.loja.checkout.model.enums.ModalidadeEntrega;
import com.loja.checkout.model.enums.NivelClube;
import com.loja.checkout.model.enums.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarEntrada(request);

        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getNivelClube());
        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, frete, request.getItens());
        int prazoEntrega = obterPrazoEntrega(request.getModalidadeEntrega());
        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        BigDecimal basePagamento = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro)
            .setScale(SCALE, ROUNDING);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal ajustePagamento = calcularAjustePagamento(basePagamento, request.getFormaPagamento(), parcelas);
        BigDecimal totalFinal = basePagamento.add(ajustePagamento).setScale(SCALE, ROUNDING);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas, request.getFormaPagamento());
        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, request.getNivelClube());
        boolean brinde = verificarBrinde(subtotalProdutos, request.getNivelClube());

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntrega);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarEntrada(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (request.getFormaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        validarCupom(request.getCupom(), calcularSubtotal(request.getItens()));
        validarModalidadeEntrega(request.getModalidadeEntrega(), request.getItens());
        validarFormaPagamento(request.getFormaPagamento(), request.getParcelas());
    }

    private void validarCupom(String cupom, BigDecimal subtotal) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        switch (cupom) {
            case "BEMVINDO10":
            case "FRETEGRATIS":
            case "LEVE3PAGUE2":
                return;
            case "MENOS50":
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
                return;
            default:
                throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarModalidadeEntrega(ModalidadeEntrega modalidade, java.util.List<Item> itens) {
        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            BigDecimal pesoTotal = itens.stream()
                .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(ZERO, BigDecimal::add);

            if (pesoTotal.compareTo(new BigDecimal("5")) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarFormaPagamento(FormaPagamento forma, Integer parcelas) {
        int numParcelas = parcelas != null ? parcelas : 1;

        switch (forma) {
            case PIX:
            case BOLETO:
                if (numParcelas != 1) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
                break;
            case CARTAO:
                if (numParcelas < 1 || numParcelas > 12) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
                break;
        }
    }

    private BigDecimal calcularSubtotal(java.util.List<Item> itens) {
        return itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(ZERO, BigDecimal::add)
            .setScale(SCALE, ROUNDING);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete, java.util.List<Item> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return ZERO.setScale(SCALE, ROUNDING);
        }

        switch (cupom) {
            case "BEMVINDO10":
                return subtotal.multiply(new BigDecimal("0.10")).setScale(SCALE, ROUNDING);
            case "MENOS50":
                return new BigDecimal("50.00").setScale(SCALE, ROUNDING);
            case "LEVE3PAGUE2":
                return itens.stream()
                    .map(item -> {
                        int quantidadeGratuita = item.getQuantidade() / 3;
                        return item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratuita));
                    })
                    .reduce(ZERO, BigDecimal::add)
                    .setScale(SCALE, ROUNDING);
            case "FRETEGRATIS":
                return frete;
            default:
                return ZERO.setScale(SCALE, ROUNDING);
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, java.util.List<Item> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return ZERO.setScale(SCALE, ROUNDING);
        }

        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(ZERO, BigDecimal::add);

        switch (modalidade) {
            case ECONOMICA:
                return new BigDecimal("12.00")
                    .add(pesoTotal.multiply(new BigDecimal("2.00")))
                    .setScale(SCALE, ROUNDING);
            case EXPRESSA:
                return new BigDecimal("25.00")
                    .add(pesoTotal.multiply(new BigDecimal("4.50")))
                    .setScale(SCALE, ROUNDING);
            case RETIRADA_LOJA:
                return ZERO.setScale(SCALE, ROUNDING);
            case MOTOBOY:
                return new BigDecimal("18.00").setScale(SCALE, ROUNDING);
            default:
                return ZERO.setScale(SCALE, ROUNDING);
        }
    }

    private int obterPrazoEntrega(ModalidadeEntrega modalidade) {
        switch (modalidade) {
            case ECONOMICA:
                return 7;
            case EXPRESSA:
                return 2;
            case RETIRADA_LOJA:
                return 1;
            case MOTOBOY:
                return 0;
            default:
                return 0;
        }
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal taxa = switch (regiao) {
            case SUDESTE -> new BigDecimal("0.01");
            case SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };

        return subtotal.multiply(taxa).setScale(SCALE, ROUNDING);
    }

    private BigDecimal calcularAjustePagamento(BigDecimal base, FormaPagamento forma, int parcelas) {
        switch (forma) {
            case PIX:
                return base.multiply(new BigDecimal("-0.05")).setScale(SCALE, ROUNDING);
            case BOLETO:
                validarBoleto(base);
                return new BigDecimal("3.49").setScale(SCALE, ROUNDING);
            case CARTAO:
                if (parcelas <= 3) {
                    return ZERO.setScale(SCALE, ROUNDING);
                }
                return calcularJurosCartao(base, parcelas);
            default:
                return ZERO.setScale(SCALE, ROUNDING);
        }
    }

    private void validarBoleto(BigDecimal total) {
        if (total.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal base, int parcelas) {
        java.math.MathContext mc = new java.math.MathContext(10);
        BigDecimal taxa = new BigDecimal("0.0199");

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaInv = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, mc), mc);
        BigDecimal denominator = BigDecimal.ONE.subtract(umMaisTaxaInv);

        BigDecimal numerator = base.multiply(taxa, mc);
        BigDecimal parcela = numerator.divide(denominator, SCALE, ROUNDING);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas)).setScale(SCALE, ROUNDING);
        return totalComJuros.subtract(base).setScale(SCALE, ROUNDING);
    }

    private BigDecimal calcularValorParcela(BigDecimal total, int parcelas, FormaPagamento forma) {
        return total.divide(new BigDecimal(parcelas), SCALE, ROUNDING);
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivel) {
        switch (nivel) {
            case PRATA:
                return subtotal.multiply(new BigDecimal("0.02")).setScale(SCALE, ROUNDING);
            case OURO:
                return subtotal.multiply(new BigDecimal("0.05")).setScale(SCALE, ROUNDING);
            case BRONZE:
            default:
                return ZERO.setScale(SCALE, ROUNDING);
        }
    }

    private boolean verificarBrinde(BigDecimal subtotal, NivelClube nivel) {
        return nivel == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }
}
