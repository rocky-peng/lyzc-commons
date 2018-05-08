package com.leyongzuche.commons.encrpt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

public class RSAEncrypt {

    private RSAPublicKey publicKey;

    public RSAEncrypt(String pkcs8PublicKeyFilePath) {
        this.publicKey = loadPublicKey(RSAEncrypt.class.getResourceAsStream(pkcs8PublicKeyFilePath));
    }

    public RSAEncrypt(InputStream is) {
        this.publicKey = loadPublicKey(is);
    }

    private RSAPublicKey loadPublicKey(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                if (readLine.charAt(0) == '-') {
                    continue;
                } else {
                    sb.append(readLine);
                    sb.append('\r');
                }
            }
            return loadPublicKey(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("加载公钥失败", e);
        }
    }

    private RSAPublicKey loadPublicKey(String publicKeyStr) {
        try {
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] buffer = base64Decoder.decodeBuffer(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("加载公钥失败", e);
        }
    }


    public byte[] encrypt(byte[] plainTextData) {
        if (publicKey == null) {
            throw new RuntimeException("加密公钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-1ANDMGF1PADDING", new BouncyCastleProvider());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(plainTextData);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

}