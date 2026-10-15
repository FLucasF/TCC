package com.loja.checkout.servico;

import com.loja.checkout.dto.ItemPedidoDTO;
import com.loja.checkout.dto.RequisicaoResumoDTO;
import com.loja.checkout.dto.RespostaResumoDTO;
import com.loja.checkout.excecao.ErroCheckout;
import com.loja.checkout.modelo.FormaPagamento;
import com.loja.checkout.modelo.ModalidadeEntrega;
import com.loja.checkout.modelo.NivelClube;
import com.loja.checkout.modelo.Regiao;
import com.loja.checkout.modelo.cupom.Cupom;
import com.loja.checkout.modelo.cupom.RegistroCupons;
import com.loja.checkout.util.ArredondamentoBancario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ServicoCheckout {

    public RespostaResumoDTO calcularResumo(RequisicaoResumoDTO requisicao) {
        validar(requisicao);

        List<ItemPedidoDTO> itens = requisicao.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
        NivelClube nivel = NivelClube.valueOf(requisicao.getNivelClube());
        Regiao regiao = Regiao.valueOf(requisicao.getRegiao());
        FormaPagamento formaPagamento = FormaPagamento.valueOf(requisicao.getFormaPagamento());

        int parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

        // 1. Subtotal dos produtos
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        // 2. Desconto do cupom
        BigDecimal descontoCupom = calcularDescontoCupom(requisicao.getCupom(), itens, subtotalProdutos, null);

        // 3. Frete
        BigDecimal frete = calcularFrete(modalidade, itens, nivel);

        // Recalcular desconto do cupom sabendo o frete (para FRETEGRATIS)
        descontoCupom = calcularDescontoCupom(requisicao.getCupom(), itens, subtotalProdutos, frete);

        // 4. Imposto
        BigDecimal produtosComDesconto = subtotalProdutos.subtract(descontoCupom);
        BigDecimal imposto = calcularImposto(produtosComDesconto, regiao);

        // 5. Total antes do ajuste de pagamento
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(imposto);

        // 6. Ajuste de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        // Calcular parcelamento
        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        // Crédito do próximo pedido
        BigDecimal creditoProximaCompra = calcularCredito(nivel, subtotalProdutos);

        // Brinde
        boolean temBrinde = nivel.isTemBrinde() && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        return new RespostaResumoDTO(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            imposto,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            temBrinde
        );
    }

    private void validar(RequisicaoResumoDTO requisicao) {
        // 1. Pedido inválido
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemPedidoDTO item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }

        // 2. Nível do clube
        if (requisicao.getNivelClube() == null) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(requisicao.getNivelClube());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Região
        if (requisicao.getRegiao() == null) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(requisicao.getRegiao());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }

        // 4. Modalidade de entrega
        if (requisicao.getModalidadeEntrega() == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        // 5. Modalidade disponível para o peso
        BigDecimal pesoTotal = calcularPesoTotal(requisicao.getItens());
        if (!modalidade.podeUsarComPeso(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        // 6. Cupom válido
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            if (!RegistroCupons.obter(requisicao.getCupom()).isPresent()) {
                throw new ErroCheckout("CUPOM_INVALIDO");
            }
        }

        // 7. Cupom aplicável
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            Optional<Cupom> cupom = RegistroCupons.obter(requisicao.getCupom());
            BigDecimal subtotal = calcularSubtotalProdutos(requisicao.getItens());
            if (!cupom.get().ehAplicavel(requisicao.getItens(), subtotal)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Forma de pagamento
        if (requisicao.getFormaPagamento() == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            FormaPagamento.valueOf(requisicao.getFormaPagamento());
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        // 9. Número de parcelas permitido
        FormaPagamento formaPagamento = FormaPagamento.valueOf(requisicao.getFormaPagamento());
        int parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        if (!formaPagamento.podeParcelarEm(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        // 10. Forma de pagamento disponível
        // Boleto não aceito acima de R$ 1.000
        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotalProdutos(requisicao.getItens());
            BigDecimal descontoCupom = calcularDescontoCupom(requisicao.getCupom(), requisicao.getItens(), subtotal, null);
            BigDecimal frete = calcularFrete(modalidade, requisicao.getItens(), NivelClube.valueOf(requisicao.getNivelClube()));
            BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete);
            if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedidoDTO> itens) {
        return itens.stream()
            .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedidoDTO> itens) {
        return itens.stream()
            .map(item -> item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Optional<Cupom> cupom = RegistroCupons.obter(codigoCupom);
        if (!cupom.isPresent()) {
            return BigDecimal.ZERO;
        }

        BigDecimal freteParaCalculo = frete != null ? frete : BigDecimal.ZERO;
        return cupom.get().calcularDesconto(itens, subtotalProdutos, freteParaCalculo);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemPedidoDTO> itens, NivelClube nivel) {
        if (nivel.isFretGratis()) {
            return BigDecimal.ZERO;
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal custoPorPeso = modalidade.getCustoParKg().multiply(pesoTotal);
        BigDecimal freteTotal = modalidade.getCustoFixo().add(custoPorPeso);

        return ArredondamentoBancario.arredondar(freteTotal);
    }

    private BigDecimal calcularImposto(BigDecimal produtosComDesconto, Regiao regiao) {
        BigDecimal imposto = produtosComDesconto.multiply(regiao.getAliquota());
        return ArredondamentoBancario.arredondar(imposto);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
            return ArredondamentoBancario.arredondar(desconto.negate());
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            return new BigDecimal("3.49");
        }

        if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO;
            }
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal fatorJuros = BigDecimal.ONE.add(taxaMensal).pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(taxaMensal).multiply(fatorJuros);
            BigDecimal denominador = fatorJuros.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
            valorParcela = ArredondamentoBancario.arredondar(valorParcela);
            BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
            return totalComJuros.subtract(totalPedido);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularValorParcela(FormaPagamento formaPagamento, BigDecimal totalFinal, int parcelas) {
        if (formaPagamento == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal fatorJuros = BigDecimal.ONE.add(taxaMensal).pow(parcelas);
            BigDecimal numerador = totalFinal.multiply(taxaMensal).multiply(fatorJuros);
            BigDecimal denominador = fatorJuros.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
            return ArredondamentoBancario.arredondar(valorParcela);
        }

        return ArredondamentoBancario.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(nivel.getPercentualCredito());
        return ArredondamentoBancario.arredondar(credito);
    }
}
