package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final RoundingMode HE = RoundingMode.HALF_EVEN;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final BigDecimal CINCO = new BigDecimal("5");
    private static final BigDecimal QUINHENTOS = new BigDecimal("500");
    private static final BigDecimal TREZENTOS = new BigDecimal("300");
    private static final BigDecimal TAXA_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(20, HE);

    private static final Set<String> NIVEIS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> FORMAS = Set.of("PIX", "CARTAO", "BOLETO");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");

    private static final Map<String, BigDecimal> TAXA_SEGURO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private static final Map<String, Integer> PRAZO_DIAS = Map.of(
            "ECONOMICA", 7,
            "EXPRESSA", 2,
            "RETIRADA_LOJA", 1,
            "MOTOBOY", 0
    );

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req.itens());
        validarEm(req.nivelClube(), NIVEIS, "NIVEL_CLUBE_INVALIDO");
        validarEm(req.regiao(), REGIOES, "REGIAO_INVALIDA");
        validarEm(req.modalidadeEntrega(), MODALIDADES, "MODALIDADE_INVALIDA");

        BigDecimal subtotal = calcSubtotal(req.itens());
        BigDecimal pesoTotal = calcPeso(req.itens());

        if ("MOTOBOY".equals(req.modalidadeEntrega()) && pesoTotal.compareTo(CINCO) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (req.cupom() != null && !CUPONS.contains(req.cupom())) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        if ("MENOS50".equals(req.cupom()) && subtotal.compareTo(TREZENTOS) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        validarEm(req.formaPagamento(), FORMAS, "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        validarParcelas(req.formaPagamento(), parcelas);

        BigDecimal freteBase = calcFreteBase(req.modalidadeEntrega(), pesoTotal);
        BigDecimal frete = "OURO".equals(req.nivelClube()) ? zero() : freteBase;

        BigDecimal desconto = calcDesconto(req.cupom(), req.itens(), subtotal, frete);

        BigDecimal seguro = round(subtotal.multiply(TAXA_SEGURO.get(req.regiao())));

        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if ("BOLETO".equals(req.formaPagamento()) && totalPedido.compareTo(MIL) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal;
        BigDecimal valorParcela;
        BigDecimal ajuste;

        switch (req.formaPagamento()) {
            case "PIX" -> {
                BigDecimal desc = round(totalPedido.multiply(TAXA_PIX));
                totalFinal = round(totalPedido.subtract(desc));
                valorParcela = totalFinal;
                ajuste = desc.negate();
            }
            case "BOLETO" -> {
                totalFinal = round(totalPedido.add(TARIFA_BOLETO));
                valorParcela = totalFinal;
                ajuste = TARIFA_BOLETO;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    totalFinal = round(totalPedido);
                    valorParcela = round(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, HE));
                    ajuste = zero();
                } else {
                    BigDecimal pow = BigDecimal.ONE.add(TAXA_CARTAO).pow(parcelas, MC);
                    BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(pow, MC), MC);
                    BigDecimal parcelaBD = totalPedido.multiply(TAXA_CARTAO, MC).divide(denom, MC);
                    valorParcela = round(parcelaBD);
                    totalFinal = round(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
                    ajuste = round(totalFinal.subtract(totalPedido));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal credito = switch (req.nivelClube()) {
            case "PRATA" -> round(subtotal.multiply(new BigDecimal("0.02")));
            case "OURO" -> round(subtotal.multiply(new BigDecimal("0.05")));
            default -> zero();
        };

        boolean brinde = "OURO".equals(req.nivelClube()) && subtotal.compareTo(QUINHENTOS) > 0;

        return new CheckoutResponse(
                round(subtotal),
                round(desconto),
                round(frete),
                PRAZO_DIAS.get(req.modalidadeEntrega()),
                round(seguro),
                round(ajuste),
                round(totalFinal),
                parcelas,
                round(valorParcela),
                round(credito),
                brinde
        );
    }

    private void validarItens(List<CheckoutRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (CheckoutRequest.Item it : itens) {
            if (it == null
                    || it.precoUnitario() == null || it.precoUnitario().signum() <= 0
                    || it.quantidade() == null || it.quantidade() <= 0
                    || it.pesoKg() == null || it.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarEm(String valor, Set<String> permitidos, String erro) {
        if (valor == null || !permitidos.contains(valor)) {
            throw new CheckoutException(erro);
        }
    }

    private void validarParcelas(String forma, int parcelas) {
        boolean ok = switch (forma) {
            case "PIX", "BOLETO" -> parcelas == 1;
            case "CARTAO" -> parcelas >= 1 && parcelas <= 12;
            default -> false;
        };
        if (!ok) throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }

    private BigDecimal calcSubtotal(List<CheckoutRequest.Item> itens) {
        BigDecimal s = BigDecimal.ZERO;
        for (var it : itens) {
            s = s.add(it.precoUnitario().multiply(BigDecimal.valueOf(it.quantidade())));
        }
        return round(s);
    }

    private BigDecimal calcPeso(List<CheckoutRequest.Item> itens) {
        BigDecimal p = BigDecimal.ZERO;
        for (var it : itens) {
            p = p.add(it.pesoKg().multiply(BigDecimal.valueOf(it.quantidade())));
        }
        return p;
    }

    private BigDecimal calcFreteBase(String modalidade, BigDecimal pesoKg) {
        return switch (modalidade) {
            case "ECONOMICA" -> round(new BigDecimal("12").add(new BigDecimal("2").multiply(pesoKg)));
            case "EXPRESSA" -> round(new BigDecimal("25").add(new BigDecimal("4.50").multiply(pesoKg)));
            case "RETIRADA_LOJA" -> zero();
            case "MOTOBOY" -> new BigDecimal("18.00");
            default -> zero();
        };
    }

    private BigDecimal calcDesconto(String cupom, List<CheckoutRequest.Item> itens,
                                     BigDecimal subtotal, BigDecimal freteExibido) {
        if (cupom == null) return zero();
        return switch (cupom) {
            case "BEMVINDO10" -> round(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> round(freteExibido);
            case "LEVE3PAGUE2" -> {
                BigDecimal d = BigDecimal.ZERO;
                for (var it : itens) {
                    int gratis = it.quantidade() / 3;
                    if (gratis > 0) {
                        d = d.add(it.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                    }
                }
                yield round(d);
            }
            default -> zero();
        };
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, HE);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
