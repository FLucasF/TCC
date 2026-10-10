package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.PedidoDto;
import com.loja.checkout.dto.ResumoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CheckoutService {

    private static final RoundingMode HALF_EVEN = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    private static final Set<String> NIVEIS_VALIDOS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES_VALIDAS = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES_VALIDAS = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> CUPONS_VALIDOS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");
    private static final Set<String> FORMAS_PAGAMENTO_VALIDAS = Set.of("PIX", "CARTAO", "BOLETO");

    private static final Map<String, Double> SEGURO_REGIAO = Map.of(
            "SUDESTE", 0.01,
            "SUL", 0.01,
            "CENTRO_OESTE", 0.015,
            "NORTE", 0.025,
            "NORDESTE", 0.02
    );

    public ResumoDto calcular(PedidoDto pedido) {
        // 1. Validar pedido
        validarPedido(pedido);

        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();

        // 2. Subtotal produtos
        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());

        // 3. Peso total
        double pesoTotal = calcularPeso(pedido.itens());

        // 4. Desconto cupom
        BigDecimal descontoCupom = calcularDescontoCupom(pedido.cupom(), subtotalProdutos, pedido.itens(), pedido.modalidadeEntrega());

        // 5. Frete
        BigDecimal frete = calcularFrete(pedido.modalidadeEntrega(), pesoTotal, pedido.nivelClube());

        // Para FRETEGRATIS, o desconto do cupom é o valor do frete (antes do desconto de clube OURO)
        if ("FRETEGRATIS".equals(pedido.cupom())) {
            BigDecimal freteNormal = calcularFreteNormal(pedido.modalidadeEntrega(), pesoTotal);
            descontoCupom = freteNormal;
            // Frete efetivo ainda é zero se OURO, ou zerado pelo cupom
            if (!"OURO".equals(pedido.nivelClube())) {
                frete = BigDecimal.ZERO;
            }
        }

        // 6. Seguro
        BigDecimal seguro = calcularSeguro(subtotalProdutos, pedido.regiao());

        // 7. Total do pedido (antes do pagamento)
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
        totalPedido = arredondar(totalPedido);

        // 8. Ajuste de pagamento
        BigDecimal[] ajusteETotal = calcularAjustePagamento(pedido.formaPagamento(), parcelas, totalPedido);
        BigDecimal ajustePagamento = ajusteETotal[0];
        BigDecimal totalFinal = ajusteETotal[1];

        // 9. Valor da parcela
        BigDecimal valorParcela = calcularValorParcela(pedido.formaPagamento(), parcelas, totalPedido, totalFinal);

        // 10. Crédito próxima compra e brinde
        BigDecimal creditoProximaCompra = calcularCredito(pedido.nivelClube(), subtotalProdutos);
        boolean brinde = calcularBrinde(pedido.nivelClube(), subtotalProdutos);

        return new ResumoDto(
                subtotalProdutos.doubleValue(),
                descontoCupom.doubleValue(),
                frete.doubleValue(),
                prazoEntrega(pedido.modalidadeEntrega()),
                seguro.doubleValue(),
                ajustePagamento.doubleValue(),
                totalFinal.doubleValue(),
                parcelas,
                valorParcela.doubleValue(),
                creditoProximaCompra.doubleValue(),
                brinde
        );
    }

    private void validarPedido(PedidoDto pedido) {
        // 1. Itens
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        // 2. Nível clube
        if (pedido.nivelClube() == null || !NIVEIS_VALIDOS.contains(pedido.nivelClube())) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Região
        if (pedido.regiao() == null || !REGIOES_VALIDAS.contains(pedido.regiao())) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        // 4. Modalidade entrega (existência)
        if (pedido.modalidadeEntrega() == null || !MODALIDADES_VALIDAS.contains(pedido.modalidadeEntrega())) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // 5. Modalidade entrega (disponibilidade)
        double pesoTotal = calcularPeso(pedido.itens());
        if ("MOTOBOY".equals(pedido.modalidadeEntrega()) && pesoTotal > 5.0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Cupom (existência)
        if (pedido.cupom() != null && !pedido.cupom().isBlank() && !CUPONS_VALIDOS.contains(pedido.cupom())) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        // 7. Cupom (aplicabilidade)
        if ("MENOS50".equals(pedido.cupom())) {
            BigDecimal subtotal = calcularSubtotal(pedido.itens());
            if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Forma de pagamento (existência)
        if (pedido.formaPagamento() == null || !FORMAS_PAGAMENTO_VALIDAS.contains(pedido.formaPagamento())) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Parcelamento
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        switch (pedido.formaPagamento()) {
            case "PIX", "BOLETO" -> {
                if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            case "CARTAO" -> {
                if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        // 10. Forma de pagamento (disponibilidade)
        if ("BOLETO".equals(pedido.formaPagamento())) {
            // Calcula total provisório sem ajuste de pagamento para verificar limite
            BigDecimal subtotal = calcularSubtotal(pedido.itens());
            BigDecimal descontoCupom = calcularDescontoCupomSimples(pedido.cupom(), subtotal, pedido.itens(), pedido.modalidadeEntrega());
            BigDecimal frete = calcularFrete(pedido.modalidadeEntrega(), pesoTotal, pedido.nivelClube());
            if ("FRETEGRATIS".equals(pedido.cupom()) && !"OURO".equals(pedido.nivelClube())) {
                frete = BigDecimal.ZERO;
            }
            BigDecimal seguro = calcularSeguro(subtotal, pedido.regiao());
            BigDecimal totalPedido = arredondar(subtotal.subtract(descontoCupom).add(frete).add(seguro));
            // Boleto adiciona R$ 3,49
            BigDecimal totalComBoleto = arredondar(totalPedido.add(new BigDecimal("3.49")));
            if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            BigDecimal preco = BigDecimal.valueOf(item.precoUnitario());
            BigDecimal qtd = BigDecimal.valueOf(item.quantidade());
            total = total.add(arredondar(preco.multiply(qtd)));
        }
        return total;
    }

    private double calcularPeso(List<ItemDto> itens) {
        double peso = 0;
        for (ItemDto item : itens) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemDto> itens, String modalidade) {
        if (cupom == null || cupom.isBlank()) return BigDecimal.ZERO;
        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> BigDecimal.ZERO; // tratado externamente
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularDescontoCupomSimples(String cupom, BigDecimal subtotal, List<ItemDto> itens, String modalidade) {
        return calcularDescontoCupom(cupom, subtotal, itens, modalidade);
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemDto> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int qtd = item.quantidade();
            int gratis = qtd / 3;
            if (gratis > 0) {
                BigDecimal preco = BigDecimal.valueOf(item.precoUnitario());
                desconto = desconto.add(arredondar(preco.multiply(BigDecimal.valueOf(gratis))));
            }
        }
        return desconto;
    }

    private BigDecimal calcularFreteNormal(String modalidade, double pesoKg) {
        return switch (modalidade) {
            case "ECONOMICA" -> arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(BigDecimal.valueOf(pesoKg))));
            case "EXPRESSA" -> arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(BigDecimal.valueOf(pesoKg))));
            case "RETIRADA_LOJA" -> BigDecimal.ZERO;
            case "MOTOBOY" -> new BigDecimal("18.00");
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularFrete(String modalidade, double pesoKg, String nivelClube) {
        if ("OURO".equals(nivelClube)) return BigDecimal.ZERO;
        return calcularFreteNormal(modalidade, pesoKg);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, String regiao) {
        double taxa = SEGURO_REGIAO.get(regiao);
        return arredondar(subtotalProdutos.multiply(BigDecimal.valueOf(taxa)));
    }

    private BigDecimal[] calcularAjustePagamento(String formaPagamento, int parcelas, BigDecimal totalPedido) {
        return switch (formaPagamento) {
            case "PIX" -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
                BigDecimal total = arredondar(totalPedido.subtract(desconto));
                yield new BigDecimal[]{desconto.negate(), total};
            }
            case "BOLETO" -> {
                BigDecimal tarifa = new BigDecimal("3.49");
                BigDecimal total = arredondar(totalPedido.add(tarifa));
                yield new BigDecimal[]{tarifa, total};
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    yield new BigDecimal[]{BigDecimal.ZERO, totalPedido};
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    // Price table: parcela = total * taxa / (1 - (1+taxa)^-n)
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                    // (1+taxa)^-n
                    BigDecimal fator = umMaisTaxa.pow(parcelas, new MathContext(20, HALF_EVEN));
                    BigDecimal fatorInverso = BigDecimal.ONE.divide(fator, 20, HALF_EVEN);
                    BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
                    BigDecimal parcela = arredondar(totalPedido.multiply(taxa).divide(denominador, 20, HALF_EVEN));
                    BigDecimal totalFinal = arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new BigDecimal[]{ajuste, totalFinal};
                }
            }
            default -> new BigDecimal[]{BigDecimal.ZERO, totalPedido};
        };
    }

    private BigDecimal calcularValorParcela(String formaPagamento, int parcelas, BigDecimal totalPedido, BigDecimal totalFinal) {
        if ("CARTAO".equals(formaPagamento) && parcelas > 3) {
            // Já foi calculado como totalFinal / parcelas arredondado
            return arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 20, HALF_EVEN));
        }
        return arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 20, HALF_EVEN));
    }

    private BigDecimal calcularCredito(String nivelClube, BigDecimal subtotalProdutos) {
        return switch (nivelClube) {
            case "PRATA" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
            case "OURO" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
            default -> BigDecimal.ZERO;
        };
    }

    private boolean calcularBrinde(String nivelClube, BigDecimal subtotalProdutos) {
        return "OURO".equals(nivelClube) && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }

    private int prazoEntrega(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> -1;
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(SCALE, HALF_EVEN);
    }
}
