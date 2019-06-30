package net.tiny.ws.rs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.Converter;

public class RestServiceFactory {

    private static Logger LOGGER = Logger.getLogger(RestServiceFactory.class.getName());

    private static final String REGEX_COOKIE_NAME_VALUE = "^(\\w+)=(.*)$";
    private static final Pattern COOKIE_PATTERN = Pattern.compile(REGEX_COOKIE_NAME_VALUE);

    @Resource(name = "restApplication")
    private Application application;
    private Vector<RestServiceWrapper> servicePatterns = new Vector<RestServiceWrapper>();
    private boolean initing = false;
    private boolean changed = true;
    private Converter converter = new Converter();

    public Application getApplication() {
        return this.application;
    }

    public void setApplication(Application application) {
        this.application = application;
        this.changed = true;
        setup();
    }

    /**
     * 初始化RestService配置
     */
    public synchronized void setup()  {
        if(!changed)
            return;
        initing = true;
        servicePatterns .clear();
        try {
            Set<Class<?>> serviceClasses = application.getClasses();
            for(Class<?> serviceClass : serviceClasses) {
                Object target = serviceClass.newInstance();
                RestServiceWrapper wrapper = new RestServiceWrapper(target);
                servicePatterns.add(wrapper);
                LOGGER.fine(String.format("[REST] - %s", wrapper.toString()));
            }
            if(this.servicePatterns.isEmpty()) {
                throw new WebApplicationException("One REST service also could not be found.");
            }
            // 对配置项进行排序，方便后面的查找
            Collections.sort(servicePatterns);
            //检查是否有重复的url
            checkDuplicateUrl();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new WebApplicationException(e.getMessage(), e);
        } finally {
            initing = false;
            changed = false;
        }
    }


    /**
     * Servlet通过访问要求取得相应的RestService句柄，并把解析的参数放入Map里
     *
     * @param request
     * @param args
     * @return 句柄
     */
    public RestServiceHandler getRestServiceHandler(final String realUrl, final String httpMthod, final Map<String, Object> args) throws IOException {
        Hitting<?> hit = hit(realUrl, httpMthod, args);
        if(null != hit) {
            return hit.getTarget(RestServiceHandler.class);
        }
        return null;
    }

    public Object[] convertArguments(final HttpExchange he, final Map<String, Object> args, final Method method) throws UnsupportedEncodingException {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        Object[] argements = new Object[paramTypes.length];
        for(int i=0; i<argements.length; i++) {
            String key = getParameterKey(annotations[i]);
            if(null != key) {
                argements[i] = convertParameter(args, key, annotations[i], paramTypes[i]);
            } else {
                argements[i] = convertParameter(he, annotations[i], paramTypes[i]);
            }
        }
        return argements;
    }

    /**
     * 注解
     * @param annotations
     * @return
     */
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

    private Object convertParameter(final Map<String, Object> args, String key, Annotation[] annotations, Class<?> paramType) {
        Object value  = args.get(key);
        if(value != null && value.getClass().isArray() && !paramType.isArray()) {
            value = Array.get(value, 0);
        }
        if(value != null && !paramType.isInstance(value)) {
            value = converter.convert(value.toString(), paramType);
        }
        if(value == null) {
            // Set default value  see @DefaultValue
            for(Annotation annotation : annotations) {
                if(annotation instanceof DefaultValue) {
                    value = converter.convert(((DefaultValue)annotation).value(), paramType);
                }
            }
        }
        return value;
    }

    private Object convertParameter(final HttpExchange he, Annotation[] annotations, Class<?> paramType) {
        String key = null;
        for(Annotation annotation : annotations) {
            if(annotation instanceof HeaderParam) {
                key = ((HeaderParam)annotation).value();
                String value = he.getRequestHeaders().getFirst(key);
                if(value != null && !paramType.isInstance(value)) {
                    return converter.convert(value.toString(), paramType);
                }
                return value;
            } else if(annotation instanceof CookieParam) {
                key = ((CookieParam)annotation).value();
                String value = getCookie(he, key, true);
                return converter.convert(value.toString(), paramType);
            } else if (annotation instanceof Context) {
                LOGGER.warning("[REST] - Not support @Context parameter type.");
                /*
                if(paramType.isAssignableFrom(UriInfo.class)) {
                    //return new UriInfo();
                } else if(paramType.isAssignableFrom(HttpHeaders.class)) {
                    //return new HttpHeaders();
                }
                */
            }
        }
        return null;
    }

    /**
     * Search and retreive a cookie from a HTTP request context
     * @param key, The cookie name to search for
     * @param pReturnJustValue, return just the cookie value or the name + value i.e. "foo=bar;fie;etc";
     * @return
     */
    public String getCookie(HttpExchange he, String key, boolean justValue) {
        Iterator<Map.Entry<String, List<String>>> it =
                he.getRequestHeaders().entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            if(entry.getKey().toLowerCase().contentEquals("cookie")){
                String result = getCookieFromSearchString(key, entry.getValue().get(0));
                if(result != null) {
                    if (justValue) {
                        Matcher m = COOKIE_PATTERN.matcher(result);
                        if ((m.matches()) && (m.groupCount() == 2)) {
                            return m.group(2);
                        } else {
                            return result;
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    private String getCookieFromSearchString(String key, String wholeCookie) {
        if (wholeCookie.contains(";")) {
            String data[] = wholeCookie.split(";");
            for (int i = 0; i < data.length; i++) {
                if (data[i].trim().startsWith(key)) {
                    return data[i].trim();
                }
            }
        } else if (wholeCookie.startsWith(key)) {
            return wholeCookie;
        }
        return null;
    }


    /**
     * 通过url取得相应的RestService索引
     *
     * @param realUrl
     * @return 索引
     */
    protected Hitting<?> hit(final String realUrl, final String requestMethod,  final Map<String, Object> args) throws IOException {
        if (initing) {
            throw new ServiceUnavailableException();
        }
        // 折半查找RestService实体
        int low = 0;
        int high = servicePatterns.size();
        int index = (low + high) / 2;
        Hitting<?> hit = servicePatterns.get((low + high) / 2).hit(realUrl, requestMethod, args);
        int compareRet = hit.getHit();
        while (compareRet != 0 && ((high - low) != 1)) {
            if (compareRet > 0) {
                high = index;
            } else {
                low = index;
            }
            index = (low + high) / 2;
            hit = servicePatterns.get((low + high) / 2).hit(realUrl, requestMethod, args);
            compareRet = hit.getHit();
        }
        if(0 == compareRet) {
            return hit;
        } else {
            return null;
        }
    }

    public String info(boolean detail) {
        StringBuilder msg = new StringBuilder(getClass().getSimpleName());
        msg.append("@" + hashCode());
        msg.append(String.format(" - [%1$d]", servicePatterns.size()));
        if(detail) {
            if(!servicePatterns.isEmpty()) {
                msg.append("\r\n");
                for(RestServiceWrapper rest : servicePatterns) {
                    msg.append(rest.toString());
                    msg.append("\r\n");
                }
            }
            return msg.toString();
        } else {
            return msg.toString();
        }
    }

    /**
     * 检查是否有重复的url定义
     */
    private void checkDuplicateUrl() {
        RestServiceWrapper prePattern = null;
        final Iterator<RestServiceWrapper> iterator = servicePatterns.iterator();
        while (iterator.hasNext()) {
            final RestServiceWrapper pattern = iterator.next();
            if (pattern.compareTo(prePattern) <= 0) {
                throw new WebApplicationException(String.format("Duplicate url : '%s'", pattern.getPath()));
            }
            prePattern = pattern;
        }
    }

    @Override
    public String toString() {
        return info(false);
    }
}
