package com.magen.family.server;

import android.util.Base64;
import com.magen.family.BuildConfig;
import org.json.JSONObject;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/** Verifies the VPS application-level signature, independent of TLS. */
public final class ServerResponseVerifier {
    private ServerResponseVerifier() {}
    public static JSONObject verifyEnvelope(JSONObject envelope) throws Exception {
        if(!"ECDSA_P256_SHA256".equals(envelope.optString("alg"))) throw new SecurityException("unexpected signature algorithm");
        byte[] raw=Base64.decode(envelope.getString("payload_b64"),Base64.DEFAULT);
        byte[] sig=Base64.decode(envelope.getString("signature"),Base64.DEFAULT);
        if(!verifyWithKey(raw,sig,BuildConfig.MAGEN_SERVER_SIGNING_PUB_B64)
                && !verifyWithKey(raw,sig,BuildConfig.MAGEN_SERVER_SIGNING_NEXT_PUB_B64))
            throw new SecurityException("server signature invalid");
        JSONObject signedPayload=new JSONObject(new String(raw,"UTF-8"));
        // payload_b64 is authoritative. The duplicated payload field is only for human/API readability.
        return signedPayload;
    }
    private static boolean verifyWithKey(byte[] raw,byte[] sig,String keyB64){
        if(keyB64==null||keyB64.trim().isEmpty()) return false;
        try{
            byte[] der=Base64.decode(keyB64,Base64.DEFAULT);
            PublicKey pub=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
            Signature v=Signature.getInstance("SHA256withECDSA"); v.initVerify(pub); v.update(raw);
            return v.verify(sig);
        }catch(Exception e){ return false; }
    }
}
