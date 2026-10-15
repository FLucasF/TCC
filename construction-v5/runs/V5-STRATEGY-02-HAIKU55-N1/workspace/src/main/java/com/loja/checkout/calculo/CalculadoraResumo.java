package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(PedidoRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido.itens());
        NivelClube nivel = Codigos.buscar(NivelClube.values(), pedido.nivelClube(), Erro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Codigos.buscar(Regiao.values(), pedido.regiao(), Erro.REGIAO_INVALIDA);
        Modalidade modalidade = Codigos.buscar(Modalidade.values(), pedido.modalidadeEntrega(), Erro.MODALIDADE_INVALIDA);

        BigDecimal peso = somar(itens, ItemPedido::peso);
        exigir(modalidade.disponivelPara(peso), Erro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = Dinheiro.arredondar(somar(itens, ItemPedido::subtotal));
        BigDecimal frete = Dinheiro.arredondar(nivel.freteFinal(modalidade.fretePara(peso)));
        Totais totais = new Totais(itens, subtotal, frete);

        Optional<Cupom> cupom = Optional.ofNullable(pedido.cupom())
                .filter(codigo -> !codigo.isBlank())
                .map(codigo -> Codigos.buscar(Cupom.values(), codigo, Erro.CUPOM_INVALIDO));
        cupom.ifPresent(c -> exigir(c.aplicavel(totais), Erro.CUPOM_NAO_APLICAVEL));
        BigDecimal descontoCupom = cupom.map(c -> Dinheiro.arredondar(c.desconto(totais)))
                .orElse(new BigDecimal("0.00"));

        BigDecimal seguro = regiao.seguroPara(subtotal);
        BigDecimal total = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento forma = Codigos.buscar(FormaPagamento.values(), pedido.formaPagamento(), Erro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(forma.parcelamentoPermitido(parcelas), Erro.PARCELAMENTO_INVALIDO);
        exigir(forma.disponivelPara(total), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        ResultadoPagamento pagamento = forma.cobrar(total, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(total),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.creditoPara(subtotal),
                nivel.temBrinde(subtotal));
    }

    private static List<ItemPedido> validarItens(List<ItemPedido> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(item -> item != null && item.valido()),
                Erro.PEDIDO_INVALIDO);
        return itens;
    }

    private static BigDecimal somar(List<ItemPedido> itens, Function<ItemPedido, BigDecimal> valor) {
        return itens.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void exigir(boolean condicao, Erro erro) {
        if (!condicao) {
            throw new ResumoException(erro);
        }
    }
}
