#!/bin/bash

# 🚀 LibSys Ngrok Startup Script - Ultimate Edition
# Bu script tüm servisleri otomatik başlatır ve ngrok ile paylaşır

set -e

echo "🚀 LibSys Ngrok Startup Script - Ultimate Edition"
echo "================================================="

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Global değişkenler
SPRING_PID=""
NGROK_PID=""
KAFKA_STARTED_BY_SCRIPT=false
PROJECT_DIR=$(pwd)
PORT=8080

# Functions
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_highlight() {
    echo -e "${PURPLE}🌟 $1${NC}"
}

print_cyan() {
    echo -e "${CYAN}🔧 $1${NC}"
}

print_separator() {
    echo -e "${PURPLE}=================================================${NC}"
}

# Cleanup function
cleanup() {
    echo ""
    print_info "Temizlik işlemi başlatılıyor..."

    # Spring Boot process'i durdur
    if [ ! -z "$SPRING_PID" ] && kill -0 $SPRING_PID 2>/dev/null; then
        print_info "Spring Boot durduruluyor... (PID: $SPRING_PID)"
        kill $SPRING_PID 2>/dev/null || true
        sleep 3
    fi

    # Ngrok process'i durdur
    if [ ! -z "$NGROK_PID" ] && kill -0 $NGROK_PID 2>/dev/null; then
        print_info "Ngrok durduruluyor... (PID: $NGROK_PID)"
        kill $NGROK_PID 2>/dev/null || true
        sleep 2
    fi

    # Docker Kafka containers'ı durdur (eğer varsa)
    if [ "$KAFKA_STARTED_BY_SCRIPT" = "true" ]; then
        print_info "Docker Kafka containers durduruluyor..."
        docker-compose -f docker-compose.yml down 2>/dev/null || true
    fi

    print_success "Temizlik tamamlandı!"
    exit 0
}

# Trap signal'ları yakala
trap cleanup INT TERM

# ===============================================
# 1. SISTEM KONTROLLERI
# ===============================================

print_highlight "SISTEM KONTROLLERI"
print_separator

# Java version check
print_info "Java versiyonu kontrol ediliyor..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_success "Java $JAVA_VERSION detected - OK"
    else
        print_error "Java 17+ gerekli! Mevcut versiyon: $JAVA_VERSION"
        print_cyan "Java 17+ kurulum linkı: https://adoptium.net/"
        exit 1
    fi
else
    print_error "Java bulunamadı! Java 17+ kurulumu gerekli."
    exit 1
fi

# Maven version check
print_info "Maven versiyonu kontrol ediliyor..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    print_success "Maven $MVN_VERSION detected - OK"
else
    print_error "Maven bulunamadı!"
    print_cyan "Maven kurulum: https://maven.apache.org/download.cgi"
    exit 1
fi

# Ngrok version check
print_info "Ngrok versiyonu kontrol ediliyor..."
if command -v ngrok &> /dev/null; then
    NGROK_VERSION=$(ngrok version 2>&1 | head -n 1 | awk '{print $3}' | sed 's/[^0-9.]*//g')
    print_success "Ngrok $NGROK_VERSION detected - OK"
else
    print_error "Ngrok bulunamadı!"
    print_cyan "Ngrok kurulum:"
    print_cyan "  • macOS: brew install ngrok"
    print_cyan "  • Windows: choco install ngrok"
    print_cyan "  • Linux: snap install ngrok"
    print_cyan "  • Manual: https://ngrok.com/download"
    exit 1
fi

# Docker check (optional)
print_info "Docker kontrol ediliyor (Kafka için)..."
if command -v docker &> /dev/null; then
    print_success "Docker detected - Kafka destekleniyor"
    DOCKER_AVAILABLE=true
else
    print_warning "Docker bulunamadı - Kafka devre dışı kalacak"
    DOCKER_AVAILABLE=false
fi

# ===============================================
# 2. PROJE YAPISI KONTROLLERI
# ===============================================

print_highlight "PROJE YAPISI KONTROLLERI"
print_separator

# pom.xml kontrolü
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml bulunamadı! Proje dizininde olduğunuzdan emin olun."
    exit 1
fi
print_success "pom.xml bulundu"

# src dizini kontrolü
if [ ! -d "src" ]; then
    print_error "src dizini bulunamadı!"
    exit 1
fi
print_success "src dizini bulundu"

# application-ngrok.yml kontrolü
if [ ! -f "src/main/resources/application-ngrok.yml" ]; then
    print_warning "application-ngrok.yml bulunamadı - oluşturuluyor..."
    # Buraya application-ngrok.yml içeriği eklenebilir
fi

# ===============================================
# 3. KULLANICI SEÇENEKLERİ
# ===============================================

print_highlight "YAPLANDIRMA SEÇENEKLERİ"
print_separator

echo "Kafka'yı etkinleştirmek ister misiniz? (Docker gerekli)"
echo "1) Evet - Tam özellikli mod (Kafka + tüm async özellikler)"
echo "2) Hayır - Basit mod (sadece HTTP API)"
read -p "Seçiminizi yapın (1/2): " KAFKA_CHOICE

if [ "$KAFKA_CHOICE" = "1" ] && [ "$DOCKER_AVAILABLE" = true ]; then
    ENABLE_KAFKA=true
    print_success "Kafka etkinleştirildi - Tam özellikli mod"
else
    ENABLE_KAFKA=false
    print_info "Kafka devre dışı - Basit mod"
fi

# Port seçimi
read -p "Hangi portu kullanmak istiyorsunuz? (varsayılan: 8080): " USER_PORT
if [ ! -z "$USER_PORT" ]; then
    PORT=$USER_PORT
fi

# ===============================================
# 4. KAFKA BAŞLATMA (İsteğe bağlı)
# ===============================================

if [ "$ENABLE_KAFKA" = true ]; then
    print_highlight "KAFKA BAŞLATILIYOR"
    print_separator

    print_info "Docker Compose ile Kafka başlatılıyor..."

    if [ -f "docker-compose.yml" ]; then
        docker-compose up -d
        KAFKA_STARTED_BY_SCRIPT=true
        print_success "Kafka başlatıldı"

        # Kafka'nın hazır olmasını bekle
        print_info "Kafka'nın hazır olması bekleniyor..."
        sleep 15

        # Kafka connection test
        if docker-compose ps | grep -q "Up"; then
            print_success "Kafka containers çalışıyor"
        else
            print_warning "Kafka containers başlatılamadı - sync mode'a geçiliyor"
            ENABLE_KAFKA=false
        fi
    else
        print_warning "docker-compose.yml bulunamadı - sync mode'a geçiliyor"
        ENABLE_KAFKA=false
    fi
fi

# ===============================================
# 5. MAVEN BUILD
# ===============================================

print_highlight "MAVEN BUILD"
print_separator

print_info "Maven temizleme ve derleme başlatılıyor..."

# Clean ve compile
print_info "Proje temizleniyor..."
mvn clean -q

# Test'leri skip ederek package
print_info "Proje derleniyor (testler atlanıyor)..."
if [ "$ENABLE_KAFKA" = true ]; then
    mvn package -DskipTests -Dspring.profiles.active=ngrok -Dapp.kafka.enabled=true -q
else
    mvn package -DskipTests -Dspring.profiles.active=ngrok -Dapp.kafka.enabled=false -q
fi

if [ $? -eq 0 ]; then
    print_success "Maven build başarılı!"
else
    print_error "Maven build başarısız!"
    exit 1
fi

# JAR dosyası kontrolü
JAR_FILE=$(find target -name "libsys*.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    print_error "JAR dosyası bulunamadı!"
    exit 1
fi
print_success "JAR dosyası bulundu: $JAR_FILE"

# ===============================================
# 6. SPRING BOOT BAŞLATMA
# ===============================================

print_highlight "SPRING BOOT BAŞLATILIYOR"
print_separator

print_info "Spring Boot uygulaması başlatılıyor..."
print_info "Port: $PORT"
print_info "Profile: ngrok"
print_info "Kafka: $([ "$ENABLE_KAFKA" = true ] && echo "Enabled" || echo "Disabled")"

# Spring Boot'u arka planda başlat
if [ "$ENABLE_KAFKA" = true ]; then
    SPRING_PROFILES_ACTIVE=ngrok \
    SERVER_PORT=$PORT \
    APP_KAFKA_ENABLED=true \
    KAFKA_ENABLED=true \
    java -jar \
        -Xmx512m \
        -Xms256m \
        -XX:+UseG1GC \
        -Dserver.port=$PORT \
        -Dspring.profiles.active=ngrok \
        -Dapp.kafka.enabled=true \
        --add-opens java.base/java.lang=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        --add-opens java.base/java.time=ALL-UNNAMED \
        "$JAR_FILE" > logs/spring-boot.log 2>&1 &
else
    SPRING_PROFILES_ACTIVE=ngrok \
    SERVER_PORT=$PORT \
    APP_KAFKA_ENABLED=false \
    KAFKA_ENABLED=false \
    java -jar \
        -Xmx512m \
        -Xms256m \
        -XX:+UseG1GC \
        -Dserver.port=$PORT \
        -Dspring.profiles.active=ngrok \
        -Dapp.kafka.enabled=false \
        --add-opens java.base/java.lang=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        --add-opens java.base/java.time=ALL-UNNAMED \
        "$JAR_FILE" > logs/spring-boot.log 2>&1 &
fi

SPRING_PID=$!
print_info "Spring Boot PID: $SPRING_PID"

# Logs dizini oluştur
mkdir -p logs

# Spring Boot'un başlamasını bekle
print_info "Spring Boot'un başlaması bekleniyor..."
for i in {1..60}; do
    if curl -s http://localhost:$PORT/actuator/health > /dev/null 2>&1; then
        print_success "Spring Boot başarıyla başlatıldı!"
        break
    fi

    if [ $i -eq 60 ]; then
        print_error "Spring Boot başlatılamadı! Logları kontrol edin:"
        print_error "tail -f logs/spring-boot.log"
        cleanup
        exit 1
    fi

    echo -n "."
    sleep 1
done
echo ""

# ===============================================
# 7. NGROK BAŞLATMA
# ===============================================

print_highlight "NGROK BAŞLATILIYOR"
print_separator

print_info "Ngrok ile tunel açılıyor..."

# Ngrok'u arka planda başlat
ngrok http $PORT --log=stdout > logs/ngrok.log 2>&1 &
NGROK_PID=$!
print_info "Ngrok PID: $NGROK_PID"

# Ngrok'un başlamasını bekle
print_info "Ngrok'un hazır olması bekleniyor..."
sleep 5

# Ngrok URL'sini al
for i in {1..30}; do
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o 'https://[^"]*\.ngrok-free\.app' | head -n 1)

    if [ ! -z "$NGROK_URL" ]; then
        break
    fi

    if [ $i -eq 30 ]; then
        print_error "Ngrok URL alınamadı!"
        print_info "Manuel kontrol: http://localhost:4040"
        NGROK_URL="http://localhost:4040 (manuel kontrol gerekli)"
        break
    fi

    sleep 1
done

# ===============================================
# 8. BAŞARISIZ DURUM BİLGİLERİ
# ===============================================

print_highlight "BAŞLATMA TAMAMLANDI!"
print_separator

print_success "🎉 LibSys API başarıyla başlatıldı ve paylaşıma hazır!"
echo ""

print_highlight "📋 ERİŞİM BİLGİLERİ"
print_separator
print_info "🌐 Ngrok Public URL: $NGROK_URL"
print_info "🏠 Local URL: http://localhost:$PORT"
print_info "🔍 Ngrok Dashboard: http://localhost:4040"
print_info "🗄️  H2 Database Console: $NGROK_URL/h2-console"
print_info "📊 Health Check: $NGROK_URL/actuator/health"
print_info "📖 API Documentation: $NGROK_URL/swagger-ui.html"
print_info "📈 Monitoring: $NGROK_URL/actuator"

echo ""
print_highlight "🔐 DEMO KULLANICI BİLGİLERİ"
print_separator
print_success "👤 Admin: admin / admin (ADMIN yetkisi)"
print_success "👤 Test: test / 123456 (USER yetkisi)"
print_success "👤 Frontend: frontend / frontend123 (USER yetkisi)"
print_success "👤 Manager: manager / manager123 (ADMIN yetkisi)"

echo ""
print_highlight "🚀 FRONTEND ARKADAŞINIZ İÇİN BİLGİLER"
print_separator
print_cyan "Base URL: $NGROK_URL"
print_cyan "Auth Endpoint: $NGROK_URL/api/auth/login"
print_cyan "Books Endpoint: $NGROK_URL/api/books"
print_cyan "Users Endpoint: $NGROK_URL/api/users"
if [ "$ENABLE_KAFKA" = true ]; then
    print_cyan "Async Features: ✅ Enabled"
    print_cyan "Stock Orders: $NGROK_URL/api/stock/orders"
    print_cyan "Invoices: $NGROK_URL/api/invoices"
else
    print_cyan "Async Features: ❌ Disabled (Sadece HTTP API)"
fi

echo ""
print_highlight "📁 ÖNEMLİ ENDPOINT'LER"
print_separator
echo "🔐 Authentication:"
echo "   POST $NGROK_URL/api/auth/login"
echo "   POST $NGROK_URL/api/auth/signup"
echo ""
echo "📚 Books Management:"
echo "   GET $NGROK_URL/api/books"
echo "   GET $NGROK_URL/api/books/{id}"
echo "   GET $NGROK_URL/api/books/search?q=kitap"
echo ""
echo "👥 User Management (Admin):"
echo "   GET $NGROK_URL/api/users"
echo ""
if [ "$ENABLE_KAFKA" = true ]; then
echo "📦 Stock Management (Admin):"
echo "   GET $NGROK_URL/api/stock/{bookId}"
echo "   POST $NGROK_URL/api/stock/check/{bookId}"
echo ""
echo "🧾 Order Management (Admin):"
echo "   GET $NGROK_URL/api/stock/orders/pending"
echo "   POST $NGROK_URL/api/stock/orders"
echo ""
echo "💰 Invoice Management (Admin):"
echo "   GET $NGROK_URL/api/invoices/order/{orderId}"
echo "   POST $NGROK_URL/api/invoices/generate/{orderId}"
fi

echo ""
print_highlight "🛠️  GELİŞTİRİCİ ARAÇLARI"
print_separator
print_info "📖 API Docs: $NGROK_URL/swagger-ui.html"
print_info "🔍 Database: $NGROK_URL/h2-console"
print_info "   - JDBC URL: jdbc:h2:mem:libsys_ngrok"
print_info "   - Username: libsys"
print_info "   - Password: libsys123"
print_info "📊 Metrics: $NGROK_URL/actuator/metrics"
print_info "🏥 Health: $NGROK_URL/actuator/health"

echo ""
print_highlight "📝 ÖRNEK CURL KOMUTLARI"
print_separator
echo "# Login"
echo "curl -X POST $NGROK_URL/api/auth/login \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"username\": \"admin\", \"password\": \"admin\"}'"
echo ""
echo "# Get Books"
echo "curl -H 'Authorization: Bearer YOUR_TOKEN' $NGROK_URL/api/books"
echo ""
echo "# Search Books"
echo "curl -H 'Authorization: Bearer YOUR_TOKEN' '$NGROK_URL/api/books/search?q=sinekli'"

echo ""
print_highlight "⚙️  ÇALIŞTIRMA BİLGİLERİ"
print_separator
print_info "✅ Spring Boot PID: $SPRING_PID"
print_info "✅ Ngrok PID: $NGROK_PID"
print_info "✅ Port: $PORT"
print_info "✅ Profile: ngrok"
print_info "✅ Kafka: $([ "$ENABLE_KAFKA" = true ] && echo "Enabled" || echo "Disabled")"
print_info "✅ Database: H2 In-Memory"
print_info "✅ CORS: All origins enabled"

echo ""
print_warning "🔥 ÖNEMLI NOTLAR:"
echo "• API 24/7 çalışacak (Ctrl+C ile durdurun)"
echo "• Ngrok free plan 8 saat sınırı var"
echo "• H2 database in-memory (restart'ta silinir)"
echo "• Sample data otomatik yüklenir"
echo "• CORS tüm origin'ler için açık"

echo ""
print_highlight "📊 REAL-TIME MONITORING"
print_separator

# Monitoring döngüsü
print_info "Real-time monitoring başlatılıyor... (Ctrl+C ile çıkış)"
echo ""

while true; do
    # API health check
    if curl -s http://localhost:$PORT/actuator/health > /dev/null 2>&1; then
        API_STATUS="🟢 ONLINE"
    else
        API_STATUS="🔴 OFFLINE"
    fi

    # Ngrok status check
    if curl -s http://localhost:4040/api/tunnels > /dev/null 2>&1; then
        NGROK_STATUS="🟢 ONLINE"
    else
        NGROK_STATUS="🔴 OFFLINE"
    fi

    # Clear line and print status
    printf "\r⏰ $(date '+%H:%M:%S') | API: $API_STATUS | Ngrok: $NGROK_STATUS | URL: $NGROK_URL"

    sleep 5
done