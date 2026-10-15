package br.com.loja.checkout;

import static br.com.loja.checkout.Dinheiro.centavos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;

/** Parte comum a todos os pedidos: ordem do cálculo, das validações e arredondamento. */
@Service
public class CalculadoraResumo {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal peso = somar(itens.stream().map(ItemPedido::pesoTotal).toList());
        if (!modalidade.atende(peso)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = centavos(somar(itens.stream().map(ItemPedido::valorTotal).toList()));
        BigDecimal frete = centavos(nivel.freteCobrado(modalidade.frete(peso)));
        BigDecimal desconto = descontoCupom(pedido.cupom(), new ContextoCupom(itens, subtotal, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        if (!forma.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = forma.pagar(totalPedido, parcelas);
        BigDecimal totalFinal = pagamento.totalFinal();

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                totalFinal.subtract(totalPedido),
                totalFinal,
                parcelas,
                pagamento.valorParcela(),
                centavos(nivel.credito(subtotal)),
                nivel.brinde(subtotal));
    }

    private static List<ItemPedido> validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(i -> i != null && i.valido())) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return centavos(BigDecimal.ZERO);
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return centavos(cupom.desconto(contexto));
    }

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
