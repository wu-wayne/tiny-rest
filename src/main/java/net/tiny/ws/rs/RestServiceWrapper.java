package net.tiny.ws.rs;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

public class RestServiceWrapper implements Comparable<RestServiceWrapper>, Constants {

    static Logger LOGGER = Logger.getLogger(RestServiceWrapper.class.getName());

    private String parentPath;
    private Object service;
    private Class<?> serviceClass;
    private String mediaType = null;
    private List<MethodPattern> methodPatterns = Collections.synchronizedList(new ArrayList<MethodPattern>());
    /** 模式:单例模式/还是每次请求生成新对象 默认是instance模式 */
    private Mode mode = Mode.instance;

    public RestServiceWrapper(Object target) {
        setService(target);
    }

    public void setService(final Object target) {
        this.methodPatterns.clear();
        this.serviceClass = target.getClass();
        Annotation annotation = serviceClass.getAnnotation(Path.class);
        if(null == annotation) {
            throw new IllegalArgumentException("The class '" + serviceClass.getName() + "' must be with @Path.");
        }
        this.parentPath  = ((Path)annotation).value();
        // Format path string
        if (this.parentPath != null && !this.parentPath.startsWith("/")) {
            this.parentPath = "/"  + this.parentPath;
        }

//        annotation = serviceClass.getAnnotation(Singleton.class);
//        if(null != annotation) {
//            this.mode = Mode.singleton;
//        } else {
//            this.mode = Mode.instance;
//            this.service = target;
//        }
        this.service = target;

        Produces  produces  = this.serviceClass .getAnnotation(Produces.class);
        if(null != produces) {
            this.mediaType = produces.value()[0];
        } else {
            this.mediaType = MediaType.TEXT_HTML;
        }

        Method[] methods = serviceClass.getDeclaredMethods();
        for(Method method : methods) {
            MethodPattern methodPattern = parseMethod(method);
            if(null != methodPattern) {
                methodPatterns.add(methodPattern);
            }
        }

        if(methodPatterns.isEmpty()) {
            for(Method method : methods) {
                LOGGER.warning("Can't register a rest method '" + method.toGenericString() +"'");
            }
            //类里没有被登录的REST方法
            throw new WebApplicationException("'" + serviceClass.getName() +"' one method has not even to register on '" + this.parentPath+"'" );
        }
        // 对配置项进行排序，方便后面的查找
        Collections.sort(methodPatterns);
    }

    protected MethodPattern parseMethod(Method method) {
        if(!method.toGenericString().startsWith("public")) {
            // Is not public method
            return null;
        }
        String httpMethod = null;
        String pattern = null;
        Annotation[] as = method.getDeclaredAnnotations();
        for(Annotation a : as) {
            //Find HTTP Method
            if(a instanceof GET) {
                httpMethod = "GET";
            } else if(a instanceof POST) {
                httpMethod = "POST";
            } else if(a instanceof PUT) {
                httpMethod = "PUT";
            }
            if(a instanceof Path) {
                pattern = ((Path)a).value();
            }
        }
        return new  MethodPattern(this.parentPath, pattern, httpMethod, this.mediaType, this.serviceClass, this.service, method);
    }

    public String getPath() {
        return this.parentPath;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * 取得实体生成模式
     *
     * @return
     */
    public Mode getMode() {
        return mode;
    }
    /**
     * 判断url是否命中pattern
     *
     * @param realUrl
     * @param requestMethod
     * @return
     * @throws UnsupportedEncodingException
     */
    public Hitting<?> hit(final String realUrl,  final String requestMethod) throws UnsupportedEncodingException {
        return hit(realUrl, requestMethod, null);
    }

    /**
     * 判断url是否命中pattern
     *
     * @param realUrl
     * @param requestMethod
     * @param args
     * @return
     * @throws UnsupportedEncodingException
     */
    public Hitting<?> hit(final String realUrl,  final String requestMethod,  final Map<String, Object> args) throws UnsupportedEncodingException {
        if (realUrl == null || realUrl.length() == 0) {
            return Hitting.NOT_HIT;
        }
        String url = realUrl;
        if(!url.startsWith(this.parentPath)) {
            //前缀路径不同时 前缀路径比较
            return new Hitting<Void>(this.parentPath.compareTo(url));
        }

        int compareRet = -1;
        for(MethodPattern methodPattern : methodPatterns) {
            compareRet = methodPattern.hit(url, requestMethod, args);
            if (compareRet == 0) {
                return methodPattern.getHitting();
            }
        }
        return new Hitting<Void>(compareRet);
    }

    @Override
    public int compareTo(final RestServiceWrapper target) {
        if (target == null) {
            return 1;
        }
        // 比较URL路径
        return getPath().compareTo(target.getPath());
    }

    @Override
    public String toString() {
        StringBuilder sb =  new StringBuilder();
        sb.append(" Path:'");
        sb.append(this.parentPath);
        sb.append("' ");
        sb.append(serviceClass.getName());
        sb.append(" ");
        sb.append(this.mode);
        sb.append("  Methods: {");
        for(MethodPattern methodPattern : methodPatterns) {
            sb.append(" [");
            sb.append(methodPattern.getMethod().getName());
            sb.append("=");
            sb.append(methodPattern.getPattern());
            sb.append("]");
        }
        sb.append(" }");
        return sb.toString();
    }
}
