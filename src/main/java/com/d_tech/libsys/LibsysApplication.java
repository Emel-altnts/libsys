package com.d_tech.libsys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

/**
 * LibSys Main Application - Railway Optimized
 * Library Management System with optional Kafka integration
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.d_tech.libsys")
@EnableScheduling
public class LibsysApplication {

	@Value("${server.port:8080}")
	private String port;

	@Value("${app.kafka.enabled:false}")
	private boolean kafkaEnabled;

	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	public static void main(String[] args) {
		SpringApplication.run(LibsysApplication.class, args);
	}

	@PostConstruct
	public void displayStartupInfo() {
		System.out.println("=================================================================");
		System.out.println("🚀 LibSys Library Management System Started!");
		System.out.println("=================================================================");
		System.out.println("📊 Port: " + port);
		System.out.println("🌐 Profile: " + activeProfile);
		System.out.println("📡 Kafka Enabled: " + (kafkaEnabled ? "✅ YES" : "❌ NO"));
		System.out.println("🔗 Base URL: http://localhost:" + port);
		System.out.println("🏥 Health Check: http://localhost:" + port + "/actuator/health");
		System.out.println("📖 API Endpoints:");
		System.out.println("   🔐 Login: POST /api/auth/login");
		System.out.println("   📝 Register: POST /api/auth/signup" + (kafkaEnabled ? "-async" : ""));
		System.out.println("   📚 Books: GET /api/books");
		System.out.println("   👥 Users: GET /api/users (Admin only)");
		if (kafkaEnabled) {
			System.out.println("   📦 Async Stock: POST /api/stock/check/{bookId}");
			System.out.println("   🧾 Async Orders: POST /api/stock/orders");
		}
		System.out.println("=================================================================");

		if ("prod".equals(activeProfile)) {
			System.out.println("🔥 PRODUCTION MODE - Railway deployment");
			System.out.println("💾 Database: PostgreSQL (Railway)");
			System.out.println("📡 Kafka: " + (kafkaEnabled ? "CloudKarafka" : "Disabled"));
		} else {
			System.out.println("🔧 DEVELOPMENT MODE");
			System.out.println("⚠️  Make sure PostgreSQL is running!");
			if (kafkaEnabled) {
				System.out.println("⚠️  Make sure Kafka is running!");
			}
		}
		System.out.println("=================================================================");
	}

	/**
	 * Application Architecture:
	 *
	 * SYNC FLOW (Always Available):
	 * Controller → Service → Repository → Database
	 *
	 * ASYNC FLOW (When Kafka Enabled):
	 * Controller → AsyncService → KafkaProducer → Kafka Topic
	 *                                                ↓
	 * Database ← Service ← Consumer ← Kafka Consumer
	 *
	 * LAYERS:
	 * - Controller: HTTP request handling
	 * - Service: Business logic (sync/async)
	 * - Repository: Database access
	 * - Model/Entity: Database entities
	 * - DTO: Data transfer objects
	 * - Config: Configuration classes
	 * - Security: Authentication & authorization
	 *
	 * RAILWAY FEATURES:
	 * - Environment-based configuration
	 * - Optional Kafka integration
	 * - PostgreSQL connection with Railway DATABASE_URL
	 * - Health checks for monitoring
	 * - Memory-optimized settings
	 */
}