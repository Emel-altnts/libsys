package com.d_tech.libsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LibSys Ana Uygulama Sınıfı
 * Kütüphane Yönetim Sistemi - Asenkron Kafka Entegrasyonlu
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.d_tech.libsys")
@EnableScheduling // Scheduled task'ları aktif et (EventTrackingService cleanup için)
public class LibsysApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibsysApplication.class, args);

		System.out.println("=================================================================");
		System.out.println("🚀 LibSys Kütüphane Yönetim Sistemi Başlatıldı!");
		System.out.println("=================================================================");
		System.out.println("📊 Port: 9090");
		System.out.println("🔗 Base URL: http://localhost:9090");
		System.out.println("📖 API Endpoints:");
		System.out.println("   📝 Asenkron Kayıt: POST /api/auth/signup-async");
		System.out.println("   🔍 Kayıt Durumu: GET /api/auth/registration-status/{eventId}");
		System.out.println("   🔐 Giriş: POST /api/auth/login");
		System.out.println("   📚 Kitaplar: GET /api/books");
		System.out.println("=================================================================");
		System.out.println("⚠️  Kafka ve PostgreSQL'in çalıştığından emin olun!");
		System.out.println("=================================================================");
	}

	// Controller → Service → Repository → Database
	// Yeni Kafka Mimarisi:
	// Controller → AsyncUserService → KafkaProducer → Kafka Topic
	//                                                      ↓
	// Database ← UserService ← UserRegistrationConsumer ← Kafka Consumer

	// Katmanlar:
	// Controller: HTTP isteklerini karşılar
	// AsyncUserService: Asenkron işlemleri yönetir
	// KafkaProducerService: Kafka'ya mesaj gönderir
	// UserRegistrationConsumer: Kafka'dan mesaj alır ve işler
	// EventTrackingService: Event durumlarını takip eder
	// UserService: Senkron kullanıcı işlemleri (backward compatibility)
	// Repository: Veritabanı erişimi sağlar
	// Model/Entity: Veritabanı tablolarını temsil eder
}