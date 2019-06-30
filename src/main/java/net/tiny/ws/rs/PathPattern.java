package net.tiny.ws.rs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class PathPattern {
    static final String PREFIX = "{";
    static final String POSTFIX = "}";

    /**
     * 比较url是否符合pattern
     *
     * @param pattern
     * @param url
     * @return
     */
    public static int comparePatternAndUrl(final String pattern, final String url) {
        return compareWithSwitch(pattern, url, '{', '}', false);
    }


    /**
     * 比较url是否符合pattern
     *
     * @param pattern
     * @param url
     * @return
     */
    public static int comparePattern(final String pattern, final String url) {
        return compareWithSwitch(pattern, url, '{', '}', true);
    }

    /**
     * 比较两个pattern或一个pattern和url，isComparePattern表示是否是pattern比较
     *
     * @param pattern
     * @param url
     * @param varPrefix
     * @param varPostfix
     * @param isComparePattern
     * @return
     */
    private static int compareWithSwitch(final String pattern,
            final String url, final char varPrefix, final char varPostfix,
            final boolean isComparePattern) {
        if (pattern == null || pattern.length() == 0) {
            if (url == null || url.length() == 0) {
                return 0;
            }
            return -1;
        }
        if (url == null || url.length() == 0) {
            return 1;
        }

        final String[] firstSegs = pattern.split("/");
        final String[] secSegs = url.split("/");
        if (firstSegs.length > secSegs.length) {
            return 1;
        }
        if (firstSegs.length < secSegs.length) {
            return -1;
        }
        if (isComparePattern) {
            validatePattern(pattern, url, varPrefix, varPostfix);
        }
        for (int i = 0; i < secSegs.length; i++) {
            if ((firstSegs[i] + secSegs[i]).contains(varPrefix + "")) {
                continue;
            }
            final int compareRet = compareString(firstSegs[i], secSegs[i], true);
            if (compareRet != 0) {
                return compareRet;
            }
        }
        return 0;
    }

    /**
     * 判断两个pattern是否有冲突即同一层级即定义变量又定义常量，如下：<br/>
     * /u/111111 与 /u/{id}
     *
     * @param pattern1
     * @param pattern2
     * @param varPrefix
     * @param varPostfix
     */
    public static void validatePattern(final String pattern1,
            final String pattern2, final char varPrefix, final char varPostfix) {
        final String[] firstSegs = pattern1.split("/");
        final String[] secSegs = pattern2.split("/");
        /*
         * if (secSegs.length != firstSegs.length) { return; }
         */
        final int count = firstSegs.length < secSegs.length ? firstSegs.length: secSegs.length;
        for (int i = 0; i < count; i++) {
            if (!(firstSegs[i] + secSegs[i]).contains(varPrefix + "")
                    && !firstSegs[i].equalsIgnoreCase(secSegs[i])) {
                break;
            }
            if ((firstSegs[i] + secSegs[i]).contains(varPrefix + "")) {
                if (!firstSegs[i].contains(varPrefix + "")
                        || !secSegs[i].contains(varPrefix + "")) {

                        throw new IllegalArgumentException(pattern1 + " and "
                                + pattern2 + "  conflict.");
                }
            }
        }
    }

    /**
     * 比较两个字符串大小 <br/>
     * 规则:<br/>
     * a 小于 b <br/>
     * ab大于b <br/>
     * ab 小于 ac <br/>
     * a 等于 a<br/>
     *
     * @param str1
     * @param str2
     * @param ignoreCase
     * @return
     */
    private static int compareString(final String str1, final String str2,
            final boolean ignoreCase) {
        final char[] firstChrs = str1.toCharArray();
        final char[] secChrs = str2.toCharArray();
        if (firstChrs.length > secChrs.length) {
            return 1;
        }
        if (firstChrs.length < secChrs.length) {
            return -1;
        }
        for (int i = 0; i < secChrs.length; i++) {
            if (firstChrs[i] > secChrs[i]) {
                return 1;
            }
            if (firstChrs[i] < secChrs[i]) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * pattern 的合法性检查:不允许连续出现{{或者}}的情况,不允许//之间为空
     *
     * @param pattern
     * @param varPrefix
     * @param varPostfix
     * @throws IllegalArgumentException
     * @return 变量个数
     */
    public static int checkPattern(final String pattern) {
        final char varPrefix = '{';
        final char varPostfix = '}';
        if (pattern == null || pattern.length() == 0) {
            return 0;
        }
        int ret = 0;
        final char[] urlArr = pattern.toCharArray();
        boolean inVar = false;
        int lastSlashPos = -2;
        for (int i = 0; i < pattern.length(); i++) {
            if (urlArr[i] == '/') {
                if (i - lastSlashPos == 1) {
                    throw new IllegalArgumentException(pattern);
                }
                lastSlashPos = i;
            } else if (urlArr[i] == ' ') {
                if (i - lastSlashPos == 1) {
                    lastSlashPos++;
                }
            } else if (urlArr[i] == varPrefix) {
                ret++;
                if (inVar || (i > 0 && (urlArr[i - 1] != '/' &&urlArr[i - 1] != ';'  && urlArr[i - 1] != '?'  && urlArr[i - 1] != '&' ))
                        || i == urlArr.length - 1
                        || urlArr[i + 1] == varPostfix) {
                    throw new IllegalArgumentException(pattern);
                }
                inVar = true;
            } else if (urlArr[i] == varPostfix) {
                if (!inVar || (i < urlArr.length - 1 && !(urlArr[i - 1] != '/' &&urlArr[i - 1] != ';'  && urlArr[i - 1] != '?' ))) {
                    throw new IllegalArgumentException(pattern);
                }
                inVar = false;
            }
        }
        return ret;
    }

    /**
     * pattern为空时由类的一个方法自动生成匹配符
     *  例：“/methodName/{id : \\d+}”
     * @param method
     * @return 匹配符文字
     */
    public static String generatorPattern(final Method  method) {
        StringBuilder sb = new StringBuilder("/");
        sb.append(method.getName());
        Annotation[][] ass  = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();
        for(int i=0; i<paramTypes.length; i++) {
            Class<?> type = paramTypes[i];
            Annotation[] as = ass[i];
            for(int n=0; n<as.length; n++) {
                Annotation a = as[n];
                if(a instanceof PathParam) {
                    PathParam pp = (PathParam)a;
                    sb.append("/{");
                    sb.append(pp.value());
                    if(isNumeric(type)) {
                        sb.append(" : ");
                        sb.append("\\d+");
                    }
                    sb.append("}");
                }
                if(a instanceof QueryParam) {
                    QueryParam pp = (QueryParam)a;
                    sb.append("?{");
                    sb.append(pp.value());
                    if(isNumeric(type)) {
                        sb.append("=");
                        sb.append("\\d+");
                    }
                    sb.append("}");
                }
            }
            type.getName();
        }
        return sb.toString();
    }

    static boolean isNumeric(Class<?> type) {
        return type != null &&
                (type.equals(int.class) ||
                type.equals(short.class) ||
                type.equals(long.class) ||
                type.equals(float.class) ||
                type.equals(double.class) ||
                type.equals(Integer.class) ||
                type.equals(Short.class) ||
                type.equals(Long.class) ||
                type.equals(Float.class) ||
                type.equals(Double.class) ||
                type.equals(BigDecimal.class) ||
                type.equals(BigInteger.class) );
    }
}
