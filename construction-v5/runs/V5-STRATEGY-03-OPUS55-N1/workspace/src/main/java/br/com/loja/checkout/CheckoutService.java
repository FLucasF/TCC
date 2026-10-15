package br.com.loja.checkout;

import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PARCELAMENTO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PEDIDO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.REGIAO_INVALIDA;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                           List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse resumir(ResumoRequest request) {
        Carrinho carrinho = carrinho(request.itens());
        NivelClube nivel = niveis.buscar(request.nivelClube()).orElseThrow(() -> recusa(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(request.regiao()).orElseThrow(() -> recusa(REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> recusa(MODALIDADE_INVALIDA));
        exigir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal descontoCupom = descontoCupom(request.cupom(), new ContextoCupom(carrinho, frete));

        FormaPagamento forma = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> recusa(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(request.parcelas(), 1);
        exigir(forma.permiteParcelas(parcelas), PARCELAMENTO_INVALIDO);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(descontoCupom).add(frete).add(seguro));
        exigir(forma.atende(totalPedido), FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = forma.aplicar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> recusa(CUPOM_INVALIDO));
        exigir(cupom.aplicavel(contexto), CUPOM_NAO_APLICAVEL);
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::itemValido), PEDIDO_INVALIDO);
        return new Carrinho(itens.stream()
                .map(item -> new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
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

    private static void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw recusa(erro);
        }
    }

    private static CheckoutException recusa(CodigoErro erro) {
        return new CheckoutException(erro);
    }
}
