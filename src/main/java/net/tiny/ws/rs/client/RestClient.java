package net.tiny.ws.rs.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.tiny.config.Converter;
import net.tiny.config.JsonParser;

public class RestClient {
    private static final Logger LOGGER =
            Logger.getLogger(RestClient.class.getName());
    /**
     * The date format pattern for RFC 1123.
     */
    private static final String RFC1123_DATE_FORMAT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final SimpleDateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.ENGLISH);
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    static {
        RFC1123_DATE_FORMAT.setTimeZone(GMT_TIME_ZONE);
    }

    public static final String MIME_TYPE_JSON  = "application/json;charset=utf-8";
    public static final String USER_AGENT      = "RestClient Java " + System.getProperty("java.version");
    public static final String BROWSER_AGENT   = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    public static final String ACCEPT          = "application/json, text/*, image/*, audio/*, */*";
    public static final String ACCEPT_ENCODING = "gzip, deflate";
    public static final String ACCEPT_LANGUAGE = "en;q=0.5,ja;q=0.5,zh;q=0.5,kr;q=0.5";
    public static final String ACCEPT_CHARSET  = "iso-8859-5, unicode-1-1;q=0.8";

    private static final String BOUNDARY_LIMIT_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
    private static final int BOUNDARY_LENGTH = 40;

    public static enum HTTP_METHOD {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
        HEAD,
        TRACE,
        LINK,
        UNLINK
    }

    private final Builder builder;

    private RestClient(Builder builder) {
        this.builder = builder;
    }

    public <T> T doGet(final URL url, Class<T> type) throws IOException {
        return execute(url).get(type);
    }

    public Response doGet(final URL url) throws IOException {
        return execute(url).get();
    }

    public <T> Response doPost(final URL url, T entity, Class<T> type) throws IOException {
        return execute(url).post(entity, type, MIME_TYPE_JSON, ACCEPT);
    }

    public <T> Response doPut(final URL url, T entity, Class<T> type) throws IOException {
        return execute(url).put(entity, type, MIME_TYPE_JSON, ACCEPT);
    }

    public Response doDelete(final URL url) throws IOException {
        return execute(url).delete();
    }

    public Response doOptions(final URL url) throws IOException {
        return execute(url).options();
    }

    public Request execute(final String url) throws IOException {
        return execute(new URL(url));
    }

    public Request execute(final URL url) throws IOException {
        return new Request(url);
    }

    protected void connect(final Request request, HTTP_METHOD httpMethod) throws IOException {

        boolean ssl = request.isSSL();
        if(ssl) {
            HostnameVerifier hv = new NonHostnameVerifier();
            // Now you are telling the JRE to trust any https server.
            // If you know the URL that you are connecting to then this should not be a problem
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        }

        // open a URL connection to the server
        HttpURLConnection conn = (HttpURLConnection) request.open();
        if(ssl) {
            HttpsURLConnection httpsconnection = (HttpsURLConnection)conn;
            // SSL No Certificate Validation
            //TODO
            ignoreValidateCertification(httpsconnection);
        }

        conn.setRequestMethod(httpMethod.name());
        // Allow Inputs
        conn.setDoInput(true);
        switch (httpMethod) {
        case POST:
        case PUT:
            // Allow Outputs
            conn.setDoOutput(true);
            break;
        default :
            break;
        }
        // Set request all header
        for (String key : request.headers.keySet()) {
            for (String value : request.getHeaders(key)) {
                conn.setRequestProperty(key, value);
            }
        }

        // conn.setFixedLengthStreamingMode(fileSize);
        // Don't use a cached copy.
        conn.setUseCaches(request.cache);
        //conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setInstanceFollowRedirects(request.redirect);

        conn.connect();
        request.handler.setConnection(conn);
    }

    private void trustAllHttpsCertificates() throws IOException {
        try {
            //  Create a trust manager that does not validate certificate chains:
            TrustManager[] trustAllCerts =  new javax.net.ssl.TrustManager[1];
            TrustManager tm = new IgnoreCertTrustManager();
            trustAllCerts[0] = tm;
            SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, null);
            SSLSocketFactory factory = sslContext.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        } catch(NoSuchAlgorithmException | KeyManagementException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private void ignoreValidateCertification(HttpsURLConnection httpsconnection)
            throws IOException {
        try {
            KeyManager[] km = null;
            TrustManager[] tm = { new EmptyTrustManager() };
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(km, tm, new SecureRandom());
            SSLSocketFactory socketFactory = sslcontext.getSocketFactory();
            //TODO
            //socketFactory...setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
            //
            httpsconnection.setSSLSocketFactory(socketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private class EmptyTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {}

        public void checkServerTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {}

        public X509Certificate[] getAcceptedIssuers() { return null; }
    }

    private static class SimpleProxyAuthenticator extends Authenticator {
        private String username;
        private String password;
        public SimpleProxyAuthenticator(String u, String p){
            this.username = u;
            this.password = p;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    private class NonHostnameVerifier implements HostnameVerifier {
        public boolean verify(String host, SSLSession session) {
            LOGGER.config("Warning: URL Host: " + host + " vs. "  + session.getPeerHost());
            return true;
        }
    }

    // Just add these two functions in your program
    private class IgnoreCertTrustManager implements TrustManager,  X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() { return null; }

        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException { return; }

        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException { return; }

    }

    public class Request {
        URL url;
        boolean ssl;
        Map<String, List<String>> headers = new HashMap<>();
        // Don't use a cached copy.
        boolean cache = false;
        boolean redirect = builder.redirect;
        HTTP_METHOD method;
        String boundary = null;
        ContentsHandler handler;

        private Request(final URL u) {
            url = u;
            ssl = url.getProtocol().startsWith("https");

            header("User-Agent", builder.userAgent);
            // If basic authorization is set - try and set the 'Authorization' header
            if (builder.basicAuth != null) {
                // Basic Authorization should be Base64 encoded
                header("Authorization", builder.basicAuth);
            }
            // Set up proxy authentication
            if(ssl && null != builder.proxyAuth) {
                // SSL HTTPS
                Authenticator.setDefault(builder.proxyAuth);
            } else if(null != builder.proxyBasicAuth) {
                // HTTP
                header("Proxy-Authorization", builder.proxyBasicAuth);
            }
            handler = new ContentsHandler(this);
        }
        private String getUrlBase() {
            String urlStr = url.toString();
            int pos = urlStr.indexOf("/", urlStr.indexOf("://") + 3);
            String base = urlStr;
            if (pos != -1) {
                base = urlStr.substring(0, pos);
            }
            return base;
        }

        public Request path(String path) {
            final String base = getUrlBase();
            final String prefix = path.startsWith("/") ? "" : "/";
            String query = url.getQuery();
            if (null != query && !query.isEmpty()) {
                query = "?" + query;
            } else {
                query = "";
            }
            try {
                url = new URL(base + prefix + path + query);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return this;
        }

        public Request queryParam(String name, Object... values) {
            final String base = getUrlBase();
            final String path = url.getPath();
            String query = url.getQuery();
            if (null != query && !query.isEmpty()) {
                query = "?" + query + "&";
            } else {
                query = "?";
            }
            StringBuffer qb = new StringBuffer();
            for (Object value : values) {
                if(qb.length() > 0) {
                    qb.append("&");
                }
                qb.append(name).append("=");
                try {
                    qb.append(URLEncoder.encode(value.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
            query = query.concat(qb.toString());
            try {
                url = new URL(base + path + query);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return this;
        }

        public Request header(String name, String... values) {
            return header(true, name, values);
        }

        public Request header(boolean single, String name, String... values) {
            if(null != values && values.length == 0)
                return this;
            List<String> list = headers.get(name);
            if (null == list) {
                list = new ArrayList<>();
            }
            if (single) {
                list.clear();
            }
            for (String value : values) {
                if (!list.contains(value)) {
                    list.add(value);
                }
            }
            return this;
        }

        public Request redirect(boolean enable) {
            redirect = enable;
            return this;
        }

        public Request cache(boolean enable) {
            cache = enable;
            return this;
        }


        public Request type(String type) {
            header("Content-Type", type);
            return this;
        }

        public Request userAgent(String ua) {
            header("User-Agent", ua);
            return this;
        }

        public Request cookie(String cookie) {
            header(false, "Cookie", cookie);
            return this;
        }

        public Request cookies(List<String> cs) {
            for (String c : cs) {
                cookie(c);
            }
            return this;
        }

        public Request basicAuth(String auth) {
            if (auth != null && !auth.startsWith("Basic ")) {
                throw new IllegalArgumentException(String.format("Not is Basic Authorization format '%s'", auth));
            }
            header(false, "Authorization", auth);
            return this;
        }

        public Request proxyAuth(String auth) {
            if (auth != null && !auth.startsWith("Basic ")) {
                throw new IllegalArgumentException(String.format("Not is Basic Authorization format '%s'", auth));
            }
            header(false, "Proxy-Authorization", auth);
            return this;
        }

        public Request proxyAuth(Authenticator pa) {
            // SSL HTTPS
            Authenticator.setDefault(pa);
            return this;
        }

        public boolean isSSL() {
            return ssl;
        }
        public List<String> getHeaders(String name) {
            for (String key : headers.keySet()) {
                if (key.equalsIgnoreCase(name))
                    return headers.get(key);
            }
            return null;
        }

        public String getHeader(String name) {
            List<String> values = getHeaders(name);
            if (null != values)
                return values.get(0);
            return null;
        }

        HttpURLConnection open() throws IOException {
            return (HttpURLConnection)url.openConnection();
        }

        //Do HTTP method
        public Response get() throws IOException {
            return get(MIME_TYPE_JSON);
        }

        public Response get(String mediaType) throws IOException {
            header(false, "Accept", mediaType);
            method = HTTP_METHOD.GET;
            connect(this, method);
            return new Response(this);
        }

        public <T> T get(Class<T> type) throws IOException {
            method = HTTP_METHOD.GET;
            connect(this, method);
            Response response = new Response(this);
            return response.readEntity(type);
        }

        public Response post(String body, String mimeType) throws IOException {
            return post(body, String.class, mimeType, ACCEPT);
        }

        public <T> Response post(T entiy, Class<T> type, String mimeType, String mediaType) throws IOException {
            method = HTTP_METHOD.POST;
            //boundary = generatorBoundary(); //TODO
            if (boundary != null && !boundary.isEmpty()) {
                header("Content-Type", "multipart/form-data;boundary=" + boundary);
            } else {
                header("Content-Type", mimeType);
            }
            if (null != mediaType) {
                header(false, "Accept", mediaType);
            }
            connect(this, method);
            return new Response(this);
        }

        public Response put(String body, String mimeType) throws IOException {
            return put(body, String.class, mimeType, ACCEPT);
        }

        public <T> Response put(T entiy, Class<T> type, String mimeType, String mediaType) throws IOException {
            method = HTTP_METHOD.PUT;
            //boundary = generatorBoundary(); //TODO
            if (boundary != null && !boundary.isEmpty()) {
                type("multipart/form-data;boundary=" + boundary);
            } else {
                type(mimeType);
            }
            if (null != mediaType) {
                header(false, "Accept", mediaType);
            }
            connect(this, method);
            return new Response(this);
        }

        public Response delete() throws IOException {
            method = HTTP_METHOD.DELETE;
            connect(this, method);
            return new Response(this);
        }

        public Response options() throws IOException {
            method = HTTP_METHOD.OPTIONS;
            connect(this, method);
            return new Response(this);
        }
    }

    public class Response {
        final Request request;
        final int status;
        final Map<String, List<String>> headers;
        final ContentsHandler handler;
        final Converter converter;
        private Response(Request req) throws IOException {
            request = req;
            converter = new Converter();
            handler = request.handler;
            handler.response(this);
            status  = handler.getStatusCode();
            headers = handler.getResponseHeaders();
            Collections.unmodifiableMap(headers);
        }

        public int getStatus() {
            return status;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public List<String> getHeaders(String name) {
            if (headers.containsKey(name)) {
                return headers.get(name);
            }
            // Capitalization sensitive
            for (String key : headers.keySet()) {
                if (name.equalsIgnoreCase(key))
                    return headers.get(key);
            }
            return null;
        }

        public String getHeader(String name) {
            List<String> values = getHeaders(name);
            if (null != values)
                return values.get(0);
            return null;
        }

        public URI getLocation() {
            String value = getHeader("Location");
            try {
                return value != null ? new URI(value) : null;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

        public int getContentLength() {
            String value = getHeader("Content-length");
            return value != null ? Integer.parseInt(value) : -1;
        }

        public Date getLastModified() {
            String value = getHeader("Last-Modified");
            if (value == null) return null;
            try {
                synchronized(RFC1123_DATE_FORMAT) {
                    return RFC1123_DATE_FORMAT.parse(value);
                }
            } catch (final ParseException pe) {
                return null;
            } finally {
                 // parse can change time zone -> set it back to GMT
                RFC1123_DATE_FORMAT.setTimeZone(GMT_TIME_ZONE);
            }
        }

        public Map<String,String> getCookies() {
            Map<String, String> cookies = new HashMap<>();
            //TODO
            return cookies;
        }

        public String getEntityTag() {
            //TODO
            return null;
        }

        public boolean hasEntity() {
            return (getContentLength() > 0 || getHeader("Content-Type") != null);
        }

        public <T> T readEntity(Class<T> type) throws IOException {
            final String body = getEntity();
            if (body == null || body.isEmpty())
                return null;
            if (String.class.equals(type)) {
                return type.cast(body);
            }
            String mediaType = getHeader("Content-Type");
            if (mediaType != null && mediaType.toLowerCase().startsWith("application/json")) {
                return JsonParser.unmarshal(body, type);
            }
            return converter.convert(body, type);
        }

        public String getEntity() throws IOException {
            if (!hasEntity()) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int size = handler.cacheContents(out);
            out.close();
            if (size > 0) {
                return new String(out.toByteArray());
            }
            return "";
        }

        public void close() {
            handler.close();
        }
    }

    private class ContentsHandler extends WebContentsHandler {
        final Request request;
        Response response;

        private ContentsHandler(Request req) {
            request = req;
        }

        public ContentsHandler response(Response res) {
            response = res;
            return this;
        }

        @Override
        protected String getHeader(String name) {
            return response.getHeader(name);
        }
    }

    protected static String generatorBoundary() {
        final Random rand = new Random();
        final StringBuffer boundary = new StringBuffer();
        for (int i = 0; i < BOUNDARY_LENGTH; i++) {
            int r = rand.nextInt(BOUNDARY_LIMIT_CHARS.length());
            boundary.append(BOUNDARY_LIMIT_CHARS.substring(r, r + 1));
        }
        return boundary.toString();
    }


    public static class Builder {
        String userAgent = USER_AGENT;
        boolean redirect = true;
        String basicAuth = null;
        String proxyBasicAuth = null;
        Authenticator proxyAuth = null;

        public Builder userAgent(String ua) {
            userAgent = ua;
            return this;
        }

        public Builder redirect(boolean enable) {
            redirect = enable;
            return this;
        }

        /**
         * Setting HTTP BASIC Authorization
         * @param name The name of authorization user
         *
         * @param pass
         *            The password of authorization user
         */
        public Builder setBasicAuthorization(String name, String pass) {
            // Basic Authorization should be Base64 encoded
            String basicAuth = name + ":" + pass;
            basicAuth = "Basic " + new String(Base64.getEncoder().encode(basicAuth.getBytes()));
            return this;
        }

        public Builder setProxyAuthorization(String name, String pass) {
            // Basic Authorization should be Base64 encoded
            String basicAuth = name + ":" + pass;
            proxyBasicAuth = "Basic " + new String(Base64.getEncoder().encode(basicAuth.getBytes()));
            proxyAuth = new SimpleProxyAuthenticator(name , pass);
            return this;
        }

        public RestClient build() {
            return new RestClient(this);
        }
    }
}
