package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarPedido(request);

        NivelClube nivelClube = validarNivelClube(request.getNivelClube());
        Regiao regiao = validarRegiao(request.getRegiao());
        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.getModalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());
        validarModalidadeDisponivel(modalidade, pesoTotal);

        validarCupom(request.getCupom());
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());
        validarCupomAplicavel(request.getCupom(), subtotalProdutos);

        FormaPagamento formaPagamento = validarFormaPagamento(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, subtotalProdutos, request.getCupom());

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), subtotalProdutos, request.getItens());
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube, request.getCupom(), descontoCupom);
        BigDecimal seguro = calcularSeguro(regiao, subtotalProdutos);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        Map<String, BigDecimal> pagamentoInfo = calcularPagamento(formaPagamento, parcelas, totalPedido);
        BigDecimal totalFinal = pagamentoInfo.get("totalFinal");
        BigDecimal valorParcela = pagamentoInfo.get("valorParcela");
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        ResumoResponse response = new ResumoResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazoDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(ResumoRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemDto item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null || nivel.isEmpty()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupom) {
        if (cupom != null && !cupom.isEmpty()) {
            if (!cupom.equals("BEMVINDO10") && !cupom.equals("MENOS50") &&
                !cupom.equals("FRETEGRATIS") && !cupom.equals("LEVE3PAGUE2")) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }
    }

    private void validarCupomAplicavel(String cupom, BigDecimal subtotalProdutos) {
        if ("MENOS50".equals(cupom)) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento,
                                                  BigDecimal subtotalProdutos, String cupom) {
        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal descontoCupom = BigDecimal.ZERO;
            if ("BEMVINDO10".equals(cupom)) {
                descontoCupom = arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            } else if ("MENOS50".equals(cupom)) {
                descontoCupom = new BigDecimal("50.00");
            }

            BigDecimal totalSemFreteSeguro = subtotalProdutos.subtract(descontoCupom);
            if (totalSemFreteSeguro.compareTo(LIMITE_BOLETO) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            BigDecimal valorItem = BigDecimal.valueOf(item.getPrecoUnitario())
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemDto> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            BigDecimal pesoItem = BigDecimal.valueOf(item.getPesoKg())
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotalProdutos,
                                             java.util.List<ItemDto> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return new BigDecimal("0.00");
        }

        return switch (cupom) {
            case "BEMVINDO10" -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> new BigDecimal("0.00");
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(java.util.List<ItemDto> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal precoUnitario = BigDecimal.valueOf(item.getPrecoUnitario());
            BigDecimal desconto = precoUnitario.multiply(BigDecimal.valueOf(unidadesGratis));
            descontoTotal = descontoTotal.add(desconto);
        }
        return arredondar(descontoTotal);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal,
                                     NivelClube nivelClube, String cupom, BigDecimal descontoCupom) {
        if (nivelClube.isFreteGratis()) {
            return new BigDecimal("0.00");
        }

        BigDecimal frete = arredondar(modalidade.calcularFrete(pesoTotal));

        if ("FRETEGRATIS".equals(cupom)) {
            return new BigDecimal("0.00");
        }

        return frete;
    }

    private BigDecimal calcularSeguro(Regiao regiao, BigDecimal subtotalProdutos) {
        return arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
    }

    private Map<String, BigDecimal> calcularPagamento(FormaPagamento formaPagamento,
                                                      int parcelas, BigDecimal totalPedido) {
        Map<String, BigDecimal> resultado = new HashMap<>();

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = arredondar(totalPedido.multiply(DESCONTO_PIX));
                totalFinal = totalPedido.subtract(desconto);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO);
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS) {
                    totalFinal = totalPedido;
                    valorParcela = arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                } else {
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
                    BigDecimal potencia = umMaisTaxa.pow(parcelas);
                    BigDecimal fatorPrice = TAXA_JUROS_MENSAL.divide(
                        BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN)),
                        10, RoundingMode.HALF_EVEN
                    );
                    valorParcela = arredondar(totalPedido.multiply(fatorPrice));
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                }
            }
            default -> {
                totalFinal = totalPedido;
                valorParcela = totalPedido;
            }
        }

        resultado.put("totalFinal", totalFinal);
        resultado.put("valorParcela", valorParcela);
        return resultado;
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotalProdutos) {
        return arredondar(subtotalProdutos.multiply(nivelClube.getPercentualCredito()));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
