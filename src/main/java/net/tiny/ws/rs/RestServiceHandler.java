package net.tiny.ws.rs;

import java.lang.reflect.Method;

public interface RestServiceHandler {

    public Object invoke(final Object[] args);
    public Object getTarget() throws Exception;
    public Method getMethod();
//	public Mode getMode();
//	public Class<?> getResponseType();
//	public String[] getRequestTypes();
//	public String[] getMediaTypes();
    public boolean acceptableMediaType(String type);

}
