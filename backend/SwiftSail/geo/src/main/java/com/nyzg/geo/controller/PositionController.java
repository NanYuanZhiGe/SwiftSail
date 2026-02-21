package com.nyzg.geo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PositionController {
    @GetMapping(path = "/test/connection")
    public String testConnection(){
        return Thread.currentThread().getName();
    }
}
