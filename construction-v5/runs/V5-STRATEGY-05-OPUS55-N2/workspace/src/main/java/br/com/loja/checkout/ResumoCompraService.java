package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<Regiao> regioes = new Catalogo<>(Arrays.asList(Regiao.values()), Regiao::name);

    public ResumoCompraService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                               List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Carrinho carrinho = carrinho(request.itens());
        NivelClube nivel = niveis.buscar(request.nivelClube()).orElseThrow(() -> recusa("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = regioes.buscar(request.regiao()).orElseThrow(() -> recusa("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> recusa("MODALIDADE_INVALIDA"));
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");
        Optional<Cupom> cupom = Optional.ofNullable(request.cupom())
                .map(codigo -> cupons.buscar(codigo).orElseThrow(() -> recusa("CUPOM_INVALIDO")));
        exigir(cupom.map(c -> c.aplicavel(carrinho)).orElse(true), "CUPOM_NAO_APLICAVEL");
        FormaPagamento pagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> recusa("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Optional.ofNullable(request.parcelas()).orElse(1);
        exigir(pagamento.aceitaParcelas(parcelas), "PARCELAMENTO_INVALIDO");

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));
        exigir(pagamento.atende(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(cobranca.totalFinal()),
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(ResumoCompraService::itemValido),
                "PEDIDO_INVALIDO");
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static void exigir(boolean condicao, String codigoErro) {
        if (!condicao) {
            throw recusa(codigoErro);
        }
    }

    private static CheckoutException recusa(String codigo) {
        return new CheckoutException(codigo);
    }
}
