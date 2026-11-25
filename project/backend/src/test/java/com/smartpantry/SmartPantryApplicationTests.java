package com.smartpantry;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SmartPantryApplicationTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String dbFile = tempDir.toAbsolutePath().toString().replace('\\', '/') + "/smartpantry";
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + dbFile);
    }

    // The context loading is the test: it fails if the main class, the
    // dependencies or the H2 file datasource configuration are broken.
    @org.junit.jupiter.api.Test
    void contextLoads() {
    }
}
