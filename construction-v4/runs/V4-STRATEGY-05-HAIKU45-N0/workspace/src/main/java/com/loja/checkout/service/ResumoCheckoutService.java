package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResumoCheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        String erro = validar(request);
        if (erro != null) {
            return ResumoResponse.erro(erro);
        }

        ResumoResponse response = new ResumoResponse();

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        BigDecimal subtotal = calcularSubtotal(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        BigDecimal freteBase = calcularFrete(request.getModalidadeEntrega(), pesoTotal, request.getNivelClube());
        BigDecimal desconto = calcularDesconto(request.getCupom(), request.getItens(), subtotal, pesoTotal, request.getModalidadeEntrega(), freteBase);
        BigDecimal frete = "FRETEGRATIS".equals(request.getCupom()) ? BigDecimal.ZERO : freteBase;
        int prazoDias = obterPrazoDias(request.getModalidadeEntrega());
        BigDecimal seguro = calcularSeguro(subtotal, request.getRegiao());

        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        total = arredondar(total);

        AjusteEParcela ajusteEParcela = calcularAjusteEParcela(request.getFormaPagamento(), total, parcelas);
        BigDecimal ajuste = ajusteEParcela.ajuste;
        BigDecimal valorParcela = ajusteEParcela.parcela;

        BigDecimal totalFinal = total.add(ajuste);
        totalFinal = arredondar(totalFinal);

        BigDecimal credito = calcularCredito(request.getNivelClube(), subtotal);
        Boolean brinde = calcularBrinde(request.getNivelClube(), subtotal);

        response.setSubtotalProdutos(arredondar(subtotal));
        response.setDescontoCupom(arredondar(desconto));
        response.setFrete(arredondar(frete));
        response.setPrazoEntregaDias(prazoDias);
        response.setSeguro(arredondar(seguro));
        response.setAjustePagamento(arredondar(ajuste));
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(arredondar(valorParcela));
        response.setCreditoProximaCompra(arredondar(credito));
        response.setBrinde(brinde);

        return response;
    }

    private String validar(ResumoRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        if (request.getNivelClube() == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (request.getRegiao() == null) {
            return "REGIAO_INVALIDA";
        }

        if (request.getModalidadeEntrega() == null) {
            return "MODALIDADE_INVALIDA";
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        if (request.getModalidadeEntrega() == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            String erroCupom = validarCupom(request.getCupom(), calcularSubtotal(request.getItens()));
            if (erroCupom != null) {
                return erroCupom;
            }
        }

        if (request.getFormaPagamento() == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        String erroParcelamento = validarParcelamento(request.getFormaPagamento(), parcelas);
        if (erroParcelamento != null) {
            return erroParcelamento;
        }

        BigDecimal subtotal = calcularSubtotal(request.getItens());
        BigDecimal freteBase = calcularFrete(request.getModalidadeEntrega(), pesoTotal, request.getNivelClube());
        BigDecimal desconto = calcularDesconto(request.getCupom(), request.getItens(), subtotal, pesoTotal, request.getModalidadeEntrega(), freteBase);
        BigDecimal frete = "FRETEGRATIS".equals(request.getCupom()) ? BigDecimal.ZERO : freteBase;
        BigDecimal seguro = calcularSeguro(subtotal, request.getRegiao());
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        total = arredondar(total);

        if (request.getFormaPagamento() == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000")) > 0) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private String validarCupom(String cupom, BigDecimal subtotal) {
        Set<String> cupomValidos = new HashSet<>();
        cupomValidos.add("BEMVINDO10");
        cupomValidos.add("MENOS50");
        cupomValidos.add("FRETEGRATIS");
        cupomValidos.add("LEVE3PAGUE2");

        if (!cupomValidos.contains(cupom)) {
            return "CUPOM_INVALIDO";
        }

        if ("MENOS50".equals(cupom) && subtotal.compareTo(new BigDecimal("300")) < 0) {
            return "CUPOM_NAO_APLICAVEL";
        }

        return null;
    }

    private String validarParcelamento(FormaPagamento forma, int parcelas) {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                return "PARCELAMENTO_INVALIDO";
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                return "PARCELAMENTO_INVALIDO";
            }
        }
        return null;
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal itemTotal = item.getPrecoUnitario()
                    .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDesconto(String cupom, List<Item> itens, BigDecimal subtotal, BigDecimal pesoTotal, ModalidadeEntrega modalidade, BigDecimal freteBase) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if ("BEMVINDO10".equals(cupom)) {
            return arredondar(subtotal.multiply(new BigDecimal("0.10")));
        } else if ("MENOS50".equals(cupom)) {
            return new BigDecimal("50.00");
        } else if ("FRETEGRATIS".equals(cupom)) {
            return freteBase;
        } else if ("LEVE3PAGUE2".equals(cupom)) {
            return calcularDescontoLeve3Pague2(itens);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(new BigDecimal(unidadesGratis));
                desconto = desconto.add(descontoItem);
            }
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivel) {
        if (nivel == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        return switch (modalidade) {
            case ECONOMICA -> arredondar(new BigDecimal("12").add(pesoTotal.multiply(new BigDecimal("2"))));
            case EXPRESSA -> arredondar(new BigDecimal("25").add(pesoTotal.multiply(new BigDecimal("4.50"))));
            case RETIRADA_LOJA -> BigDecimal.ZERO;
            case MOTOBOY -> new BigDecimal("18.00");
        };
    }

    private int obterPrazoDias(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> 7;
            case EXPRESSA -> 2;
            case RETIRADA_LOJA -> 1;
            case MOTOBOY -> 0;
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = switch (regiao) {
            case SUDESTE, SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };
        return arredondar(subtotal.multiply(percentual));
    }

    private AjusteEParcela calcularAjusteEParcela(FormaPagamento forma, BigDecimal total, int parcelas) {
        return switch (forma) {
            case PIX -> {
                BigDecimal desconto = arredondar(total.multiply(new BigDecimal("-0.05")));
                BigDecimal totalComDesconto = total.add(desconto);
                yield new AjusteEParcela(desconto, arredondar(totalComDesconto.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN)));
            }
            case BOLETO -> new AjusteEParcela(new BigDecimal("3.49"), arredondar(total.add(new BigDecimal("3.49")).divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN)));
            case CARTAO -> {
                if (parcelas <= 3) {
                    yield new AjusteEParcela(BigDecimal.ZERO, arredondar(total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN)));
                } else {
                    double taxaMensal = 0.02;
                    double totalDouble = total.doubleValue();

                    double numerador = totalDouble * taxaMensal;
                    double denominador = 1.0 - Math.pow(1.0 + taxaMensal, -parcelas);
                    double parcelaDouble = numerador / denominador;

                    BigDecimal parcela = new BigDecimal(parcelaDouble);
                    parcela = arredondar(parcela);
                    BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
                    totalComJuros = arredondar(totalComJuros);
                    BigDecimal ajuste = arredondar(totalComJuros.subtract(total));
                    yield new AjusteEParcela(ajuste, parcela);
                }
            }
        };
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotal) {
        return switch (nivel) {
            case BRONZE -> BigDecimal.ZERO;
            case PRATA -> arredondar(subtotal.multiply(new BigDecimal("0.02")));
            case OURO -> arredondar(subtotal.multiply(new BigDecimal("0.05")));
        };
    }

    private Boolean calcularBrinde(NivelClube nivel, BigDecimal subtotal) {
        if (nivel == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500")) >= 0) {
            return true;
        }
        return false;
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
        }
        return peso;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static class AjusteEParcela {
        BigDecimal ajuste;
        BigDecimal parcela;

        AjusteEParcela(BigDecimal ajuste, BigDecimal parcela) {
            this.ajuste = ajuste;
            this.parcela = parcela;
        }
    }
}
