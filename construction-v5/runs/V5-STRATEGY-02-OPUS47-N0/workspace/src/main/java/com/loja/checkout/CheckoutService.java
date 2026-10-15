package com.loja.checkout;

import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private static final RoundingMode RM = RoundingMode.HALF_EVEN;
    private static final MathContext MC = new MathContext(20, RM);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RM);
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal TRES_CENTOS = new BigDecimal("300");
    private static final BigDecimal QUINHENTOS = new BigDecimal("500");
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal DESC_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal CINCO_KG = new BigDecimal("5");

    public RespostaResumo calcular(PedidoRequest req) {
        validarItens(req);

        NivelClube nivel = parseEnum(req.nivelClube(), NivelClube.class, "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(req.regiao(), Regiao.class, "REGIAO_INVALIDA");
        Modalidade modalidade = parseEnum(req.modalidadeEntrega(), Modalidade.class, "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = BigDecimal.ZERO;
        BigDecimal subtotalRaw = BigDecimal.ZERO;
        for (var it : req.itens()) {
            pesoTotal = pesoTotal.add(it.pesoKg().multiply(BigDecimal.valueOf(it.quantidade())));
            subtotalRaw = subtotalRaw.add(it.precoUnitario().multiply(BigDecimal.valueOf(it.quantidade())));
        }
        BigDecimal subtotal = subtotalRaw.setScale(2, RM);

        if (!modalidade.suporta(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteCalculado = modalidade.frete(pesoTotal).setScale(2, RM);

        Cupom cupom = parseCupom(req.cupom());
        BigDecimal desconto = calcularDescontoCupom(cupom, subtotal, req);

        BigDecimal frete = (nivel == NivelClube.OURO) ? ZERO : freteCalculado;
        if (cupom == Cupom.FRETEGRATIS) {
            desconto = frete;
        }

        BigDecimal seguro = subtotal.multiply(regiao.taxaSeguro).setScale(2, RM);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro).setScale(2, RM);

        FormaPagamento fp = parseEnum(req.formaPagamento(), FormaPagamento.class, "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        validarParcelas(fp, parcelas);

        if (fp == FormaPagamento.BOLETO && totalPedido.compareTo(MIL) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajuste;
        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (fp) {
            case PIX -> {
                ajuste = totalPedido.multiply(DESC_PIX).setScale(2, RM).negate();
                totalFinal = totalPedido.add(ajuste).setScale(2, RM);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                ajuste = TARIFA_BOLETO;
                totalFinal = totalPedido.add(ajuste).setScale(2, RM);
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    totalFinal = totalPedido;
                    ajuste = ZERO;
                    valorParcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RM);
                } else {
                    double pow = Math.pow(1.0 + TAXA_CARTAO.doubleValue(), parcelas);
                    BigDecimal onePlusIpowN = new BigDecimal(pow, MC);
                    BigDecimal invPow = BigDecimal.ONE.divide(onePlusIpowN, MC);
                    BigDecimal denom = BigDecimal.ONE.subtract(invPow);
                    BigDecimal num = totalPedido.multiply(TAXA_CARTAO);
                    valorParcela = num.divide(denom, MC).setScale(2, RM);
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RM);
                    ajuste = totalFinal.subtract(totalPedido).setScale(2, RM);
                }
            }
            default -> throw new IllegalStateException();
        }

        BigDecimal credito = subtotal.multiply(nivel.taxaCredito).setScale(2, RM);
        boolean brinde = nivel == NivelClube.OURO && subtotal.compareTo(QUINHENTOS) > 0;

        return new RespostaResumo(
                subtotal, desconto, frete, modalidade.prazoDias, seguro,
                ajuste, totalFinal, parcelas, valorParcela, credito, brinde
        );
    }

    private void validarItens(PedidoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (var it : req.itens()) {
            if (it == null
                    || it.precoUnitario() == null || it.precoUnitario().signum() <= 0
                    || it.quantidade() == null || it.quantidade() <= 0
                    || it.pesoKg() == null || it.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(String valor, Class<E> type, String erro) {
        if (valor == null || valor.isBlank()) throw new CheckoutException(erro);
        try {
            return Enum.valueOf(type, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }

    private Cupom parseCupom(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal, PedidoRequest req) {
        if (cupom == null) return ZERO;
        return switch (cupom) {
            case BEMVINDO10 -> subtotal.multiply(new BigDecimal("0.10")).setScale(2, RM);
            case MENOS50 -> {
                if (subtotal.compareTo(TRES_CENTOS) < 0) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                yield new BigDecimal("50.00");
            }
            case LEVE3PAGUE2 -> {
                BigDecimal d = BigDecimal.ZERO;
                for (var it : req.itens()) {
                    int gratis = it.quantidade() / 3;
                    if (gratis > 0) {
                        d = d.add(it.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                    }
                }
                yield d.setScale(2, RM);
            }
            case FRETEGRATIS -> ZERO;
        };
    }

    private void validarParcelas(FormaPagamento fp, int parcelas) {
        switch (fp) {
            case PIX, BOLETO -> {
                if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            case CARTAO -> {
                if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }
}
