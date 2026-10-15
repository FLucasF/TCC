package com.loja.checkout.servico;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. Cada eixo que varia (entrega, cupom, clube,
 * pagamento) e resolvido por uma busca num mapa de estrategias e usado de forma
 * polimorfica; aqui mora apenas a sequencia fixa do calculo e a ordem das
 * recusas, que sao iguais para todos os casos.
 */
@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveisClube,
                           List<FormaPagamento> formasPagamento) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveisClube = indexar(niveisClube, NivelClube::codigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> itens, Function<T, String> chave) {
        return itens.stream().collect(Collectors.toMap(chave, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        // 1 - itens
        List<Item> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somar(itens, Item::totalLinha));
        BigDecimal pesoTotal = somar(itens, Item::pesoLinha);

        // 2 - clube
        NivelClube clube = exigir(niveisClube.get(req.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);

        // 3 - regiao
        Regiao regiao = Regiao.resolver(req.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));

        // 4 e 5 - entrega
        ModalidadeEntrega modalidade = exigir(modalidades.get(req.modalidadeEntrega()), CodigoErro.MODALIDADE_INVALIDA);
        if (!modalidade.atende(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal freteBase = Dinheiro.centavos(modalidade.frete(pesoTotal));
        BigDecimal frete = clube.freteGratis() ? Dinheiro.ZERO : freteBase;

        // 6 e 7 - cupom
        BigDecimal descontoCupom = calcularDescontoCupom(req.cupom(), subtotal, itens, frete);

        // seguro
        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.taxaSeguro()));

        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8, 9 e 10 - pagamento
        FormaPagamento pagamento = exigir(formasPagamento.get(req.formaPagamento()), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                Dinheiro.centavos(clube.credito(subtotal)),
                clube.brinde(subtotal));
    }

    private List<Item> validarItens(List<ItemRequest> requisitados) {
        if (requisitados == null || requisitados.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>(requisitados.size());
        for (ItemRequest r : requisitados) {
            if (positivo(r.precoUnitario()) && r.quantidade() != null && r.quantidade() > 0 && positivo(r.pesoKg())) {
                itens.add(new Item(r.nome(), r.precoUnitario(), r.quantidade(), r.pesoKg()));
            } else {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private BigDecimal calcularDescontoCupom(String codigo, BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(cupons.get(codigo), CodigoErro.CUPOM_INVALIDO);
        CupomContexto contexto = new CupomContexto(subtotal, itens, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal somar(List<Item> itens, Function<Item, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> T exigir(T valor, CodigoErro erro) {
        if (valor == null) {
            throw new CheckoutException(erro);
        }
        return valor;
    }
}
