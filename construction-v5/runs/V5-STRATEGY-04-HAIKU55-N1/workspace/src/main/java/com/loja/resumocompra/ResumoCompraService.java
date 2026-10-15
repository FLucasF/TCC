package com.loja.resumocompra;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(PedidoRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido.itens());
        NivelClube nivel = obter(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = obter(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = obter(ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal peso = itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.atende(peso)) {
            throw new ErroPedido(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = pedido.cupom() == null ? null : obter(Cupom.class, pedido.cupom(), CodigoErro.CUPOM_INVALIDO);
        BigDecimal subtotal = Dinheiro.arredondar(itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal frete = Dinheiro.arredondar(nivel.aplicarFrete(modalidade.custo(peso)));
        Contexto contexto = new Contexto(itens, subtotal, frete);

        BigDecimal desconto = new BigDecimal("0.00");
        if (cupom != null) {
            if (!cupom.aplicavel(contexto)) {
                throw new ErroPedido(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            desconto = Dinheiro.arredondar(cupom.desconto(contexto));
        }

        FormaPagamento forma = obter(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new ErroPedido(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.taxaSeguro()));
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.atende(total)) {
            throw new ErroPedido(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Pagamento pagamento = forma.calcular(total, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(pagamento.totalFinal());
        BigDecimal credito = Dinheiro.arredondar(subtotal.multiply(nivel.taxaCredito()));

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                totalFinal.subtract(total),
                totalFinal,
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()),
                credito,
                nivel.temBrinde(subtotal));
    }

    private static List<ItemPedido> validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemPedido item : itens) {
            if (item == null || naoPositivo(item.precoUnitario()) || item.quantidade() <= 0 || naoPositivo(item.pesoKg())) {
                throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private static boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.signum() <= 0;
    }

    private static <E extends Enum<E>> E obter(Class<E> tipo, String nome, CodigoErro erro) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroPedido(erro));
    }
}
