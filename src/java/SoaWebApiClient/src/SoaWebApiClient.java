import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SoaWebApiClient {

    private String headnode;
    private String brokernode;
    private String username;
    private String password;
    private String servicename;
    private String basicauthinfo;
    private String defaultBatchId = "DEFAULTBATCHID";
    private int sessionId = -1;
    private String userdataSeparator;
    private String responseSeparator;

    public SoaWebApiClient(String headnode, String servicename,
            String username, String password) {
        this.headnode = headnode;
        this.servicename = servicename;
        this.username = username;
        this.password = password;

        String plainauth = String.format("%s:%s", this.username, this.password);
        this.basicauthinfo = new sun.misc.BASE64Encoder().encode(plainauth
                .getBytes());

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, getTrustManager(), new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection
                    .setDefaultHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String urlHostname,
                                javax.net.ssl.SSLSession _session) {
                            return true;
                        }
                    });
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        char[] tmpArrayA = { (char) Integer.parseInt("EF", 16),
                (char) Integer.parseInt("BF", 16),
                (char) Integer.parseInt("BF", 16), };
        char[] tmpArrayB = { (char) Integer.parseInt("00", 16) };
        userdataSeparator = new String(tmpArrayA);
        responseSeparator = new String(tmpArrayB);

    }

    public int createSession() throws Throwable {
        URL url = null;
        HttpsURLConnection createSession = null;
        int responseCode = -1;
        try {
            url = new URL(String.format(
                    "https://%s/SOA/sessions/Create?durable=false",
                    this.headnode));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {

            createSession = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out
                .println(String.format("Create session...%s", url.toString()));
        try {
            createSession.setRequestMethod("POST");
            createSession.setDoOutput(true);
            createSession.setRequestProperty("Authorization", "Basic "
                    + this.basicauthinfo);
            createSession.setRequestProperty("CONTENT-TYPE",
                    "application/json; charset=utf-8");
            createSession.setRequestProperty("Accept", "application/json");
            createSession.setRequestProperty("TransferEncoding", "chunked");
            createSession.setReadTimeout(60000);
            createSession.connect();
            OutputStreamWriter wr = new OutputStreamWriter(
                    createSession.getOutputStream());
            String sessionStartInfo = String
                    .format("{\"ServiceName\":\"%s\",\"Username\":\"%s\",\"Password\":\"%s\",\"MaxMessageSize\":2147483647,\"Runtime\":-1}",
                            this.servicename,
                            this.username.replaceAll("\\\\", "\\\\\\\\"),
                            this.password);
            wr.write(sessionStartInfo);
            wr.flush();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            char[] buffer = new char[0x10000];
            StringBuilder sb = new StringBuilder();
            Reader reader = new InputStreamReader(
                    createSession.getInputStream());
            int len;
            do {
                len = reader.read(buffer, 0, buffer.length);
                if (len > 0) {
                    sb.append(buffer, 0, len);
                }
            } while (len >= 0);
            System.out
                    .println(String.format("WebSessionInfo %s", sb.toString()));
            Pattern pattern = Pattern
                    .compile("\"Id\":(\\d+),\"Secure\":(.*),\"ServiceOperationTimeout\":(\\d+),\"ServiceVersion\":(.*),\"TransportScheme\":(\\d+),\"UseInprocessBroker\":(.*),\"BrokerNode\":\"(.*)\",\"IsDurable\":(.*)");
            Matcher matcher = pattern.matcher(sb.toString());
            if (matcher.find()) {
                this.sessionId = Integer.parseInt(matcher.group(1));
                this.brokernode = matcher.group(7);
                System.out.println(String.format("brokernode: %s",
                        this.brokernode));
            }
            responseCode = createSession.getResponseCode();
            System.out.println(String.format(
                    "Session %d is created successfully!", this.sessionId));

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        throwIfError(responseCode, "CreateSession failed.");
        
        return this.sessionId;
    }

    public void closeSession() throws Throwable {
        URL url = null;
        HttpsURLConnection closeSession = null;
        int responseCode = -1;
        try {
            url = new URL(String.format("https://%s/SOA/sessions/%d/Close",
                    this.headnode, this.sessionId));
            closeSession = (HttpsURLConnection) url.openConnection();
            closeSession.setRequestMethod("GET");
            closeSession.setRequestProperty("Authorization", "Basic "
                    + this.basicauthinfo);
            closeSession.setRequestProperty("CONTENT-TYPE",
                    "application/json; charset=utf-8");
            closeSession.connect();
            responseCode = closeSession.getResponseCode();
            System.out.println(String.format("Session closed. %d", responseCode));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        throwIfError(responseCode, "CloseSession failed.");
    }

    public void attachSession(int attachSessionId) throws Throwable {
        URL url = null;
        HttpsURLConnection attachSession = null;
        int responseCode = -1;
        String websessionInfo = null;
        System.out.println(String.format("Try to attach session %d...", attachSessionId));
        try {
            url = new URL(String.format(
                    "https://%s/SOA/sessions/%d/Attach?durable=false",
                    this.headnode, attachSessionId));
            attachSession = (HttpsURLConnection) url.openConnection();
            attachSession.setRequestMethod("GET");
            attachSession.setRequestProperty("Authorization", "Basic "
                    + this.basicauthinfo);
            attachSession.setRequestProperty("Accept", "application/json");
            attachSession.setRequestProperty("CONTENT-TYPE",
                    "application/json; charset=utf-8");
            attachSession.setDoInput(true);
            attachSession.setReadTimeout(120000);
            
            InputStreamReader reader = new InputStreamReader(
                    attachSession.getInputStream());
            StringBuilder sb = new StringBuilder();
            Reader in = new BufferedReader(reader);
            int val;
            while ((val = in.read()) > -1) {
                sb.append((char) val);
            }
            in.close();
            websessionInfo = sb.toString();
            System.out.println(websessionInfo);
            Pattern pattern = Pattern
            .compile("\"Id\":(\\d+),\"Secure\":(.*),\"ServiceOperationTimeout\":(\\d+),\"ServiceVersion\":(.*),\"TransportScheme\":(\\d+),\"UseInprocessBroker\":(.*),\"BrokerNode\":\"(.*)\",\"IsDurable\":(.*)");
            Matcher matcher = pattern.matcher(websessionInfo);
            if (matcher.find()) {
                this.sessionId = Integer.parseInt(matcher.group(1));
                this.brokernode = matcher.group(7);
                System.out.println(String.format("brokernode: %s",
                        this.brokernode));
            }
            responseCode = attachSession.getResponseCode();

            System.out.println(String.format("Session %d attached successfully", this.sessionId));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        throwIfError(responseCode, "AttachSession failed.");
    }

    public void sendRequest(String[] requests, String[] userdata) throws Throwable {
        sendRequest(requests, userdata, this.defaultBatchId);
    }
    
    public void sendRequest(String[] requests, String[] userdata, boolean commit) throws Throwable {
        sendRequest(requests, userdata, commit, this.defaultBatchId);
    }
    
    public void sendRequest(String[] requests, String[] userdata, String batchId) throws Throwable {
        sendRequest(requests, userdata, true, batchId);
    }

    public void sendRequest(String[] requests, String[] userdata, boolean commit, String batchId) throws Throwable {
        System.out.println("Sending request");
        URL url = null;
        HttpURLConnection sendRequest = null;
        int responseCode = -1;
        try {
            url = new URL(
                    String.format(
                            "https://%s/SOA/sessions/%d/batches/%s?genericservice=true&commit=%b",
                            this.brokernode, this.sessionId, batchId, commit));
            System.out.println(url.toString());
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            sendRequest = (HttpURLConnection) url.openConnection();
            sendRequest.setRequestMethod("POST");
            sendRequest.setRequestProperty("Authorization", "Basic "
                    + this.basicauthinfo);
            sendRequest.setRequestProperty("CONTENT-TYPE",
                    "application/octet-stream");
            sendRequest.setRequestProperty("TransferEncoding", "chunked");
            sendRequest.setDoOutput(true);
            sendRequest.setReadTimeout(60000);
            sendRequest.connect();
            OutputStreamWriter out = new OutputStreamWriter(
                    sendRequest.getOutputStream());
            for (int i = 0; i < requests.length; i++) {
                out.write(requests[i]);
                out.write(userdataSeparator);
                out.write(userdata[i]);
                out.write(responseSeparator);
            }
            out.flush();

            InputStream is = sendRequest.getInputStream();
            is.close();
            int respCode = sendRequest.getResponseCode();
            System.out.println(String.format("SendRequest complete %d",
                    respCode));
            responseCode = sendRequest.getResponseCode();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        throwIfError(responseCode, "SendRequest failed");
    }

    public String getResponse() throws Throwable {
        return getResponse(this.defaultBatchId);
    }

    public String getResponse(String batchId) throws Throwable {
        return getResponse(batchId, "", "", -1, true);
    }
    
    public String getResponse(int count, boolean startFromBenginning) throws Throwable {
        return getResponse(this.defaultBatchId, "", "", count, startFromBenginning);
    }
    
    public String getResponse(String batchId, String action, String clientdata, int count, boolean startFromBenginning) throws Throwable {
        URL url = null;
        String response = null;
        int responseCode = -1;
        try {
            url = new URL(
                    String.format(
                            "https://%s/SOA/sessions/%d/batches/%s?genericservice=true&action=%s&clientdata=%s&count=%d&reset=%b",
                            this.brokernode, this.sessionId, batchId, action, clientdata, count, startFromBenginning));
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        HttpURLConnection getResponse = null;
        try {
            getResponse = (HttpURLConnection) url.openConnection();
            getResponse.setRequestMethod("GET");
            getResponse.setRequestProperty("Authorization", "Basic "
                    + this.basicauthinfo);
            getResponse.setDoInput(true);
            getResponse.setReadTimeout(120000);

            InputStreamReader reader = new InputStreamReader(
                    getResponse.getInputStream());
            StringBuilder sb = new StringBuilder();
            Reader in = new BufferedReader(reader);
            int val;
            while ((val = in.read()) > -1) {
                sb.append((char) val);
            }
            in.close();
            response = sb.toString();
            String[] responses;
            String[] parts;
            responses = response.split(responseSeparator);
            for (int i = 0; i < responses.length - 1; i++) {
                parts = responses[i].split(userdataSeparator);
                System.out.println(String.format(
                        "Action: %s Response: %s UserData: %s", parts[0],
                        parts[1], parts[2]));
            }
            responseCode = getResponse.getResponseCode();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        throwIfError(responseCode, "GetResponse failed");
        return response;
    }
    
    public void endRequests() throws Throwable {
        endRequests(this.sessionId, this.defaultBatchId);
    }
    
    public void endRequests(int sessionId, String batchId) throws Throwable {
        URL url = null;
        HttpsURLConnection endRequests = null;
        int responseCode = -1;
        System.out.println(String.format("Batch %s end requests...", batchId));
        
        try {
            url = new URL(String.format("https://%s/SOA/sessions/%d/batches/%s/Commit", this.headnode, sessionId, batchId));
            endRequests = (HttpsURLConnection) url.openConnection();
            endRequests.setRequestMethod("GET");
            endRequests.setRequestProperty("Authorization", "Basic " + this.basicauthinfo);
            endRequests.connect();
            responseCode = endRequests.getResponseCode();
            System.out.println(String.format("Batch %s commited. %d", batchId, responseCode));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        throwIfError(responseCode, "EndRequest failed");
    }
    
    public void purgeBatch() throws Throwable {
        purgeBatch(this.sessionId, this.defaultBatchId);
    }
    
    public void purgeBatch(String batchId) throws Throwable {
        purgeBatch(this.sessionId, batchId);
    }
    
    public void purgeBatch(int sessionId, String batchId) throws Throwable {
        URL url = null;
        HttpsURLConnection purgeClient = null;
        int responseCode = -1;
        System.out.println(String.format("Purge batch %s ...", batchId));
        try {
            url = new URL(String.format("https://%s/SOA/sessions/%d/batches/%s/Purge", this.headnode, sessionId, batchId));
            purgeClient = (HttpsURLConnection) url.openConnection();
            purgeClient.setRequestMethod("GET");
            purgeClient.setRequestProperty("Authorization", "Basic " + this.basicauthinfo);
            purgeClient.connect();
            responseCode = purgeClient.getResponseCode();
            System.out.println(String.format("Batch %s purged. %d", batchId, responseCode));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        throwIfError(responseCode, "GetResponse failed");
    }
    
    public String getBatchStatus() throws Throwable {
        return getBatchStatus(this.sessionId, this.defaultBatchId);
    }
    
    public String getBatchStatus(int sessionId, String batchId) throws Throwable {
        URL url = null;
        HttpsURLConnection getBatchStatus = null;
        String batchStatus = null;
        int responseCode = -1;
        try {
            url = new URL(String.format("https://%s/SOA/sessions/%d/batches/%s/Status", this.headnode, sessionId, batchId));
            getBatchStatus = (HttpsURLConnection) url.openConnection();
            getBatchStatus.setRequestMethod("GET");
            getBatchStatus.setRequestProperty("Authorization", "Basic " + this.basicauthinfo);
            getBatchStatus.setRequestProperty("Accept", "application/json");
            getBatchStatus.setDoInput(true);
            getBatchStatus.setReadTimeout(120000);

            InputStreamReader reader = new InputStreamReader(
                    getBatchStatus.getInputStream());
            StringBuilder sb = new StringBuilder();
            Reader in = new BufferedReader(reader);
            int val;
            while ((val = in.read()) > -1) {
                sb.append((char) val);
            }
            in.close();
            batchStatus = sb.toString();
            responseCode = getBatchStatus.getResponseCode();
            System.out.println(String.format("Batch %s status: %s", batchId, batchStatus));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        throwIfError(responseCode, "GetBatchStatus failed");
        return batchStatus;
    }
    
    public int getSessionId() {
        return this.sessionId;
    }
    
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    private TrustManager[] getTrustManager() {
        TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String t) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String t) {
            }
        } };
        return certs;
    }

    private void throwIfError(int responseCode, String errorMsg) throws Throwable {
        if(responseCode != 200 && responseCode != 201) {
                throw new Exception(errorMsg);
        }
    }
}
