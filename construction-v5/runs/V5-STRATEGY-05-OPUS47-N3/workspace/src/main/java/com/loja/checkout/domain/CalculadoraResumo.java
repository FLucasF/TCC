package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraResumo {

    public Resumo calcular(Entrada entrada) {
        List<Item> itens = validarItens(entrada.itens());
        NivelClube nivel = parseEnum(NivelClube.class, entrada.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, entrada.regiao(), "REGIAO_INVALIDA");
        Modalidade modalidade = parseEnum(Modalidade.class, entrada.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal subtotal = Dinheiro.cent(itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.atende(pesoTotal)) {
            throw new ErroCalculo("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.pagaFrete()
                ? Dinheiro.cent(modalidade.custo(pesoTotal))
                : Dinheiro.ZERO;

        Cupom cupom = null;
        if (entrada.cupom() != null && !entrada.cupom().isBlank()) {
            cupom = parseEnum(Cupom.class, entrada.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(itens, subtotal, frete)) {
                throw new ErroCalculo("CUPOM_NAO_APLICAVEL");
            }
        }

        BigDecimal desconto = cupom == null
                ? Dinheiro.ZERO
                : Dinheiro.cent(cupom.desconto(itens, subtotal, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.cent(
                subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento forma = parseEnum(FormaPagamento.class, entrada.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = entrada.parcelas() == null ? 1 : entrada.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new ErroCalculo("PARCELAMENTO_INVALIDO");
        }
        if (!forma.atende(totalPedido)) {
            throw new ErroCalculo("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.Resultado rp = forma.calcular(totalPedido, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                rp.ajuste(),
                rp.totalFinal(),
                parcelas,
                rp.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal)
        );
    }

    private static List<Item> validarItens(List<Item> brutos) {
        if (brutos == null || brutos.isEmpty()) {
            throw new ErroCalculo("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>(brutos.size());
        for (Item i : brutos) {
            if (i == null
                    || i.precoUnitario() == null
                    || i.precoUnitario().signum() <= 0
                    || i.quantidade() <= 0
                    || i.pesoKg() == null
                    || i.pesoKg().signum() <= 0) {
                throw new ErroCalculo("PEDIDO_INVALIDO");
            }
            itens.add(i);
        }
        return itens;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> tipo, String nome, String erro) {
        if (nome == null) {
            throw new ErroCalculo(erro);
        }
        try {
            return Enum.valueOf(tipo, nome);
        } catch (IllegalArgumentException e) {
            throw new ErroCalculo(erro);
        }
    }

    public record Entrada(
            List<Item> itens,
            String modalidadeEntrega,
            String cupom,
            String formaPagamento,
            Integer parcelas,
            String nivelClube,
            String regiao
    ) {}
}
