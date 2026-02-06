package com.ezzyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EzzyhubBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EzzyhubBackendApplication.class, args);

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");
        System.out.println("║      🚀 EzzyHub Backend Running! 🚀   ║");
        System.out.println("║                                        ║");
        System.out.println("║   Access at: http://localhost:8080     ║");
        System.out.println("║   Health: http://localhost:8080/api/health ║");
        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }
}