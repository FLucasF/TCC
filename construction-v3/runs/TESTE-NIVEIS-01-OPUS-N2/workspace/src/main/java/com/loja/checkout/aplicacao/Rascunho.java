package com.loja.checkout.aplicacao;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.Entrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * O pedido em preparo: resolve os codigos que o site enviou e vai calculando as
 * etapas do resumo, cada uma uma vez. As etapas derivadas (pedido, frete,
 * desconto, total) so fazem sentido depois que as regras do pedido passaram.
 */
public final class Rascunho {

    private final ResumoRequest bruto;
    private final Catalogos catalogos;

    private final Supplier<Pedido> pedido = memo(this::montarPedido);
    private final Supplier<Entrega> entrega = memo(this::calcularEntrega);
    private final Supplier<BigDecimal> frete = memo(this::calcularFrete);
    private final Supplier<BigDecimal> descontoCupom = memo(this::calcularDescontoCupom);
    private final Supplier<BigDecimal> seguro = memo(this::calcularSeguro);
    private final Supplier<BigDecimal> totalPedido = memo(this::calcularTotalPedido);

    Rascunho(ResumoRequest bruto, Catalogos catalogos) {
        this.bruto = bruto == null
                ? new ResumoRequest(null, null, null, null, null, null, null)
                : bruto;
        this.catalogos = catalogos;
    }

    public List<ItemRequest> itensBrutos() {
        return bruto.itens() == null ? List.of() : bruto.itens();
    }

    public Optional<NivelClube> nivelClube() {
        return catalogos.niveis().buscar(bruto.nivelClube());
    }

    public Optional<Regiao> regiao() {
        return Optional.ofNullable(bruto.regiao()).flatMap(Rascunho::regiaoDeCodigo);
    }

    public Optional<ModalidadeEntrega> modalidadeEntrega() {
        return catalogos.entregas().buscar(bruto.modalidadeEntrega());
    }

    public Optional<FormaPagamento> formaPagamento() {
        return catalogos.pagamentos().buscar(bruto.formaPagamento());
    }

    public boolean informouCupom() {
        return bruto.cupom() != null;
    }

    public Optional<Cupom> cupom() {
        return catalogos.cupons().buscar(bruto.cupom());
    }

    public int parcelas() {
        return bruto.parcelas() == null ? 1 : bruto.parcelas();
    }

    public Pedido pedido() {
        return pedido.get();
    }

    public Entrega entrega() {
        return entrega.get();
    }

    /** O frete que o cliente paga, ja considerando o frete gratis do clube. */
    public BigDecimal frete() {
        return frete.get();
    }

    public ContextoCupom contextoCupom() {
        return new ContextoCupom(pedido(), frete());
    }

    public BigDecimal descontoCupom() {
        return descontoCupom.get();
    }

    public BigDecimal seguro() {
        return seguro.get();
    }

    public BigDecimal totalPedido() {
        return totalPedido.get();
    }

    private Pedido montarPedido() {
        return new Pedido(itensBrutos().stream()
                .map(item -> new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private Entrega calcularEntrega() {
        return modalidadeEntrega().orElseThrow().calcular(pedido());
    }

    private BigDecimal calcularFrete() {
        return nivelClube().orElseThrow().freteGratis()
                ? Dinheiro.ZERO
                : entrega().valor();
    }

    private BigDecimal calcularDescontoCupom() {
        return cupom()
                .map(cupom -> cupom.desconto(contextoCupom()))
                .orElse(Dinheiro.ZERO);
    }

    private BigDecimal calcularSeguro() {
        return regiao().orElseThrow().seguro(pedido().subtotalProdutos());
    }

    private BigDecimal calcularTotalPedido() {
        return Dinheiro.centavos(pedido().subtotalProdutos()
                .subtract(descontoCupom())
                .add(frete())
                .add(seguro()));
    }

    private static Optional<Regiao> regiaoDeCodigo(String codigo) {
        return Arrays.stream(Regiao.values())
                .filter(regiao -> regiao.name().equals(codigo))
                .findFirst();
    }

    private static <T> Supplier<T> memo(Supplier<T> fonte) {
        return new Supplier<>() {
            private T valor;

            @Override
            public T get() {
                if (valor == null) {
                    valor = fonte.get();
                }
                return valor;
            }
        };
    }
}
