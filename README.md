### **步骤 1: 环境准备**

#### 1.1 **安装 Docker**

Kuma 依赖于 Docker 来进行快速的服务部署。确保你已经在 M1 Mac 上安装了 Docker。

- 你可以从 [Docker 官方网站](https://www.docker.com/products/docker-desktop) 下载并安装 Docker Desktop。

#### 1.2 **安装 Kuma**

Kuma 提供了两种模式：Standalone（独立模式）和 Kubernetes 模式。对于本地测试，我们使用 **Standalone 模式**。

1. 下载并安装 Kuma 的二进制文件：

   ```bash
   curl -L https://kuma.io/installer.sh | sh
   ```

2. 添加 Kuma 到系统路径（地址需要换成自己的）：

   ```bash
   export PATH=$PATH:/Users/Vova/Github/small-servicemesh-kuma/kuma-2.8.4/bin
   ```

3. 启动 Kuma 控制平面（控制面板）：

   ```bash
   kuma-cp run
   ```

   这将启动 Kuma 控制平面，默认情况下运行在 `http://localhost:5681`。

4. 验证 Kuma 是否成功启动：

   - 访问 Kuma 的管理界面 [Kuma GUI](http://localhost:5681/gui) 以查看控制面板状态。

### **步骤 2: 创建 Java 服务**

我们将创建一个简单的 Java 服务，通过 Kuma 的数据平面代理注册，并允许其他服务通过它访问。

#### 2.1 **项目结构**

你可以使用 Spring Boot 创建一个简单的 RESTful 服务。使用 Spring Initializr 创建项目或手动配置：

1. 在 [Spring Initializr](https://start.spring.io/) 中选择 `Maven Project`，语言选择 `Java`。
2. 添加依赖项：`Spring Web` 和 `Spring Boot Actuator`。

#### 2.2 **Java 服务的代码**

创建一个简单的 REST 服务，它将被注册到 Kuma，并通过 `/hello` 端点提供服务。

```java
package com.example.kumaservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Java Service!";
    }
}
```

#### 2.3 **pom.xml 文件**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>kuma-java-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Kuma Java Demo</name>
    <description>Demo project for Spring Boot with Kuma</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.8</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2.4 **application.yml 文件**

```yaml
server:
  port: 8080  # Java 服务运行的端口
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 暴露健康检查端点
logging:
  level:
    root: INFO
```

#### 2.5 **运行 Java 服务（第一个服务器）**

1. 构建并运行 Java 服务：

   ```bash
   mvn spring-boot:run
   ```

2. 生成token：

   ```shell
   export PATH=$PATH:/Users/Vova/Github/small-servicemesh-kuma/kuma-2.9.0/bin
   
   kumactl generate dataplane-token --name java-service --valid-for=24h > java-service-token
   ```

   

3. 启动 Kuma 数据平面代理来代理 Java 服务：

   ```bash
   
   lsof -i :9903
   
   
   export PATH=$PATH:/Users/Vova/Github/small-servicemesh-kuma/kuma-2.9.0/bin
   export KUMA_READINESS_PORT=9901 \
   export KUMA_APPLICATION_PROBE_PROXY_PORT=9902 \
         
   kuma-dp run \
    --cp-address=https://127.0.0.1:5678/ \
    --dns-enabled=false \
    --dataplane-token-file=java-service-token \
    --dataplane="
    type: Dataplane
    mesh: default
    name: java-service
    networking: 
      address: 127.0.0.1
      inbound: 
        - port: 15000
          servicePort: 8080
          serviceAddress: 127.0.0.1
          tags: 
            kuma.io/service: java-service
            kuma.io/protocol: http
      outbound: 
        - port: 8082  
          tags:
            kuma.io/service: golang-service
      admin:
         port: 9903"
   ```

### **步骤 3: 创建 Golang 服务**

同样，我们将在 Golang 中实现另一个服务，并通过 Kuma 注册该服务。

#### 3.1 **编写 Golang 服务**

创建一个名为 `main.go` 的文件，编写简单的 Golang HTTP 服务：

```go
package main

import (
	"fmt"
	"log"
	"net/http"
)

func helloHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Hello from Golang Service!")
}

func main() {
	http.HandleFunc("/hello", helloHandler)

	log.Println("Starting Golang service on port 9091...")
	err := http.ListenAndServe(":9091", nil)
	if err != nil {
		log.Fatalf("Failed to start Golang service: %v", err)
	}
}
```

#### 3.2 **运行 Golang 服务（第二个服务器）**

1. 启动 Golang 服务：

   ```bash
   go run main.go
   ```

2. 生成token：

   ```shell
   kumactl generate dataplane-token --name golang-service --valid-for=24h > golang-service-token
   ```

   

3. 启动 Kuma 数据平面代理为 Golang 服务代理：

   ```bash
   export PATH=$PATH:/Users/Vova/Github/small-servicemesh-kuma/kuma-2.9.0/bin    
   export KUMA_READINESS_PORT=9904 \
   export KUMA_APPLICATION_PROBE_PROXY_PORT=9905 \
   
               
   kuma-dp run \
    --cp-address=https://127.0.0.1:5678/ \
    --dns-enabled=false \
    --dataplane-token-file=golang-service-token \
    --dataplane="
    type: Dataplane
    mesh: default
    name: golang-service
    networking: 
      address: 127.0.0.1
      inbound: 
        - port: 15001
          servicePort: 9091
          serviceAddress: 127.0.0.1
          tags: 
            kuma.io/service: golang-service
            kuma.io/protocol: http
      outbound: 
        - port: 8083
          tags:
            kuma.io/service: java-service
      admin:
         port: 9906"
   ```

### **步骤 4: 测试服务的相互调用

#### 4.1 **让 Java 服务调用 Golang 服务**

在 Java 服务的 `HelloController` 中，增加一个调用 Golang 服务的逻辑：

```java
@Autowired
private RestTemplate restTemplate;

@GetMapping("/call-golang")
public String callGolang() {
    // 使用 Kuma 的服务发现来调用 Golang 服务
    String golangServiceUrl = "http://127.0.0.1:15001/hello";
    return restTemplate.getForObject(golangServiceUrl, String.class);
}
```

#### 4.2 **让 Golang 服务调用 Java 服务**

在 Golang 服务中，修改 `helloHandler` 函数以调用 Java 服务：

```go
func helloHandler(w http.ResponseWriter, r *http.Request) {
    javaServiceUrl := "http://localhost:8080/hello"
    resp, err := http.Get(javaServiceUrl)
    if err != nil {
        log.Printf("Failed to call Java service: %v", err)
        http.Error(w, "Error calling Java service", http.StatusInternalServerError)
        return
    }
    defer resp.Body.Close()

    body, _ := ioutil.ReadAll(resp.Body)
    fmt.Fprintf(w, "Response from Java Service: %s", body)
}
```

### **步骤 5: 验证 Kuma Service Mesh**

1. 启动所有服务，并确保它们通过 Kuma 数据平面代理进行通信。

2. 使用浏览器或 `curl` 测试：

   - 调用 Java 服务并触发对 Golang 服务的调用：

     ```bash
     curl http://localhost:8080/call-golang
     ```

   - 调用 Golang 服务并触发对 Java 服务的调用：

     ```bash
     curl http://localhost:9091/hello
     ```

3. 通过 Kuma 控制台查看服务的健康状况和流量情况，确保服务间通信通过 Kuma 代理进行。

4. 
