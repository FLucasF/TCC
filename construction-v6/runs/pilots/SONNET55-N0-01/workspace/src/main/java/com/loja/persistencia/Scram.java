package com.loja.persistencia;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Autenticação SCRAM-SHA-256 (RFC 5802/7677), o padrão do PostgreSQL moderno. */
final class Scram {

    private final String senha;
    private final String nonce;
    private final String primeiraBase;

    Scram(String usuario, String senha, String nonce) {
        this.senha = senha;
        this.nonce = nonce;
        this.primeiraBase = "n=" + usuario + ",r=" + nonce;
    }

    String primeiraMensagem() {
        return "n,," + primeiraBase;
    }

    /** Responde à mensagem do servidor com a prova da senha. */
    String mensagemFinal(String respostaServidor) {
        Map<String, String> atributos = new HashMap<>();
        for (String parte : respostaServidor.split(",")) {
            atributos.put(parte.substring(0, 1), parte.substring(2));
        }
        String nonceCompleto = atributos.get("r");
        if (nonceCompleto == null || !nonceCompleto.startsWith(nonce)) {
            throw new IllegalStateException("Resposta SCRAM inválida");
        }
        byte[] sal = Base64.getDecoder().decode(atributos.get("s"));
        int iteracoes = Integer.parseInt(atributos.get("i"));
        try {
            byte[] salgada = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256)).getEncoded();
            byte[] chaveCliente = hmac(salgada, "Client Key");
            byte[] chaveGuardada = MessageDigest.getInstance("SHA-256").digest(chaveCliente);
            String semProva = "c=biws,r=" + nonceCompleto;
            String autenticacao = primeiraBase + "," + respostaServidor + "," + semProva;
            byte[] assinatura = hmac(chaveGuardada, autenticacao);
            byte[] prova = new byte[chaveCliente.length];
            for (int i = 0; i < prova.length; i++) {
                prova[i] = (byte) (chaveCliente[i] ^ assinatura[i]);
            }
            return semProva + ",p=" + Base64.getEncoder().encodeToString(prova);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] chave, String texto) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
    }
}
