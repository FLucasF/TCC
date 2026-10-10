package com.loja.checkout.service;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validar(request);

        List<Item> itens = request.getItens();
        ModalidadeEntrega modalidade = request.getModalidadeEntrega();
        String cupom = request.getCupom();
        FormaPagamento formaPagamento = request.getFormaPagamento();
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        NivelClube nivelClube = request.getNivelClube();
        Regiao regiao = request.getRegiao();

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, itens);

        BigDecimal frete = calcularFrete(modalidade, itens, nivelClube);
        int prazoEntrega = obterPrazoEntrega(modalidade);

        BigDecimal seguro = calcularSeguro(regiao, subtotalProdutos);

        BigDecimal totalAntesPagamento = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalAntesPagamento = ArredondamentoUtil.arredondar(totalAntesPagamento);

        BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);

        ValorAjustePagamento ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesPagamento, parcelas);

        BigDecimal totalFinal = totalAntesPagamento.add(ajustePagamento.ajuste);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas);

        boolean brinde = verificarBrinde(nivelClube, subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(prazoEntrega);
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento.ajuste);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validar(CheckoutRequest request) {
        validarPedido(request.getItens());
        validarNivelClube(request.getNivelClube());
        validarRegiao(request.getRegiao());
        validarModalidadeEntrega(request.getModalidadeEntrega());
        validarModalidadeEntregaDisponivel(request.getModalidadeEntrega(), request.getItens());
        validarCupom(request.getCupom(), request.getItens());
        validarFormaPagamento(request.getFormaPagamento());
        validarParcelamento(request.getFormaPagamento(), request.getParcelas());

        BigDecimal totalAntesPagamento = calcularSubtotalProdutos(request.getItens())
                .subtract(calcularDescontoCupom(request.getCupom(), calcularSubtotalProdutos(request.getItens()), request.getItens()))
                .add(calcularFrete(request.getModalidadeEntrega(), request.getItens(), request.getNivelClube()))
                .add(calcularSeguro(request.getRegiao(), calcularSubtotalProdutos(request.getItens())));
        totalAntesPagamento = ArredondamentoUtil.arredondar(totalAntesPagamento);

        validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalAntesPagamento);
    }

    private void validarPedido(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getPrecoUnitario() <= 0 || item.getQuantidade() <= 0 || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(NivelClube nivelClube) {
        if (nivelClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(Regiao regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidadeEntrega(ModalidadeEntrega modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeEntregaDisponivel(ModalidadeEntrega modalidade, List<Item> itens) {
        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            double pesoTotal = itens.stream().mapToDouble(item -> item.getPesoKg() * item.getQuantidade()).sum();
            if (pesoTotal > 5.0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private void validarCupom(String cupom, List<Item> itens) {
        if (cupom == null) {
            return;
        }

        if (!isCupomValido(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (cupom.equals("MENOS50")) {
            BigDecimal subtotal = calcularSubtotalProdutos(itens);
            if (subtotal.compareTo(BigDecimal.valueOf(300.0)) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, Integer parcelas) {
        int numParcelas = parcelas != null ? parcelas : 1;

        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (numParcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (numParcelas < 1 || numParcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalAntesPagamento) {
        if (formaPagamento == FormaPagamento.BOLETO && totalAntesPagamento.compareTo(BigDecimal.valueOf(1000.0)) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal preco = BigDecimal.valueOf(item.getPrecoUnitario() * item.getQuantidade());
            subtotal = subtotal.add(preco);
        }
        return ArredondamentoUtil.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos, List<Item> itens) {
        if (cupom == null) {
            return ArredondamentoUtil.arredondar(BigDecimal.ZERO);
        }

        return switch (cupom) {
            case "BEMVINDO10" -> ArredondamentoUtil.arredondar(
                    subtotalProdutos.multiply(BigDecimal.valueOf(0.10))
            );
            case "MENOS50" -> ArredondamentoUtil.arredondar(BigDecimal.valueOf(50.0));
            case "FRETEGRATIS" -> ArredondamentoUtil.arredondar(BigDecimal.ZERO);
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> ArredondamentoUtil.arredondar(BigDecimal.ZERO);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<Item> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int quantidadeGratis = item.getQuantidade() / 3;
            BigDecimal valorItemGratis = BigDecimal.valueOf(item.getPrecoUnitario() * quantidadeGratis);
            desconto = desconto.add(valorItemGratis);
        }
        return ArredondamentoUtil.arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<Item> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return ArredondamentoUtil.arredondar(BigDecimal.ZERO);
        }

        double pesoTotal = itens.stream().mapToDouble(item -> item.getPesoKg() * item.getQuantidade()).sum();

        BigDecimal frete = switch (modalidade) {
            case ECONOMICA -> BigDecimal.valueOf(12.0).add(BigDecimal.valueOf(2.0 * pesoTotal));
            case EXPRESSA -> BigDecimal.valueOf(25.0).add(BigDecimal.valueOf(4.5 * pesoTotal));
            case RETIRADA_LOJA -> ArredondamentoUtil.arredondar(BigDecimal.ZERO);
            case MOTOBOY -> BigDecimal.valueOf(18.0);
        };

        return ArredondamentoUtil.arredondar(frete);
    }

    private int obterPrazoEntrega(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> 7;
            case EXPRESSA -> 2;
            case RETIRADA_LOJA -> 1;
            case MOTOBOY -> 0;
        };
    }

    private BigDecimal calcularSeguro(Regiao regiao, BigDecimal subtotalProdutos) {
        double percentual = switch (regiao) {
            case SUDESTE, SUL -> 0.01;
            case CENTRO_OESTE -> 0.015;
            case NORTE -> 0.025;
            case NORDESTE -> 0.02;
        };

        return ArredondamentoUtil.arredondar(
                subtotalProdutos.multiply(BigDecimal.valueOf(percentual))
        );
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        double percentual = switch (nivelClube) {
            case BRONZE -> 0.0;
            case PRATA -> 0.02;
            case OURO -> 0.05;
        };

        return ArredondamentoUtil.arredondar(
                subtotalProdutos.multiply(BigDecimal.valueOf(percentual))
        );
    }

    private ValorAjustePagamento calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal totalAntesPagamento, int parcelas) {
        BigDecimal ajuste = switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = ArredondamentoUtil.arredondar(
                        totalAntesPagamento.multiply(BigDecimal.valueOf(0.05))
                );
                yield desconto.negate();
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    yield ArredondamentoUtil.arredondar(BigDecimal.ZERO);
                } else {
                    double taxaMensal = 0.0199;
                    BigDecimal numerador = BigDecimal.valueOf(taxaMensal);
                    BigDecimal denominador = BigDecimal.ONE.subtract(
                            BigDecimal.valueOf(Math.pow(1 + taxaMensal, -parcelas))
                    );
                    BigDecimal parcela = totalAntesPagamento.multiply(numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN));
                    parcela = ArredondamentoUtil.arredondar(parcela);
                    BigDecimal total = parcela.multiply(BigDecimal.valueOf(parcelas));
                    yield ArredondamentoUtil.arredondar(total.subtract(totalAntesPagamento));
                }
            }
            case BOLETO -> ArredondamentoUtil.arredondar(BigDecimal.valueOf(3.49));
            default -> ArredondamentoUtil.arredondar(BigDecimal.ZERO);
        };

        return new ValorAjustePagamento(ajuste);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return ArredondamentoUtil.arredondar(
                totalFinal.divide(BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN)
        );
    }

    private boolean verificarBrinde(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        return nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(BigDecimal.valueOf(500.0)) > 0;
    }

    private boolean isCupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") ||
               cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") ||
               cupom.equals("LEVE3PAGUE2");
    }

    private static class ValorAjustePagamento {
        BigDecimal ajuste;

        ValorAjustePagamento(BigDecimal ajuste) {
            this.ajuste = ajuste;
        }
    }
}
