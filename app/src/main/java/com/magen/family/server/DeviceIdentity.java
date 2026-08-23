package com.magen.family.server;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyInfo;
import android.os.Build;
import java.security.KeyFactory;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

/** Per-device ECDSA identity. Private key is non-exportable in Android Keystore. */
public final class DeviceIdentity {
    private static final String KS="AndroidKeyStore";
    private static final String ALIAS="magen_device_identity_v1";
    private DeviceIdentity() {}
    private static KeyStore store() throws Exception { KeyStore k=KeyStore.getInstance(KS); k.load(null); return k; }
    public static synchronized void ensure() throws Exception {
        KeyStore k=store(); if(k.containsAlias(ALIAS)) return;
        KeyPairGenerator g=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,KS);
        g.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256).build());
        g.generateKeyPair();
    }
    public static PublicKey publicKey() throws Exception { ensure(); return store().getCertificate(ALIAS).getPublicKey(); }
    public static PrivateKey privateKey() throws Exception { ensure(); return (PrivateKey)store().getKey(ALIAS,null); }
    public static String deviceId(Context c) throws Exception {
        byte[] d=MessageDigest.getInstance("SHA-256").digest(publicKey().getEncoded());
        StringBuilder sb=new StringBuilder("magen-"); for(int i=0;i<12;i++) sb.append(String.format("%02x",d[i])); return sb.toString();
    }
    public static String publicKeyPem() throws Exception {
        String b=Base64.encodeToString(publicKey().getEncoded(),Base64.NO_WRAP);
        StringBuilder out=new StringBuilder("-----BEGIN PUBLIC KEY-----\n");
        for(int i=0;i<b.length();i+=64) out.append(b, i, Math.min(i+64,b.length())).append('\n');
        return out.append("-----END PUBLIC KEY-----\n").toString();
    }

    /** Best-effort local signal. Remote server treats this as telemetry, not cryptographic attestation. */
    public static boolean isHardwareBacked() {
        try {
            PrivateKey key=privateKey();
            KeyFactory f=KeyFactory.getInstance(key.getAlgorithm(),KS);
            KeyInfo info=f.getKeySpec(key,KeyInfo.class);
            return info.isInsideSecureHardware();
        } catch(Exception e){ return false; }
    }
    public static boolean isStrongBoxBacked() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;
        try {
            PrivateKey key=privateKey();
            KeyFactory f=KeyFactory.getInstance(key.getAlgorithm(),KS);
            KeyInfo info=f.getKeySpec(key,KeyInfo.class);
            return info.getSecurityLevel()==KeyProperties.SECURITY_LEVEL_STRONGBOX;
        } catch(Exception e){ return false; }
    }

    public static String signBase64(byte[] data) throws Exception {
        Signature s=Signature.getInstance("SHA256withECDSA"); s.initSign(privateKey()); s.update(data);
        return Base64.encodeToString(s.sign(),Base64.NO_WRAP);
    }
}
