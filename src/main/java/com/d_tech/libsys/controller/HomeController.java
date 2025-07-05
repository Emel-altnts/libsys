package com.d_tech.libsys.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/") //localhost:9090/home/...

public class HomeController {
    // okumak içi GET yazmak için POST silmek için DELETE güncelleme PUT

    @GetMapping(path="/message")
    public String helloMethod(){
        return "Hello World";

    }
}
