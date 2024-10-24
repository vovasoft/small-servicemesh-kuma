package com.vova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author WangYang - vova
 * @version Create in 11:04 2024/10/22
 */


@RestController
public class HelloController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Java Service!";
    }

    @GetMapping("/call-golang")
    public String callGolang() {
        // 使用 Kuma 的服务发现来调用 Golang 服务
        String golangServiceUrl = "http://127.0.0.1:15001/hello";
        return restTemplate.getForObject(golangServiceUrl, String.class);
    }
}