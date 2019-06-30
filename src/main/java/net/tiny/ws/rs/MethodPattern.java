package net.tiny.ws.rs;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

/**
 * REST 表述性状态传递的URI路径和服务方法匹配器
 */
public class MethodPattern implements Comparable<MethodPattern>, RestServiceHandler, Constants {

    private static Logger LOGGER = Logger.getLogger(MethodPattern.class.getName());

    private String path;
    private String pattern;
    private Object service = null;
    private Class<?> serviceClass;
    private String httpMethod = null;
    private Class<?> returnType = null;
    private final Method method;
    private final Hitting<MethodPattern> hit;
    /** 模式:单例模式/还是每次请求生成新对象 默认是instance模式 */
    private Mode mode = Mode.instance;
    private String[] requestTypes;
    private String[] mediaTypes;

    /**
     *
     *
     * @param path
     * @param pattern
     * @param httpMethod
     * @param serviceClass
     * @param service
     * @param method
     */
    public MethodPattern(final String path, final String pattern, final String httpMethod, final String mediaType,
            final Class<?> serviceClass, Object service, final Method method) {
        if(null == path || path.isEmpty()) {
            throw new  IllegalArgumentException(String.format("'%1$s' not found  annotation @Path.", serviceClass));
        }
        this.path = path;
        this.httpMethod = httpMethod;
        this.serviceClass = serviceClass;
        if(service != null) {
            this.mode = Mode.singleton;
            this.service = service;
        }
        this.method = method;
        if(null != pattern) {
            // Format pattern string
            if (!pattern.startsWith("/")) {
                this.pattern = this.path +  "/" + pattern;
            } else {
                this.pattern = this.path + pattern;
            }
            if (pattern.endsWith("/")) {
                this.pattern = this.pattern.substring(0, this.pattern.length() - 1);
            }
        } else {
            this.pattern = this.path + PathPattern.generatorPattern(method);
        }
        PathPattern.checkPattern(this.pattern);
        this.hit = new Hitting<MethodPattern>(this);

        Produces  produces  = method .getAnnotation(Produces.class);
        if(null != produces) {
            this.mediaTypes = produces.value();
        } else {
            this.mediaTypes = new String[] {mediaType};
        }
        Arrays.sort(this.mediaTypes);

        Consumes consumes = method .getAnnotation(Consumes.class);
        if(null != consumes) {
            this.requestTypes = consumes.value();
        }
        this.returnType = method.getReturnType();
    }

    public Hitting<?> getHitting() {
        return this.hit;
    }

    public Mode getMode() {
        return this.mode;
    }

    public Class<?> getResponseType() {
        return this.returnType;
    }

    public String[] getRequestTypes() {
        return this.requestTypes;
    }

    public String[] getMediaTypes() {
        return this.mediaTypes;
    }

    @Override
    public boolean acceptableMediaType(String type) {
        if(type== null) {
            if(this.mediaTypes.length == 0) {
                return true;
            }
            return false;
        }
        String acceptType = type.toLowerCase();
        for(String mediaType : mediaTypes) {
            if(acceptType.contains(mediaType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getPattern() {
        return this.pattern;
    }

    /**
     * 判断uri是否匹配pattern
     *
     * @param realUrl
     * @param requestMethod
     * @return 0:匹配 0以外:不匹配
     * @throws UnsupportedEncodingException
     */
    public int hit(final String realUrl,  final String requestMethod, final Map<String, Object> args) throws UnsupportedEncodingException {
        int hitted = 1;
        if(validatePattern(realUrl, requestMethod, args)) {
            // Hit it
            hitted = 0;
        } else {
            //int compareRet = super.comparePatternAndUrl(this.path + this.pattern, realUrl);
            int compareRet = this.pattern.compareTo(realUrl);
            if(compareRet == 0) {
                hitted = 1;
            } else {
                hitted = compareRet;
            }
        }
        if (hitted==0) {
            LOGGER.fine(String.format("[REST] '%s' hitted pattern '%s'" , realUrl, this.pattern));
        }
        return hitted;
    }


    /**
     * 判断HTTP访问的URI是否匹配
     *
     * @param uri
     * @param requestMethod
     * @throws UnsupportedEncodingException
     */
    public boolean validatePattern(final String uri, final String requestMethod) throws UnsupportedEncodingException {
        return validatePattern(uri, requestMethod, null);
    }

    /**
     * 判断HTTP访问的URI是否匹配
     *
     * @param uri
     * @param requestMethod
     * @throws UnsupportedEncodingException
     */
    public boolean validatePattern(final String uri, final String requestMethod, final Map<String, Object> args) throws UnsupportedEncodingException {
        if(this.httpMethod != null) {
            if(!this.httpMethod.equals(requestMethod)) {
                return false;
            }
        }
        if(!uri.startsWith(path)) {
            return false;
        }
        return matchPattern(uri, args);
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public Object getTarget() throws Exception {
        Object target = null;
        switch(mode) {
        case instance:
            target = serviceClass.newInstance();
            break;
        case singleton:
            target = service;
            break;
        }
        return target;
    }

    @Override
    public Object invoke(final Object[] args) {
        try {
            return method.invoke(getTarget(), args);
        } catch (Exception ex) {
            throw new WebApplicationException(ex.getMessage(), ex);
        }
    }

    private String getParameterKey(Annotation[] annotations) {
        for(Annotation annotation : annotations) {
            if(annotation instanceof PathParam) {
                return ((PathParam)annotation).value();
            } else if(annotation instanceof QueryParam) {
                return ((QueryParam)annotation).value();
            } else if(annotation instanceof MatrixParam) {
                return ((MatrixParam)annotation).value();
            } else if(annotation instanceof FormParam) {
                return ((FormParam)annotation).value();
            }
        }
        return null;
    }

    Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        String[] pair = query.split("&");
        for(int i=0; i<pair.length; i++) {
            String[] nv = pair[i].split("=");
            params.put(nv[0], nv[1]);
        }
        return params;
    }

    boolean matchQueryParam(String query, final Map<String, Object> args) throws UnsupportedEncodingException {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        Map<String, String> params = parseQuery(query);
        List<String> names = new ArrayList<String>();
        for(int i=0; i<paramTypes.length; i++) {
            String key = getParameterKey(annotations[i]);
            if(null != key) {
                names.add(key);
                if(params.containsKey(key)) {
                    String value = params.get(key);
                    args.put(key, URLDecoder.decode(value, "UTF-8"));
                }
            }
        }
        // Check validate parameter
        Set<String> keys = params.keySet();
        for(String key : keys) {
            if(!names.contains(key)) {
                return false;
            }
        }
        return true;
    }

    boolean matchPattern(final String patternSeg, final String uriSeg, final String delim, boolean validate, final Map<String, Object> args) throws UnsupportedEncodingException {
        if (!patternSeg.contains("{")) {
            // 不含”{...}“定型文字比较
            //URI 'query?name1=value1&name2=value2&name3=value3'的情况下解析
            boolean match =uriSeg.startsWith(patternSeg);
            int pos = uriSeg.indexOf("?");
            if(match && pos != -1) {
                String query = uriSeg.substring(pos+1);
                match = matchQueryParam(query, args);
            }
            return match;
        }
        //含”{arg : [regex]}“ 或是  ”{arg = [regex]}“文字比较
        int pos = patternSeg.indexOf(delim);
        if(pos >0 ) {
            String argName = "";
            String value = uriSeg;
            if(delim.equals(":")) {
                argName = patternSeg.substring(1, pos).trim();
            } else if(delim.equals("=")) {
                argName = patternSeg.substring(1, pos).trim();
                value = uriSeg.substring(pos).trim();
            }
            String  regex = patternSeg.substring(pos+1, patternSeg.length()-1).trim();
            if( Pattern.matches(regex, value) ) {
                if(null != args) {
                    args.put(argName, URLDecoder.decode(value, "UTF-8"));
                }
                return true;
            } else {
                return false;
            }
        }
        //"name = value"文字型
        if(validate) {
            pos = uriSeg.indexOf("=");
            String name = uriSeg.substring(0, pos);
            String value = uriSeg.substring(pos+1);
            if( patternSeg.substring(1, patternSeg.length()-1).equalsIgnoreCase(name) ) {
                if(null != args) {
                    args.put(name, URLDecoder.decode(value, "UTF-8"));
                }
                return true;
            } else {
                return false;
            }
        }
        if(null != args) {
            args.put(patternSeg.substring(1, patternSeg.length() - 1), URLDecoder.decode(uriSeg, "UTF-8"));
        }
        return true;
    }

    boolean matchGroupPattern(final String[] patternSegs, final String[] uriSegs, final String delim, final Map<String, Object> args) throws UnsupportedEncodingException {
        if(patternSegs.length < uriSegs.length) {
            return false;
        }
        final int count = uriSegs.length;
        for (int i = 0; i < count; i++) {
            if(!matchPattern(patternSegs[i], uriSegs[i], delim, true, args)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断URI每个节是否匹配，如下：<br/>
     *  /u/{id} 与 /u/111111
     *
     * @param patternSeg
     * @param uriSeg
     * @throws UnsupportedEncodingException
     */
    boolean matchPattern(final String patternSeg, final String uriSeg, final Map<String, Object> args) throws UnsupportedEncodingException {
        //含”path?{arg1}&{arg2}“ 或是 ”path?{arg1=[regex1]}&{arg2=[regex2]}“文字比较
        int pos = patternSeg.indexOf("?");
        if(pos >0) {
            if(uriSeg.indexOf("?")  != pos) {
                return false;
            }
            pos++;
            return matchGroupPattern(patternSeg.substring(pos).split("&"), uriSeg.substring(pos).split("&"), "=", args);
        }
        //含”path;{arg1=[regex1]};{arg2=[regex2]}“文字比较
        pos = patternSeg.indexOf(";");
        if(pos >0) {
            pos++;
            int p = uriSeg.indexOf(";") + 1;
            return matchGroupPattern(patternSeg.substring(pos).split(";"), uriSeg.substring(p).split(";"), "=", args);
        }

        // 不含”{...}“定型文字比较
        //含”{arg : [regex]}“文字比较
        return matchPattern(patternSeg, uriSeg, ":", false, args);
    }

    /**
     * 判断URI是否匹配，如下：<br/>
     *  /u/{id} 与 /u/111111
     *
     * @param url
     * @throws UnsupportedEncodingException
     */
    public boolean matchPattern(final String uri, final Map<String, Object> args) throws UnsupportedEncodingException {
        final String[] patternSegs = pattern.split("/");
        final String[] uriSegs = uri.split("/");

        if(patternSegs.length < uriSegs.length) {
            //Exception error =  new IllegalArgumentException(String.format("'%1$s' argument number %2$d > %3$d", uri,  uriSegs.length, patternSegs.length));
            return false;
        }
        final int count = uriSegs.length;
        for (int i = 0; i < count; i++) {
            // URI每个节是否匹配
            if(!matchPattern(patternSegs[i], uriSegs[i], args)) {
                //Exception error =  new IllegalArgumentException(String.format("'%1$s'  Invalidated. '%2$s' not matched '%3$s'", uri,  uriSegs[i],  patternSegs[i]));
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(final MethodPattern target) {
        if (target == null) {
            return 1;
        }
        return getPattern().compareTo(target.getPattern());
    }

}
