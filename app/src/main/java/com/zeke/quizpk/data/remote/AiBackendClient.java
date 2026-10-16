package com.zeke.quizpk.data.remote;

import com.zeke.quizpk.domain.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

public final class AiBackendClient {
    public static final class Failure extends Exception {
        public final String code;public final boolean retrySame;
        public Failure(String code,boolean retrySame) {super(code);this.code=code;this.retrySame=retrySame;}
    }
    public interface Transport { String post(String endpoint,String token,String id,String body) throws Failure; void cancel(); }
    public static final class HttpsTransport implements Transport {
        private volatile HttpsURLConnection active;
        private volatile boolean canceled;
        @Override public String post(String endpoint,String token,String id,String body) throws Failure {
            if(canceled)throw new Failure("CANCELED",true);
            try {
                HttpsURLConnection c=(HttpsURLConnection)new URL(BackendCredentials.validateEndpoint(endpoint)+"/api/quizzes/generate").openConnection();
                active=c; if(canceled)throw new Failure("CANCELED",true);
                c.setConnectTimeout(10000);c.setReadTimeout(65000);c.setInstanceFollowRedirects(false);
                c.setRequestMethod("POST");c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+token);
                c.setRequestProperty("Idempotency-Key",id);
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);
                try(OutputStream out=c.getOutputStream()){out.write(bytes);}
                int status=c.getResponseCode();
                InputStream stream=status>=200&&status<300?c.getInputStream():c.getErrorStream();
                String response=read(stream);
                return decodeHttp(status,response);
            } catch(Failure e) {throw e;}
            catch(SocketTimeoutException e){throw new Failure("TIMEOUT",true);}
            catch(IOException e){throw new Failure(canceled?"CANCELED":"NO_CONNECTION",true);}
            catch(RuntimeException e){throw new Failure("SETUP_REQUIRED",false);}
            finally {HttpsURLConnection c=active;if(c!=null)c.disconnect();active=null;}
        }
        private String read(InputStream stream) throws IOException,Failure {
            if(stream==null)return "";
            try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] bytes=new byte[4096];int n;
                while((n=in.read(bytes))!=-1){if(canceled)throw new Failure("CANCELED",true);
                    if(out.size()+n>180000)throw new Failure("MALFORMED_QUESTIONS",false);out.write(bytes,0,n);}
                return out.toString("UTF-8");
            }
        }
        @Override public void cancel(){canceled=true;HttpsURLConnection c=active;if(c!=null)c.disconnect();}
    }
    public static String decodeHttp(int status,String response) throws Failure {
        if(status==200)return response;
        String code=status==401||status==403?"AUTH_REQUIRED":status==429?"RATE_LIMITED":status==425?"IN_PROGRESS":status==504?"TIMEOUT":"SERVER_UNAVAILABLE";
        try {
            String serverCode=new JSONObject(response).getJSONObject("error").getString("code");
            if(java.util.Arrays.asList("AUTH_REQUIRED","RATE_LIMITED","QUOTA_EXCEEDED","BUSY","PROVIDER_LIMIT","IN_PROGRESS","REQUEST_INTERRUPTED","TIMEOUT","MALFORMED_QUESTIONS","PROVIDER_UNAVAILABLE","SETUP_REQUIRED","TOPIC_REJECTED","INVALID_TOPIC","INVALID_SETTINGS","INVALID_REQUEST","IDEMPOTENCY_CONFLICT").contains(serverCode))code=serverCode;
        }catch(JSONException ignored){ /* Do not render untrusted server bodies. */ }
        if("TIMEOUT".equals(code))code="GENERATION_TIMEOUT";
        throw new Failure(code,status==425);
    }
    private final Transport transport;
    public AiBackendClient(Transport transport){this.transport=transport;}
    public AiQuiz generate(String endpoint,String token,String id,AiQuizRequest request) throws Failure {
        try{return AiQuiz.parse(transport.post(endpoint,token,id,request.toJson().toString()),id,request);}
        catch(JSONException|IllegalArgumentException e){throw new Failure("MALFORMED_QUESTIONS",false);}
    }
    public void cancel(){transport.cancel();}
}
