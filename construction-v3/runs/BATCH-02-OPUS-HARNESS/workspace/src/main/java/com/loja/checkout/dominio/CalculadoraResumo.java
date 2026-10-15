package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra na ordem combinada: produtos, cupom, frete,
 * total do pedido e ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(PedidoRequisicao requisicao) {
        List<ItemPedido> itens = itensValidos(requisicao);

        ModalidadeEntrega entrega = ModalidadeEntrega.porCodigo(requisicao.modalidadeEntrega())
                .orElseThrow(ErroCheckout.MODALIDADE_INVALIDA::excecao);
        BigDecimal pesoKg = soma(itens.stream().map(ItemPedido::pesoTotal));
        if (!entrega.atende(pesoKg)) {
            throw ErroCheckout.MODALIDADE_INDISPONIVEL.excecao();
        }

        BigDecimal subtotalProdutos = Dinheiro.centavos(soma(itens.stream().map(ItemPedido::valorTotal)));
        BigDecimal frete = Dinheiro.centavos(entrega.frete(pesoKg));

        Optional<Cupom> cupom = cupomInformado(requisicao.cupom());
        BaseCupom baseCupom = new BaseCupom(itens, subtotalProdutos, frete);
        BigDecimal descontoCupom = cupom.map(aplicado -> desconto(aplicado, baseCupom)).orElse(Dinheiro.ZERO);

        BigDecimal totalPedido = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento pagamento = FormaPagamento.porCodigo(requisicao.formaPagamento())
                .orElseThrow(ErroCheckout.FORMA_PAGAMENTO_INVALIDA::excecao);
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw ErroCheckout.PARCELAMENTO_INVALIDO.excecao();
        }
        if (!pagamento.atende(totalPedido)) {
            throw ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL.excecao();
        }

        ResultadoPagamento resultado = pagamento.aplicar(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new Resumo(subtotalProdutos, descontoCupom, frete, entrega.prazoDias(),
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela());
    }

    private static List<ItemPedido> itensValidos(PedidoRequisicao requisicao) {
        if (requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw ErroCheckout.PEDIDO_INVALIDO.excecao();
        }
        return requisicao.itens().stream().map(CalculadoraResumo::itemValido).toList();
    }

    private static ItemPedido itemValido(ItemRequisicao item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw ErroCheckout.PEDIDO_INVALIDO.excecao();
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static Optional<Cupom> cupomInformado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Cupom.porCodigo(codigo).orElseThrow(ErroCheckout.CUPOM_INVALIDO::excecao));
    }

    private static BigDecimal desconto(Cupom cupom, BaseCupom base) {
        if (!cupom.aplicavel(base)) {
            throw ErroCheckout.CUPOM_NAO_APLICAVEL.excecao();
        }
        return Dinheiro.centavos(cupom.desconto(base));
    }

    private static BigDecimal soma(java.util.stream.Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
