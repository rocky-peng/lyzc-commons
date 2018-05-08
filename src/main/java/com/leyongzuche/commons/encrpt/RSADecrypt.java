package com.leyongzuche.commons.encrpt;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;

public class RSADecrypt {

    private RSAPrivateKey privateKey;

    public RSADecrypt(String pkcs8PublicKeyFilePath) {
        this.privateKey = loadPrivateKey(RSADecrypt.class.getResourceAsStream(pkcs8PublicKeyFilePath));
    }

    public RSADecrypt(InputStream is) {
        this.privateKey = loadPrivateKey(is);
    }

    public RSAPrivateKey loadPrivateKey(InputStream in) {
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
            return loadPrivateKey(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("私钥数据读取错误");
        }
    }

    public RSAPrivateKey loadPrivateKey(String privateKeyStr) {
        //pkcs8
        try {
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] buffer = base64Decoder.decodeBuffer(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception ex) {
            //pkcs1
            try {
                RSAPrivateKeyStructure asn1PrivKey = new RSAPrivateKeyStructure((ASN1Sequence) ASN1Sequence.fromByteArray(new BASE64Decoder().decodeBuffer(privateKeyStr)));
                RSAPrivateKeySpec rsaPrivKeySpec = new RSAPrivateKeySpec(asn1PrivKey.getModulus(), asn1PrivKey.getPrivateExponent());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) keyFactory.generatePrivate(rsaPrivKeySpec);
            } catch (Exception e) {
                throw new RuntimeException("无此算法");
            }
        }
    }


    public byte[] decrypt(byte[] cipherData) {
        if (privateKey == null) {
            throw new RuntimeException("解密私钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-1ANDMGF1PADDING", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("解密失败");
        }
    }
}