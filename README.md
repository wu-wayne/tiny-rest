## Tiny Rest: 一个基于Restful协议的微服框架
## 设计目的
 - 使用Tiny Boot包进行服务器配置。
 - 基于Tiny Service的Java类实装Rest服务。
 - 支持javax.ws.rs基类的Rest实装
 - 提供精简的Rest客户端Java类。

##Usage

###1. Simple Run
```java
java net.tiny.boot.Main --verbose
```


###2. Application configuration file with profile
```properties
Configuration file : application-{profile}.[yml, json, conf, properties]

main = ${launcher}
daemon = true
executor = ${pool}
callback = ${services}
pool.class = net.tiny.service.PausableThreadPoolExecutor
pool.size = 2
pool.max = 10
pool.timeout = 1
services.class = net.tiny.service.ServiceLocator
launcher.class = net.tiny.ws.Launcher
launcher.builder.bind = 192.168.1.1
launcher.builder.port = 80
launcher.builder.backlog = 10
launcher.builder.stopTimeout = 1
launcher.builder.executor = ${pool}
launcher.builder.handlers = ${rest}, ${health}
rest.class = net.tiny.ws.rs.RestfulHttpHandler
rest.path = /v1/api
rest.filters = ${logger}, ${params}
rest.factory.class = net.tiny.ws.rs.RestServiceFactory
rest.factory.application = ${rest.application}
rest.application.class = net.tiny.ws.rs.RestApplication
rest.application.pattern = "your.rest.*, !java.*, !com.sun.*"
health.class = net.tiny.ws.VoidHttpHandler
health.path = /health
health.filters = ${logger}
logger.class = net.tiny.ws.AccessLogger
logger.format = COMBINED
logger.file = /var/log/http-access.log
params.class = net.tiny.ws.ParameterFilter
```


###3. Sample Rest service java
```java
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/api/test")
public class TestService {
    @Resource
    private DataSource dataSource;

    private String id;

    @GET
    @Path("{id}")
    @Produces(value = MediaType.APPLICATION_JSON)
    public String getId(@PathParam("id")String id) {
        return "Hello! Id is " + id;
    }
}
```

###4. Sample Rest client java
```java
import net.tiny.ws.rs.client.RestClient;

RestClient client = new RestClient.Builder()
        .build();

String response = client.execute("http://localhost:8080/v1/api/test/1234")
            .get(MediaType.APPLICATION_JSON)
            .getEntity();

client.close();
```

##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
