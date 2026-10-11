package com.loja.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutService {

    private static final Set<String> NIVEIS_CLUBE = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");
    private static final Set<String> FORMAS_PAGAMENTO = Set.of("PIX", "CARTAO", "BOLETO");

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());

        BigDecimal descontoCupom = calcularDescontoCupom(pedido, subtotalProdutos, pesoTotal);

        ModalidadeEntregaInfo modalidadeInfo = calcularFrete(pedido.getModalidadeEntrega(), pesoTotal, pedido.getNivelClube());
        BigDecimal frete = modalidadeInfo.valor();
        Integer prazoEntregaDias = modalidadeInfo.prazo();

        if ("FRETEGRATIS".equals(pedido.getCupom())) {
            descontoCupom = frete;
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, pedido.getRegiao());

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int numeroParcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
        PagamentoInfo pagamentoInfo = calcularPagamento(pedido.getFormaPagamento(), numeroParcelas, totalPedido);

        BigDecimal creditoProximaCompra = calcularCreditoClube(pedido.getNivelClube(), subtotalProdutos);
        Boolean brinde = "OURO".equals(pedido.getNivelClube()) && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(prazoEntregaDias);
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(pagamentoInfo.ajuste());
        resposta.setTotalFinal(pagamentoInfo.totalFinal());
        resposta.setParcelas(numeroParcelas);
        resposta.setValorParcela(pagamentoInfo.valorParcela());
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (pedido.getNivelClube() == null || !NIVEIS_CLUBE.contains(pedido.getNivelClube())) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (pedido.getRegiao() == null || !REGIOES.contains(pedido.getRegiao())) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (pedido.getModalidadeEntrega() == null || !MODALIDADES.contains(pedido.getModalidadeEntrega())) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(pedido.getItens());
        if ("MOTOBOY".equals(pedido.getModalidadeEntrega()) && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (pedido.getCupom() != null && !pedido.getCupom().isEmpty()) {
            if (!CUPONS.contains(pedido.getCupom())) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            if ("MENOS50".equals(pedido.getCupom())) {
                BigDecimal subtotal = calcularSubtotalProdutos(pedido.getItens());
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
            }
        }

        if (pedido.getFormaPagamento() == null || !FORMAS_PAGAMENTO.contains(pedido.getFormaPagamento())) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int numeroParcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
        if ("PIX".equals(pedido.getFormaPagamento()) || "BOLETO".equals(pedido.getFormaPagamento())) {
            if (numeroParcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if ("CARTAO".equals(pedido.getFormaPagamento())) {
            if (numeroParcelas < 1 || numeroParcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if ("BOLETO".equals(pedido.getFormaPagamento())) {
            BigDecimal subtotal = calcularSubtotalProdutos(pedido.getItens());
            BigDecimal descontoCupom = calcularDescontoCupomParcial(pedido, subtotal);
            ModalidadeEntregaInfo modalidadeInfo = calcularFrete(pedido.getModalidadeEntrega(), pesoTotal, pedido.getNivelClube());
            BigDecimal frete = modalidadeInfo.valor();
            if ("FRETEGRATIS".equals(pedido.getCupom())) {
                descontoCupom = frete;
            }
            BigDecimal seguro = calcularSeguro(subtotal, pedido.getRegiao());
            BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro).add(new BigDecimal("3.49"));

            if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(PedidoRequest pedido, BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
        if (pedido.getCupom() == null || pedido.getCupom().isEmpty()) {
            return new BigDecimal("0.00");
        }

        return switch (pedido.getCupom()) {
            case "BEMVINDO10" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> new BigDecimal("0.00");
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(pedido.getItens());
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal calcularDescontoCupomParcial(PedidoRequest pedido, BigDecimal subtotalProdutos) {
        if (pedido.getCupom() == null || pedido.getCupom().isEmpty()) {
            return new BigDecimal("0.00");
        }

        return switch (pedido.getCupom()) {
            case "BEMVINDO10" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> new BigDecimal("0.00");
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(pedido.getItens());
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int quantidadeGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratis));
            desconto = desconto.add(descontoItem);
        }
        return arredondar(desconto);
    }

    private ModalidadeEntregaInfo calcularFrete(String modalidade, BigDecimal pesoTotal, String nivelClube) {
        if ("OURO".equals(nivelClube)) {
            return switch (modalidade) {
                case "ECONOMICA" -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 7);
                case "EXPRESSA" -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 2);
                case "RETIRADA_LOJA" -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 1);
                case "MOTOBOY" -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 0);
                default -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 0);
            };
        }

        return switch (modalidade) {
            case "ECONOMICA" -> {
                BigDecimal valor = new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotal));
                yield new ModalidadeEntregaInfo(arredondar(valor), 7);
            }
            case "EXPRESSA" -> {
                BigDecimal valor = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotal));
                yield new ModalidadeEntregaInfo(arredondar(valor), 2);
            }
            case "RETIRADA_LOJA" -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 1);
            case "MOTOBOY" -> new ModalidadeEntregaInfo(new BigDecimal("18.00"), 0);
            default -> new ModalidadeEntregaInfo(new BigDecimal("0.00"), 0);
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, String regiao) {
        BigDecimal percentual = switch (regiao) {
            case "SUDESTE", "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> new BigDecimal("0.00");
        };
        return arredondar(subtotalProdutos.multiply(percentual));
    }

    private PagamentoInfo calcularPagamento(String formaPagamento, int numeroParcelas, BigDecimal totalPedido) {
        return switch (formaPagamento) {
            case "PIX" -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
                BigDecimal totalFinal = totalPedido.subtract(desconto);
                BigDecimal ajuste = totalFinal.subtract(totalPedido);
                yield new PagamentoInfo(ajuste, totalFinal, totalFinal);
            }
            case "BOLETO" -> {
                BigDecimal tarifa = new BigDecimal("3.49");
                BigDecimal totalFinal = totalPedido.add(tarifa);
                BigDecimal ajuste = totalFinal.subtract(totalPedido);
                yield new PagamentoInfo(ajuste, totalFinal, totalFinal);
            }
            case "CARTAO" -> {
                if (numeroParcelas <= 3) {
                    BigDecimal valorParcela = arredondar(totalPedido.divide(new BigDecimal(numeroParcelas), 10, RoundingMode.HALF_EVEN));
                    BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(numeroParcelas));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new PagamentoInfo(ajuste, totalFinal, valorParcela);
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                    BigDecimal denominador = BigDecimal.ONE.subtract(
                        umMaisTaxa.pow(-numeroParcelas, new java.math.MathContext(10, RoundingMode.HALF_EVEN))
                    );
                    BigDecimal valorParcela = arredondar(totalPedido.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN));
                    BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(numeroParcelas));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new PagamentoInfo(ajuste, totalFinal, valorParcela);
                }
            }
            default -> new PagamentoInfo(new BigDecimal("0.00"), totalPedido, totalPedido);
        };
    }

    private BigDecimal calcularCreditoClube(String nivelClube, BigDecimal subtotalProdutos) {
        return switch (nivelClube) {
            case "PRATA" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
            case "OURO" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private record ModalidadeEntregaInfo(BigDecimal valor, Integer prazo) {}
    private record PagamentoInfo(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
