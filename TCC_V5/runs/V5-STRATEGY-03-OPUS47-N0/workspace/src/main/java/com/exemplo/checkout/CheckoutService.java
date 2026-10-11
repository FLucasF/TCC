package com.exemplo.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutService {

    private static final RoundingMode ROUND = RoundingMode.HALF_EVEN;
    private static final MathContext MC = new MathContext(30, ROUND);
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal LIMITE_MOTOBOY_KG = new BigDecimal("5");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal LIMITE_MENOS50 = new BigDecimal("300");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TAXA_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_CARTAO_MENSAL = new BigDecimal("0.0199");

    private static final Set<String> NIVEIS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");
    private static final Set<String> PAGAMENTOS = Set.of("PIX", "CARTAO", "BOLETO");

    public ResumoResponse calcular(ResumoRequest req) {
        if (req == null) throw new CheckoutException("PEDIDO_INVALIDO");

        // 1
        validarItens(req.itens());

        // 2
        String nivel = req.nivelClube();
        if (nivel == null || !NIVEIS.contains(nivel)) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        // 3
        String regiao = req.regiao();
        if (regiao == null || !REGIOES.contains(regiao)) throw new CheckoutException("REGIAO_INVALIDA");

        // 4
        String modalidade = req.modalidadeEntrega();
        if (modalidade == null || !MODALIDADES.contains(modalidade)) throw new CheckoutException("MODALIDADE_INVALIDA");

        // 5
        BigDecimal pesoTotal = calcularPeso(req.itens());
        if ("MOTOBOY".equals(modalidade) && pesoTotal.compareTo(LIMITE_MOTOBOY_KG) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(req.itens());

        // 6 e 7
        String cupom = req.cupom();
        if (cupom != null && !cupom.isEmpty() && !CUPONS.contains(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        if ("MENOS50".equals(cupom) && subtotal.compareTo(LIMITE_MENOS50) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // 8
        String pagamento = req.formaPagamento();
        if (pagamento == null || !PAGAMENTOS.contains(pagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        validarParcelas(pagamento, parcelas);

        // Cálculo
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivel);
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, req.itens(), subtotal, frete);
        BigDecimal seguro = calcularSeguro(regiao, subtotal);
        int prazo = prazoDias(modalidade);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, ROUND);

        // 10
        if ("BOLETO".equals(pagamento) && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste pagamento
        Pagamento result = aplicarPagamento(pagamento, totalPedido, parcelas);

        BigDecimal credito = calcularCredito(nivel, subtotal);
        boolean brinde = "OURO".equals(nivel) && subtotal.compareTo(LIMITE_BRINDE) > 0;

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                prazo,
                seguro,
                result.ajuste,
                result.totalFinal,
                parcelas,
                result.valorParcela,
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item == null) throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.quantidade() == null || item.quantidade() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.pesoKg() == null || item.pesoKg().signum() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total.setScale(2, ROUND);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal peso, String nivel) {
        if ("OURO".equals(nivel)) return new BigDecimal("0.00");
        BigDecimal valor = switch (modalidade) {
            case "ECONOMICA" -> new BigDecimal("12").add(new BigDecimal("2").multiply(peso));
            case "EXPRESSA" -> new BigDecimal("25").add(new BigDecimal("4.50").multiply(peso));
            case "RETIRADA_LOJA" -> BigDecimal.ZERO;
            case "MOTOBOY" -> new BigDecimal("18");
            default -> BigDecimal.ZERO;
        };
        return valor.setScale(2, ROUND);
    }

    private int prazoDias(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> 0;
        };
    }

    private BigDecimal calcularDescontoCupom(String cupom, List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        if (cupom == null || cupom.isEmpty()) return new BigDecimal("0.00");
        return switch (cupom) {
            case "BEMVINDO10" -> subtotal.multiply(new BigDecimal("0.10")).setScale(2, ROUND);
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> frete.setScale(2, ROUND);
            case "LEVE3PAGUE2" -> {
                BigDecimal total = BigDecimal.ZERO;
                for (ItemRequest item : itens) {
                    int gratis = item.quantidade() / 3;
                    if (gratis > 0) {
                        total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                    }
                }
                yield total.setScale(2, ROUND);
            }
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal calcularSeguro(String regiao, BigDecimal subtotal) {
        BigDecimal taxa = switch (regiao) {
            case "SUDESTE" -> new BigDecimal("0.01");
            case "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> BigDecimal.ZERO;
        };
        return subtotal.multiply(taxa).setScale(2, ROUND);
    }

    private BigDecimal calcularCredito(String nivel, BigDecimal subtotal) {
        BigDecimal taxa = switch (nivel) {
            case "PRATA" -> new BigDecimal("0.02");
            case "OURO" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
        return subtotal.multiply(taxa).setScale(2, ROUND);
    }

    private void validarParcelas(String pagamento, int parcelas) {
        switch (pagamento) {
            case "PIX", "BOLETO" -> {
                if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            case "CARTAO" -> {
                if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            default -> throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private record Pagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}

    private Pagamento aplicarPagamento(String pagamento, BigDecimal totalPedido, int parcelas) {
        switch (pagamento) {
            case "PIX" -> {
                BigDecimal desconto = totalPedido.multiply(TAXA_PIX).setScale(2, ROUND);
                BigDecimal ajuste = desconto.negate();
                BigDecimal totalFinal = totalPedido.add(ajuste).setScale(2, ROUND);
                return new Pagamento(ajuste, totalFinal, totalFinal);
            }
            case "BOLETO" -> {
                BigDecimal ajuste = TARIFA_BOLETO;
                BigDecimal totalFinal = totalPedido.add(ajuste).setScale(2, ROUND);
                return new Pagamento(ajuste, totalFinal, totalFinal);
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    BigDecimal totalFinal = totalPedido.setScale(2, ROUND);
                    BigDecimal parcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 2, ROUND);
                    return new Pagamento(new BigDecimal("0.00"), totalFinal, parcela);
                } else {
                    BigDecimal fator = BigDecimal.ONE.add(TAXA_CARTAO_MENSAL).pow(parcelas, MC);
                    BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC));
                    BigDecimal parcela = totalPedido.multiply(TAXA_CARTAO_MENSAL, MC)
                            .divide(denom, 2, ROUND);
                    BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas))
                            .setScale(2, ROUND);
                    BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2, ROUND);
                    return new Pagamento(ajuste, totalFinal, parcela);
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
