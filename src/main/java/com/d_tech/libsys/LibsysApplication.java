package com.d_tech.libsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = "com.d_tech.libsys")
public class LibsysApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibsysApplication.class, args);
	}
// Controller → Service → Repository → Database
//Katmanlar:
//Controller: HTTP isteklerini karşılar

//Service : business logic barındırır
//Repository : Veritabanı erişimi sağlar
//Model entity : Veritabanı tablolarını temsil eder
}
