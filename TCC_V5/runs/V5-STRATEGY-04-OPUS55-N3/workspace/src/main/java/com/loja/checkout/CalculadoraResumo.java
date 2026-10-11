package com.loja.checkout;

import com.loja.checkout.api.PedidoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Ordem fixa do cálculo e das validações; o que varia fica nos casos de cada catálogo. */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades, Catalogo<Cupom> cupons,
            Catalogo<NivelClube> niveis, Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        Carrinho carrinho = carrinhoValido(pedido.itens());
        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.buscar(pedido.regiao()), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = exigir(modalidades.buscar(pedido.modalidadeEntrega()), "MODALIDADE_INVALIDA");
        verificar(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = carrinho.subtotal();
        // frete já com a vantagem do clube: é o que aparece no resumo e o que o FRETEGRATIS desconta
        BigDecimal freteCobrado = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = descontoCupom(pedido.cupom(), new ContextoCupom(carrinho, subtotal, freteCobrado));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(freteCobrado).add(seguro);

        FormaPagamento forma = exigir(formasPagamento.buscar(pedido.formaPagamento()), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        verificar(forma.aceitaParcelas(parcelas), "PARCELAMENTO_INVALIDO");
        verificar(forma.disponivel(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pagamento = forma.pagar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                freteCobrado,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(cupons.buscar(codigo), "CUPOM_INVALIDO");
        verificar(cupom.aplicavel(contexto), "CUPOM_NAO_APLICAVEL");
        return cupom.desconto(contexto);
    }

    private static Carrinho carrinhoValido(List<Item> itens) {
        verificar(itens != null && !itens.isEmpty(), "PEDIDO_INVALIDO");
        itens.forEach(item -> verificar(itemValido(item), "PEDIDO_INVALIDO"));
        return new Carrinho(itens);
    }

    private static boolean itemValido(Item item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static <T> T exigir(Optional<T> caso, String erro) {
        return caso.orElseThrow(() -> new CheckoutException(erro));
    }

    private static void verificar(boolean condicao, String erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}
