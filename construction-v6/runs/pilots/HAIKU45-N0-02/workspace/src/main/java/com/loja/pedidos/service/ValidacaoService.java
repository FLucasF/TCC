package com.loja.pedidos.service;

import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.domain.Produto;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ValidacaoService {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TransLogService transLogService;

    public void validarCheckout(CheckoutRequestDTO req) {
        validarDadosBasicos(req);
        validarProdutos(req.getItens());
        validarNivelClube(req.getNivelClube());
        validarRegiao(req.getRegiao());
        validarCep(req.getCep());
        validarModalidadeEntrega(req.getModalidadeEntrega());
        validarFormaPagamento(req.getFormaPagamento(), req.getParcelas());
        validarCupom(req.getCupom(), req.getItens());
    }

    private void validarDadosBasicos(CheckoutRequestDTO req) {
        if (req.getCliente() == null || req.getCliente().getCpf() == null || req.getCliente().getCpf().isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO", "CPF is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (req.getCliente().getEmail() == null || req.getCliente().getEmail().isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO", "Email is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (req.getItens() == null || req.getItens().isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO", "Cart is empty", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> skusVendidos = new HashSet<>();
        for (ItemDTO item : req.getItens()) {
            if (item.getSku() == null || item.getSku().isEmpty()) {
                throw new PedidoException("PEDIDO_INVALIDO", "SKU is required", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (item.getQuantidade() <= 0) {
                throw new PedidoException("PEDIDO_INVALIDO", "Quantity must be positive", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!skusVendidos.add(item.getSku())) {
                throw new PedidoException("PEDIDO_INVALIDO", "Duplicate SKU in cart", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validarProdutos(List<ItemDTO> itens) {
        for (ItemDTO item : itens) {
            Optional<Produto> prod = produtoRepository.findById(item.getSku());
            if (prod.isEmpty()) {
                throw new PedidoException("PRODUTO_INEXISTENTE", "Product not found", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (prod.get().isForaDeLinha()) {
                throw new PedidoException("PRODUTO_FORA_DE_LINHA", "Product is discontinued", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (prod.get().getEstoque() < item.getQuantidade()) {
                throw new PedidoException("ESTOQUE_INSUFICIENTE", "Insufficient stock", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validarNivelClube(String nivelClube) {
        if (nivelClube == null || !nivelClube.matches("BRONZE|PRATA|OURO")) {
            throw new PedidoException("NIVEL_CLUBE_INVALIDO", "Invalid club level", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarRegiao(String regiao) {
        if (regiao == null || !regiao.matches("SUDESTE|SUL|CENTRO_OESTE|NORTE|NORDESTE")) {
            throw new PedidoException("REGIAO_INVALIDA", "Invalid region", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarCep(String cep) {
        if (cep == null || !cep.matches("\\d{8}")) {
            throw new PedidoException("CEP_INVALIDO", "Invalid CEP format", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarModalidadeEntrega(String modalidade) {
        if (modalidade == null || !modalidade.matches("ECONOMICA|EXPRESSA|RETIRADA_LOJA|MOTOBOY")) {
            throw new PedidoException("MODALIDADE_INVALIDA", "Invalid delivery method", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarFormaPagamento(String forma, Integer parcelas) {
        if (forma == null || !forma.matches("PIX|CARTAO|BOLETO")) {
            throw new PedidoException("FORMA_PAGAMENTO_INVALIDA", "Invalid payment method", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        int parcelasEf = parcelas != null ? parcelas : 1;

        if ("PIX".equals(forma) || "BOLETO".equals(forma)) {
            if (parcelasEf != 1) {
                throw new PedidoException("PARCELAMENTO_INVALIDO", "Invalid installment count", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if ("CARTAO".equals(forma)) {
            if (parcelasEf < 1 || parcelasEf > 12) {
                throw new PedidoException("PARCELAMENTO_INVALIDO", "Invalid installment count", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validarCupom(String cupom, List<ItemDTO> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        if (!cupom.matches("BEMVINDO10|MENOS50|FRETEGRATIS|LEVE3PAGUE2")) {
            throw new PedidoException("CUPOM_INVALIDO", "Invalid coupon", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public void validarCupomAplicavel(String cupom, CheckoutRequestDTO req) {
        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        if ("BEMVINDO10".equals(cupom)) {
            var cliente = clienteRepository.findByCpf(req.getCliente().getCpf());
            if (cliente.isPresent()) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL", "Coupon not applicable", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if ("MENOS50".equals(cupom)) {
            BigDecimal subtotal = calcularSubtotal(req.getItens());
            if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL", "Coupon not applicable", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if ("FRETEGRATIS".equals(cupom)) {
            if ("RETIRADA_LOJA".equals(req.getModalidadeEntrega())) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL", "Coupon not applicable", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDTO> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            var produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                BigDecimal preco = produto.get().getPreco();
                BigDecimal quantidade = new BigDecimal(item.getQuantidade());
                total = total.add(preco.multiply(quantidade));
            }
        }
        return total;
    }

    public void validarDisponibilidadeEntrega(CheckoutRequestDTO req, BigDecimal pesoKg) {
        String modalidade = req.getModalidadeEntrega();

        if ("MOTOBOY".equals(modalidade)) {
            if (pesoKg.compareTo(new BigDecimal("5")) > 0) {
                throw new PedidoException("MODALIDADE_INDISPONIVEL", "Motoboy unavailable for this weight", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if ("ECONOMICA".equals(modalidade) || "EXPRESSA".equals(modalidade)) {
            String service = "ECONOMICA".equals(modalidade) ? "STD" : "EXP";
            TransLogService.FreteResult resultado = transLogService.consultarFrete(req.getCep(), pesoKg, service);
            if (!resultado.available) {
                throw new PedidoException("MODALIDADE_INDISPONIVEL", "Delivery method not available", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    public void validarFormaPagamentoDisponivel(String forma, BigDecimal totalFinal) {
        if ("BOLETO".equals(forma) && totalFinal.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL", "Payment method not available", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
