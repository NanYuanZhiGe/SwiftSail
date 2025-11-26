package com.nyzg.dock.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/hello_world")
    public String helloWorld(){
        return "hello world";
    }
}
