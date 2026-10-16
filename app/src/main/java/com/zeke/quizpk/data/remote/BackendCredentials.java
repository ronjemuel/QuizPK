package com.zeke.quizpk.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Individual backend login only. Never contains any provider credential. */
public final class BackendCredentials {
    private static final String ALIAS="quizpk_backend_login";
    private final SharedPreferences prefs;
    public BackendCredentials(Context context) { prefs=context.getSharedPreferences("quizpk_backend_login",Context.MODE_PRIVATE); }
    public String endpoint() { return prefs.getString("endpoint",""); }
    public static String validateEndpoint(String input) {
        try {
            URI uri=new URI(input.trim());
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null
                    || uri.getQuery()!=null || uri.getFragment()!=null || !(uri.getPath().isEmpty()||"/".equals(uri.getPath()))
                    || input.length()>240) throw new IllegalArgumentException();
            return "https://"+uri.getRawAuthority();
        } catch(Exception e) { throw new IllegalArgumentException("HTTPS_ENDPOINT_REQUIRED"); }
    }
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    public void save(String endpoint,String token) {
        endpoint=validateEndpoint(endpoint);
        if(!token.matches("[A-Za-z0-9_-]{43,128}"))throw new IllegalArgumentException("INVALID_ACCESS_TOKEN");
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
            String encrypted=Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
            if(!prefs.edit().putString("endpoint",endpoint).putString("token",encrypted)
                    .putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).commit())throw new Exception();
        } catch(Exception e) { throw new IllegalStateException("LOGIN_STORAGE_FAILED"); }
    }
    public String token() {
        if(!prefs.contains("token"))return "";
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),
                    new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(prefs.getString("token",""),Base64.NO_WRAP)),StandardCharsets.UTF_8);
        } catch(Exception e) { throw new IllegalStateException("LOGIN_REQUIRED"); }
    }
    public void clear() { prefs.edit().clear().apply(); }
}
