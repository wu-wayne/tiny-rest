package net.tiny.ws.rs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class ReflectBenchmarkTest {
    @Test
    public void testReflect() throws Throwable {
        final int exeCount = 10000000;
        final Class<?> c = TestService.class;
        final Object j = c.newInstance();
        Method m = c.getMethod("setId", String.class);
        System.out.println("测试开始，循环次数：" + exeCount / 10000 + "万");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.println("直接调用Method："); //30ms
        long currTime = System.currentTimeMillis();
        for (int i = 0; i < exeCount; i++) {
            m.invoke(j, "test");
        }
        System.out.println("执行结束，耗时" + (System.currentTimeMillis() - currTime)
                + "豪秒");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.println("每次构造Method再执行："); //3000ms
        currTime = System.currentTimeMillis();
        for (int i = 0; i < exeCount; i++) {
            m = c.getMethod("setId", String.class);
            m.invoke(j, "test");
        }
        System.out.println("执行结束，耗时" + (System.currentTimeMillis() - currTime)
                + "豪秒");
        System.out.println("----------------------------------------------------------------------------------");
        final TestService service = new TestService();
        System.out.println("直接调用：");  //20ms
        currTime = System.currentTimeMillis();
        for (int i = 0; i < exeCount; i++) {
            service.setId("test");
        }
        System.out.println("执行结束，耗时" + (System.currentTimeMillis() - currTime)
                + "豪秒");

        System.out.println("----------------------------------------------------------------------------------");
        System.out.println("方法句柄调用：");//100ms
        currTime = System.currentTimeMillis();
        MethodHandles.Lookup lookkup=MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class, String.class);
        MethodHandle methodHandle=lookkup.findVirtual(TestService.class, "setId", mt);
        assertNotNull(methodHandle);
        for (int i = 0; i < exeCount; i++) {
            methodHandle.invokeExact(service, "test");
        }
        System.out.println("执行结束，耗时" + (System.currentTimeMillis() - currTime)
                + "豪秒");

    }
}
