package com.terry.research.snowflake;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SnowflakeIdGeneratorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SnowflakeIdGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
    }

    /**
     * Print.
     */
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private static void print(Object s) {
        System.out.printf("%s%s%s", GREEN, s, RESET);
    }

    private static void println(Object s) {
        System.out.printf("%s%s%s\n", GREEN, s, RESET);
    }

    private static void err(Object s) {
        System.out.printf("%s%s%s", RED, s, RESET);
    }

    private static void errln(Object s) {
        System.out.printf("%s%s%s\n", RED, s, RESET);
    }

}
