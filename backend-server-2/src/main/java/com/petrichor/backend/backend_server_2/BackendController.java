package com.petrichor.backend.backend_server_2;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")

public class BackendController {
    @GetMapping("/test")
    public String handleRequest(){
        return "reponse from backend server" + System.getenv("SERVER_NAME");
    }
}

