#!/bin/bash

# 🚀 LibSys Ngrok Startup Script
# Bu script tüm servisleri otomatik başlatır ve ngrok ile paylaşır

set -e

echo "🚀 LibSys Ngrok Startup Script"
echo "================================"

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Java version check
print_info "Java versiyonu kontrol ediliyor..."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 17 ]; then
    print_success "Java $JAVA_VERSION detected - OK"
else
    print_error "Java 17+ gerekli! Mevcut versiyon: $JAVA_VERSION"
    exit 1
fi

# Maven version check
print_info "Maven versiyonu kontrol ediliyor..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    print_success "Maven $MVN_VERSION detected - OK"
else
    print_error "Maven bulunamadı! Maven kurulumu gerekli."
    exit 1
fi

# Ngrok version check
print_info "Ngrok versiyonu kontrol ediliyor..."
if command -v ngrok &> /dev/null; then
    NGROK_VERSION=$(ngrok version | head -n 1 | awk '{print $3}')
    print_success "Ngrok $NGROK_VERSION detected - OK"
else
    print_error "Ngrok bulunamadı!"
    print_warning "Ngrok kurulumu için: https://ngrok.com/download"
    print_warning "Veya: brew install ngrok (macOS) / choco install ngrok (Windows)"
    exit 1
fi

# Port kontrolü
PORT=${PORT:-8080}
print_info "Port $PORT kullanılabilirliği kontrol ediliyor..."
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    print_warning "Port $PORT kullanımda! Durdurulsun mu? (y/n)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_info "Port $PORT üzerindeki process durduruluyor..."
        lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
        sleep 2
    else
        print_error "Port $PORT kullanımda - çıkılıyor"
        exit 1
    fi
fi

# Proje dizini kontrolü
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml bulunamadı! Proje dizininde olduğunuzdan emin olun."
    exit 1
fi

print_info "Proje temizleniyor ve derleniyor..."
mvn clean compile -q

# Kafka kontrol - optional
KAFKA_ENABLED=${KAFKA_ENABLED:-true}
if [ "$KAFKA_ENABLED" = "true" ]; then
    print_info "Kafka durumu kontrol ediliyor..."
    if ! nc -z localhost 9092 2>/dev/null; then
        print_warning "Kafka bulunamadı - Docker ile başlatılsın mı? (y/n)"
        read -r kafka_response
        if [[ "$kafka_response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
            print_info "Kafka Docker ile başlatılıyor..."

            # Mini Kafka setup with docker
            docker run -d --name temp-kafka-zk \
                -p 2181:2181 \
                -e ZOOKEEPER_CLIENT_PORT=2181 \
                -e ZOOKEEPER_TICK_TIME=2000 \
                confluentinc/cp-zookeeper:7.4.0

            sleep 5

            docker run -d --name temp-kafka \
                -p 9092:9092 \
                -e KAFKA_BROKER_ID=1 \
                -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
                -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
                -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
                confluentinc/cp-kafka:7.4.0

            print_info "Kafka başlatılıyor... (30 saniye bekleniyor)"
            sleep 30

        else
            print_warning "Kafka olmadan devam ediliyor - sync mode"
            KAFKA_ENABLED=false
        fi
    else
        print_success "Kafka hazır - localhost:9092"
    fi
fi

# Background'da Spring Boot başlat
print_info "Spring Boot uygulaması başlatılıyor..."
print_info "Port: $PORT"
print_info "Kafka: $KAFKA_ENABLED"
print_info "Profile: dev"

# Spring Boot'u background'da başlat
nohup mvn spring-boot:run \
    -Dspring.profiles.active=dev \
    -Dserver.port=$PORT \
    -Dapp.kafka.enabled=$KAFKA_ENABLED \
    > libsys.log 2>&1 &

SPRING_PID=$!
print_info "Spring Boot PID: $SPRING_PID"

# Spring Boot'un başlamasını bekle
print_info "Uygulama başlatılıyor... (30 saniye bekleniyor)"
sleep 10

# Health check
for i in {1..20}; do
    if curl -s http://localhost:$PORT/actuator/health > /dev/null; then
        print_success "Uygulama hazır!"
        break
    fi
    print_info "Uygulama başlaması bekleniyor... ($i/20)"
    sleep 3
done

# Final health check
if ! curl -s http://localhost:$PORT/actuator/health > /dev/null; then
    print_error "Uygulama başlatılamadı!"
    print_info "Log kontrolü için: tail -f libsys.log"
    exit 1
fi

# API endpoints test
print_info "API endpoints test ediliyor..."
API_HEALTH=$(curl -s http://localhost:$PORT/actuator/health | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
print_success "Health Status: $API_HEALTH"

# Ngrok başlat
print_info "Ngrok tunnel açılıyor..."
print_warning "Ngrok auth token ayarlandığından emin olun: ngrok config add-authtoken YOUR_TOKEN"

# Ngrok'u background'da başlat
nohup ngrok http $PORT --log=stdout > ngrok.log 2>&1 &
NGROK_PID=$!

print_info "Ngrok PID: $NGROK_PID"
print_info "Ngrok başlatılıyor... (10 saniye bekleniyor)"
sleep 10

# Ngrok URL'sini al
NGROK_URL=""
for i in {1..10}; do
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url' 2>/dev/null || echo "")
    if [ "$NGROK_URL" != "" ] && [ "$NGROK_URL" != "null" ]; then
        break
    fi
    print_info "Ngrok URL bekleniyor... ($i/10)"
    sleep 2
done

echo ""
echo "🎉 BAŞARILI! TÜM SERVİSLER HAZIR!"
echo "================================="
echo ""
print_success "✅ Spring Boot: http://localhost:$PORT"
if [ "$NGROK_URL" != "" ] && [ "$NGROK_URL" != "null" ]; then
    print_success "✅ Ngrok Public URL: $NGROK_URL"
    print_success "✅ Ngrok Dashboard: http://localhost:4040"
else
    print_warning "⚠️  Ngrok URL alınamadı - http://localhost:4040 kontrol edin"
fi
print_success "✅ H2 Console: http://localhost:$PORT/h2-console"
print_success "✅ Health Check: http://localhost:$PORT/actuator/health"
print_success "✅ API Docs: http://localhost:$PORT/actuator/info"

echo ""
echo "📚 API ENDPOINTS:"
echo "=================="
if [ "$NGROK_URL" != "" ] && [ "$NGROK_URL" != "null" ]; then
    BASE_URL=$NGROK_URL
else
    BASE_URL="http://localhost:$PORT"
fi

echo ""
echo "🔐 Authentication:"
echo "  POST $BASE_URL/api/auth/login"
echo "  POST $BASE_URL/api/auth/signup"
echo ""
echo "📚 Books:"
echo "  GET  $BASE_URL/api/books"
echo "  GET  $BASE_URL/api/books/categories"
echo "  GET  $BASE_URL/api/books/search?q=java"
echo ""
echo "📦 Stock:"
echo "  GET  $BASE_URL/api/stock/1"
echo "  GET  $BASE_URL/api/stock/low-stock"
if [ "$KAFKA_ENABLED" = "true" ]; then
    echo "  POST $BASE_URL/api/stock/check/1"
fi
echo ""
echo "🛒 Orders:"
echo "  GET  $BASE_URL/api/stock/orders/pending"
if [ "$KAFKA_ENABLED" = "true" ]; then
    echo "  POST $BASE_URL/api/stock/orders"
fi
echo ""
echo "👥 Users:"
echo "  GET  $BASE_URL/api/users"
echo ""

echo "🔑 Test Kullanıcıları:"
echo "====================="
echo "  Username: admin    Password: admin      (ADMIN role)"
echo "  Username: test     Password: 123456     (USER role)"
echo "  Username: manager  Password: manager123 (ADMIN role)"

echo ""
print_info "📊 Monitoring:"
echo "  - Uygulama Logları: tail -f libsys.log"
echo "  - Ngrok Logları: tail -f ngrok.log"
echo "  - Servisleri Durdur: kill $SPRING_PID $NGROK_PID"
if [ "$KAFKA_ENABLED" = "true" ]; then
    echo "  - Kafka Durdur: docker stop temp-kafka temp-kafka-zk && docker rm temp-kafka temp-kafka-zk"
fi

echo ""
print_success "🚀 NGROK ile tüm dünyaya açık LibSys hazır!"
print_warning "💡 Free tier: 2 saat sonra tunnel kapanır"
print_info "🔄 Yeniden başlatmak için bu scripti tekrar çalıştırın"

echo ""
echo "Çalışmaya devam etmek için CTRL+C ile çıkabilirsiniz."
echo "Servisleri durdurmak için: kill $SPRING_PID $NGROK_PID"

# Bekleme döngüsü
trap 'echo ""; print_info "Servisler durduruluyor..."; kill $SPRING_PID $NGROK_PID 2>/dev/null; exit 0' INT

while true; do
    sleep 30
    # Health check
    if ! curl -s http://localhost:$PORT/actuator/health > /dev/null; then
        print_error "Uygulama yanıt vermiyor!"
        break
    fi

    # Ngrok check
    if ! curl -s http://localhost:4040/api/tunnels > /dev/null; then
        print_error "Ngrok yanıt vermiyor!"
        break
    fi

    print_info "Servisler çalışıyor... $(date '+%H:%M:%S')"
done