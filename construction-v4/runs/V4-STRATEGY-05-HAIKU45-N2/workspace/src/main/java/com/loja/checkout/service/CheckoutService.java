package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.util.Arredondador;

@Service
public class CheckoutService {

    public RespostaCheckout calcularResumo(RequisicaoCheckout requisicao) {
        // Validações na ordem especificada
        validarPedido(requisicao.getItens());
        NivelClube nivelClube = validarNivelClube(requisicao.getNivelClube());
        Regiao regiao = validarRegiao(requisicao.getRegiao());
        ModalidadeEntrega modalidade = validarModalidade(requisicao.getModalidadeEntrega());
        FormaPagamento formaPagamento = validarFormaPagamento(requisicao.getFormaPagamento());

        // Validar parcelas
        Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);

        // Calcular subtotal
        BigDecimal subtotalProdutos = calcularSubtotal(requisicao.getItens());

        // Validar e calcular cupom
        Cupom cupom = null;
        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            cupom = validarCupom(requisicao.getCupom());
            if (!cupom.ehAplicavel(subtotalProdutos, calcularPesoTotal(requisicao.getItens()), requisicao.getItens())) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }

        // Calcular peso total
        BigDecimal pesoTotal = calcularPesoTotal(requisicao.getItens());

        // Validar se modalidade está disponível
        if (!modalidade.ehDisponivel(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        // Calcular frete
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);

        // Calcular desconto do cupom
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, requisicao.getItens(), frete);
        }

        // Arredondar desconto do cupom
        descontoCupom = Arredondador.arredondar(descontoCupom);

        // Calcular seguro
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        // Calcular total da compra antes do ajuste de pagamento
        BigDecimal total = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
        total = Arredondador.arredondar(total);

        // Validar forma de pagamento
        if (formaPagamento == FormaPagamento.BOLETO && total.compareTo(BigDecimal.valueOf(1000.00)) > 0) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Calcular valor final e parcela
        BigDecimal totalFinal;
        BigDecimal valorParcela;
        BigDecimal ajustePagamento;

        if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
            valorParcela = calcularParcelaComJuros(total, parcelas);
            valorParcela = Arredondador.arredondar(valorParcela);
            totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            totalFinal = Arredondador.arredondar(totalFinal);
            ajustePagamento = Arredondador.arredondar(totalFinal.subtract(total));
        } else if (formaPagamento == FormaPagamento.PIX) {
            ajustePagamento = total.multiply(new BigDecimal("0.05")).negate();
            ajustePagamento = Arredondador.arredondar(ajustePagamento);
            totalFinal = total.add(ajustePagamento);
            totalFinal = Arredondador.arredondar(totalFinal);
            valorParcela = totalFinal;
        } else if (formaPagamento == FormaPagamento.BOLETO) {
            ajustePagamento = new BigDecimal("3.49");
            totalFinal = total.add(ajustePagamento);
            totalFinal = Arredondador.arredondar(totalFinal);
            valorParcela = totalFinal;
        } else {
            ajustePagamento = Arredondador.arredondar(BigDecimal.ZERO);
            totalFinal = total;
            valorParcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
            valorParcela = Arredondador.arredondar(valorParcela);
        }

        // Calcular crédito da próxima compra
        BigDecimal creditoProximaCompra = nivelClube.getTaxaCredito()
            .multiply(subtotalProdutos)
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        creditoProximaCompra = Arredondador.arredondar(creditoProximaCompra);

        // Determinar se tem brinde
        boolean temBrinde = nivelClube.isTemBrinde() && subtotalProdutos.compareTo(BigDecimal.valueOf(500.00)) > 0;

        return new RespostaCheckout(
            Arredondador.arredondar(subtotalProdutos),
            descontoCupom,
            Arredondador.arredondar(frete),
            modalidade.getPrazo(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            temBrinde
        );
    }

    private void validarPedido(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null || nivel.isEmpty()) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null || regiao.isEmpty()) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String modalidade) {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
    }

    private FormaPagamento validarFormaPagamento(String forma) {
        if (forma == null || forma.isEmpty()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(forma.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private Cupom validarCupom(String codigo) {
        try {
            return Cupom.valueOf(codigo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
    }

    private void validarParcelamento(FormaPagamento forma, Integer parcelas) {
        if (!forma.ehParcelamentoValido(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal preco = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(preco);
        }
        return Arredondador.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivel) {
        if (nivel.isSemFrete()) {
            return BigDecimal.ZERO;
        }
        return Arredondador.arredondar(modalidade.calcularFrete(pesoTotal));
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        return Arredondador.arredondar(subtotal.multiply(regiao.getPercentualSeguro()));
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, Integer parcelas) {
        java.math.MathContext mc = new java.math.MathContext(50);
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal taxaMais1 = BigDecimal.ONE.add(taxa);
        BigDecimal potencia = taxaMais1.pow(parcelas, mc);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, mc));
        return total.multiply(taxa, mc).divide(divisor, mc);
    }
}
