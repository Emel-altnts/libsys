#!/bin/bash

# 🚀 LibSys Ngrok Startup Script - Enhanced Edition
# Bu script tüm servisleri otomatik başlatır ve ngrok ile paylaşır

set -e

echo "🚀 LibSys Ngrok Startup Script - Enhanced Edition"
echo "=================================================="

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
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

print_highlight() {
    echo -e "${PURPLE}🌟 $1${NC}"
}

print_cyan() {
    echo -e "${CYAN}🔧 $1${NC}"
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
        docker stop temp-kafka temp-kafka-zk 2>/dev/null || true
        docker rm temp-kafka temp-kafka-zk 2>/dev/null || true
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
echo "=================="

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
    NGROK_VERSION=$(ngrok version | head -n 1 | awk '{print $3}')
    print_success "Ngrok $NGROK_VERSION detected - OK"
else
    print_error "Ngrok bulunamadı!"
    print_cyan "Ngrok kurulum:"
    print_cyan "  • macOS: brew install ngrok"
    print_cyan "  • Windows: choco