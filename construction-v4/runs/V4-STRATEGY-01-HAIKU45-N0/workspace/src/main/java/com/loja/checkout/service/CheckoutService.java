package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.dto.RespostaResumo;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal BD_100 = new BigDecimal("100");
    private static final BigDecimal BD_5 = new BigDecimal("5");
    private static final BigDecimal BD_3_49 = new BigDecimal("3.49");
    private static final BigDecimal BD_1_0199 = new BigDecimal("1.0199");

    public Object calcularResumo(RequisicaoResumo requisicao) {
        Object erro = validarRequisicao(requisicao);
        if (erro instanceof com.loja.checkout.dto.RespostaErro) {
            return erro;
        }

        try {
            List<ItemCarrinho> itens = requisicao.getItens();
            String modalidadeStr = requisicao.getModalidadeEntrega();
            String cupomStr = requisicao.getCupom();
            String formaPagamentoStr = requisicao.getFormaPagamento();
            Integer parcelas = requisicao.getParcelas();
            NivelClube nivelClube = NivelClube.valueOf(requisicao.getNivelClube());
            Regiao regiao = Regiao.valueOf(requisicao.getRegiao());
            ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeStr);
            FormaPagamento formaPagamento = FormaPagamento.valueOf(formaPagamentoStr);

            BigDecimal subtotalProdutos = calcularSubtotal(itens);
            BigDecimal descontoCupom = calcularDescontoCupom(cupomStr, subtotalProdutos, modalidade, itens);
            BigDecimal frete = calcularFrete(modalidade, itens, nivelClube);
            Integer prazoEntrega = modalidade.getPrazo();
            BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
            BigDecimal totalAntesAjuste = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
            BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalAntesAjuste, parcelas);
            BigDecimal totalFinal = totalAntesAjuste.add(ajustePagamento);
            BigDecimal creditoProximaCompra = calcularCredito(nivelClube, subtotalProdutos);
            Boolean brinde = validarBrinde(nivelClube, subtotalProdutos);

            BigDecimal valorParcela;
            if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
                valorParcela = calcularParcelaComJuros(totalAntesAjuste, parcelas);
            } else {
                valorParcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
            }

            return new RespostaResumo(
                subtotalProdutos, descontoCupom, frete, prazoEntrega, seguro,
                ajustePagamento, totalFinal, parcelas, valorParcela,
                creditoProximaCompra, brinde
            );
        } catch (Exception e) {
            return new com.loja.checkout.dto.RespostaErro("PEDIDO_INVALIDO");
        }
    }

    private Object validarRequisicao(RequisicaoResumo requisicao) {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            return new com.loja.checkout.dto.RespostaErro("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                return new com.loja.checkout.dto.RespostaErro("PEDIDO_INVALIDO");
            }
        }

        try {
            NivelClube.valueOf(requisicao.getNivelClube());
        } catch (Exception e) {
            return new com.loja.checkout.dto.RespostaErro("NIVEL_CLUBE_INVALIDO");
        }

        try {
            Regiao.valueOf(requisicao.getRegiao());
        } catch (Exception e) {
            return new com.loja.checkout.dto.RespostaErro("REGIAO_INVALIDA");
        }

        try {
            ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
        } catch (Exception e) {
            return new com.loja.checkout.dto.RespostaErro("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
        Double pesoTotal = requisicao.getItens().stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();

        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal > 5) {
            return new com.loja.checkout.dto.RespostaErro("MODALIDADE_INDISPONIVEL");
        }

        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            if (!isCupomValido(requisicao.getCupom())) {
                return new com.loja.checkout.dto.RespostaErro("CUPOM_INVALIDO");
            }

            BigDecimal subtotal = calcularSubtotal(requisicao.getItens());
            if (!isCupomAplicavel(requisicao.getCupom(), subtotal, modalidade, requisicao.getItens())) {
                return new com.loja.checkout.dto.RespostaErro("CUPOM_NAO_APLICAVEL");
            }
        }

        try {
            FormaPagamento.valueOf(requisicao.getFormaPagamento());
        } catch (Exception e) {
            return new com.loja.checkout.dto.RespostaErro("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = requisicao.getParcelas();
        FormaPagamento formaPagamento = FormaPagamento.valueOf(requisicao.getFormaPagamento());

        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                return new com.loja.checkout.dto.RespostaErro("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                return new com.loja.checkout.dto.RespostaErro("PARCELAMENTO_INVALIDO");
            }
        }

        BigDecimal totalAntesAjuste = calcularSubtotal(requisicao.getItens())
            .subtract(calcularDescontoCupom(requisicao.getCupom(), calcularSubtotal(requisicao.getItens()), modalidade, requisicao.getItens()))
            .add(calcularFrete(modalidade, requisicao.getItens(), NivelClube.valueOf(requisicao.getNivelClube())))
            .add(calcularSeguro(calcularSubtotal(requisicao.getItens()), Regiao.valueOf(requisicao.getRegiao())));

        if (formaPagamento == FormaPagamento.BOLETO && totalAntesAjuste.compareTo(new BigDecimal("1000")) > 0) {
            return new com.loja.checkout.dto.RespostaErro("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return null;
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal precoUnitario = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            BigDecimal itemTotal = precoUnitario.multiply(quantidade);
            subtotal = subtotal.add(itemTotal);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, ModalidadeEntrega modalidade, List<ItemCarrinho> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return arredondar(BigDecimal.ZERO);
        }

        if ("BEMVINDO10".equals(cupom)) {
            BigDecimal desconto = subtotal.multiply(new BigDecimal("0.10"));
            return arredondar(desconto);
        }

        if ("MENOS50".equals(cupom)) {
            if (subtotal.compareTo(new BigDecimal("300")) >= 0) {
                return arredondar(new BigDecimal("50"));
            }
        }

        if ("FRETEGRATIS".equals(cupom)) {
            BigDecimal frete = calcularFrete(modalidade, itens, NivelClube.BRONZE);
            return arredondar(frete);
        }

        if ("LEVE3PAGUE2".equals(cupom)) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemCarrinho item : itens) {
                int quantidade = item.getQuantidade();
                int gratis = quantidade / 3;
                BigDecimal precoUnitario = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
                BigDecimal descontoItem = precoUnitario.multiply(new BigDecimal(gratis));
                desconto = desconto.add(descontoItem);
            }
            return arredondar(desconto);
        }

        return arredondar(BigDecimal.ZERO);
    }

    private boolean isCupomValido(String cupom) {
        return "BEMVINDO10".equals(cupom) || "MENOS50".equals(cupom) ||
               "FRETEGRATIS".equals(cupom) || "LEVE3PAGUE2".equals(cupom);
    }

    private boolean isCupomAplicavel(String cupom, BigDecimal subtotal, ModalidadeEntrega modalidade, List<ItemCarrinho> itens) {
        if ("MENOS50".equals(cupom)) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }
        return true;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemCarrinho> itens, NivelClube nivelClube) {
        if (nivelClube == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }

        Double pesoTotal = itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();

        BigDecimal taxaBase = new BigDecimal(String.valueOf(modalidade.getTaxaBase()));
        BigDecimal taxaPorKg = new BigDecimal(String.valueOf(modalidade.getTaxaPorKg()));
        BigDecimal peso = new BigDecimal(String.valueOf(pesoTotal));

        BigDecimal frete = taxaBase.add(taxaPorKg.multiply(peso));
        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = regiao.getPercentualSeguro();
        BigDecimal seguro = subtotal.multiply(percentual);
        return arredondar(seguro);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, Integer parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
            return arredondar(desconto).negate();
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            return arredondar(BD_3_49);
        }

        if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal juros = calcularJuros(total, taxa, parcelas);
            return arredondar(juros);
        }

        return arredondar(BigDecimal.ZERO);
    }

    private BigDecimal calcularJuros(BigDecimal total, BigDecimal taxaMensal, Integer parcelas) {
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxaMensal);
        BigDecimal expoente = BigDecimal.ONE.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = um.subtract(umMaisTaxa.pow(-parcelas, new java.math.MathContext(50)));
        BigDecimal parcela = total.multiply(taxaMensal).divide(denominador, 20, RoundingMode.HALF_EVEN);
        BigDecimal parcelaArredondada = arredondar(parcela);
        BigDecimal totalComJuros = parcelaArredondada.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(total);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, Integer parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal denominador = BigDecimal.ONE.subtract(umMaisTaxa.pow(-parcelas, new java.math.MathContext(50)));
        BigDecimal parcela = total.multiply(taxa).divide(denominador, 20, RoundingMode.HALF_EVEN);
        return arredondar(parcela);
    }

    private BigDecimal calcularCredito(NivelClube nivelClube, BigDecimal subtotal) {
        if (nivelClube == NivelClube.BRONZE) {
            return arredondar(BigDecimal.ZERO);
        }

        BigDecimal percentual;
        if (nivelClube == NivelClube.PRATA) {
            percentual = new BigDecimal("0.02");
        } else {
            percentual = new BigDecimal("0.05");
        }

        BigDecimal credito = subtotal.multiply(percentual);
        return arredondar(credito);
    }

    private Boolean validarBrinde(NivelClube nivelClube, BigDecimal subtotal) {
        return nivelClube == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500")) > 0;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return new BigDecimal("0.00");
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
