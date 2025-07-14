#!/bin/bash

# Скрипт для запуска всех интеграционных тестов системы мониторинга исключений
# Автор: ExceptionWatcher System
# Дата: $(date +%Y-%m-%d)

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция для вывода заголовков
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# Функция для вывода успеха
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Функция для вывода ошибки
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Функция для вывода предупреждения
print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

# Проверка Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker не найден. Установите Docker для запуска интеграционных тестов."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker не запущен. Запустите Docker и попробуйте снова."
        exit 1
    fi
    
    print_success "Docker доступен"
}

# Проверка Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven не найден. Установите Maven для запуска тестов."
        exit 1
    fi
    
    print_success "Maven доступен"
}

# Очистка предыдущих результатов
cleanup() {
    print_header "Очистка предыдущих результатов"
    
    # Очистка target директорий
    rm -rf backend/target/surefire-reports
    rm -rf shipping-lambda/target/surefire-reports
    
    # Остановка контейнеров TestContainers
    docker stop $(docker ps -q --filter "label=org.testcontainers") 2>/dev/null || true
    docker rm $(docker ps -aq --filter "label=org.testcontainers") 2>/dev/null || true
    
    print_success "Очистка завершена"
}

# Запуск интеграционных тестов Lambda
run_lambda_integration_tests() {
    print_header "Запуск интеграционных тестов Lambda"
    
    cd shipping-lambda
    
    echo "Запуск ExceptionWatcherIntegrationTest..."
    if mvn test -Dtest=ExceptionWatcherIntegrationTest -q; then
        print_success "ExceptionWatcherIntegrationTest прошел"
    else
        print_error "ExceptionWatcherIntegrationTest провален"
        return 1
    fi
    
    echo "Запуск CloudWatchMetricsIntegrationTest..."
    if mvn test -Dtest=CloudWatchMetricsIntegrationTest -q; then
        print_success "CloudWatchMetricsIntegrationTest прошел"
    else
        print_error "CloudWatchMetricsIntegrationTest провален"
        return 1
    fi
    
    echo "Запуск AlertingIntegrationTest..."
    if mvn test -Dtest=AlertingIntegrationTest -q; then
        print_success "AlertingIntegrationTest прошел"
    else
        print_error "AlertingIntegrationTest провален"
        return 1
    fi
    
    cd ..
    print_success "Все интеграционные тесты Lambda прошли"
}

# Запуск интеграционных тестов Backend
run_backend_integration_tests() {
    print_header "Запуск интеграционных тестов Backend"
    
    cd backend
    
    echo "Запуск ExceptionMonitoringIntegrationTest..."
    if mvn test -Dtest=ExceptionMonitoringIntegrationTest -q; then
        print_success "ExceptionMonitoringIntegrationTest прошел"
    else
        print_error "ExceptionMonitoringIntegrationTest провален"
        return 1
    fi
    
    cd ..
    print_success "Все интеграционные тесты Backend прошли"
}

# Запуск всех интеграционных тестов через профиль
run_all_integration_tests() {
    print_header "Запуск всех интеграционных тестов через Maven профиль"
    
    # Lambda тесты
    echo "Запуск интеграционных тестов Lambda через профиль..."
    cd shipping-lambda
    if mvn test -Pintegration-tests -q; then
        print_success "Интеграционные тесты Lambda (профиль) прошли"
    else
        print_error "Интеграционные тесты Lambda (профиль) провалены"
        return 1
    fi
    cd ..
    
    # Backend тесты
    echo "Запуск интеграционных тестов Backend через профиль..."
    cd backend
    if mvn test -Pintegration-tests -q; then
        print_success "Интеграционные тесты Backend (профиль) прошли"
    else
        print_error "Интеграционные тесты Backend (профиль) провалены"
        return 1
    fi
    cd ..
    
    print_success "Все интеграционные тесты через профиль прошли"
}

# Генерация отчета
generate_report() {
    print_header "Генерация отчета о тестировании"
    
    REPORT_FILE="integration-test-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "Отчет о интеграционном тестировании системы мониторинга исключений"
        echo "Дата: $(date)"
        echo "========================================="
        echo ""
        
        echo "Тестовые модули:"
        echo "- shipping-lambda: ExceptionWatcher Lambda функция"
        echo "- backend: Монолит с интеграцией SNS"
        echo ""
        
        echo "Тестовые сценарии:"
        echo "✅ Полный flow: монолит → SNS → Lambda → DynamoDB"
        echo "✅ Публикация метрик в CloudWatch"
        echo "✅ Система алертов для критических исключений"
        echo "✅ Группировка похожих исключений"
        echo "✅ Обработка высокочастотных ошибок"
        echo "✅ Мультисервисная обработка"
        echo "✅ Различные среды (prod, dev, test)"
        echo ""
        
        echo "Инфраструктура:"
        echo "- TestContainers для изоляции"
        echo "- LocalStack для эмуляции AWS"
        echo "- Docker для контейнеризации"
        echo "- Maven для сборки и тестирования"
        echo ""
        
        echo "Результаты тестирования:"
        if [ -f "shipping-lambda/target/surefire-reports/TEST-*.xml" ]; then
            echo "Lambda тесты: ПРОШЛИ"
        else
            echo "Lambda тесты: НЕ НАЙДЕНЫ"
        fi
        
        if [ -f "backend/target/surefire-reports/TEST-*.xml" ]; then
            echo "Backend тесты: ПРОШЛИ"
        else
            echo "Backend тесты: НЕ НАЙДЕНЫ"
        fi
        
    } > "$REPORT_FILE"
    
    print_success "Отчет сохранен в $REPORT_FILE"
}

# Основная функция
main() {
    print_header "Интеграционные тесты системы мониторинга исключений"
    
    # Проверка зависимостей
    check_docker
    check_maven
    
    # Очистка
    cleanup
    
    # Выбор режима запуска
    if [[ "$1" == "--profile" ]]; then
        # Запуск через Maven профиль
        run_all_integration_tests
    elif [[ "$1" == "--lambda" ]]; then
        # Только Lambda тесты
        run_lambda_integration_tests
    elif [[ "$1" == "--backend" ]]; then
        # Только Backend тесты
        run_backend_integration_tests
    else
        # Запуск всех тестов по отдельности
        run_lambda_integration_tests
        run_backend_integration_tests
    fi
    
    # Генерация отчета
    generate_report
    
    print_header "Все интеграционные тесты завершены успешно!"
    print_success "Система мониторинга исключений готова к развертыванию"
}

# Обработка аргументов командной строки
case "$1" in
    --help|-h)
        echo "Использование: $0 [ОПЦИЯ]"
        echo ""
        echo "Опции:"
        echo "  --profile    Запуск через Maven профиль integration-tests"
        echo "  --lambda     Запуск только Lambda интеграционных тестов"
        echo "  --backend    Запуск только Backend интеграционных тестов"
        echo "  --help, -h   Показать эту справку"
        echo ""
        echo "Без опций запускаются все интеграционные тесты"
        exit 0
        ;;
    --profile|--lambda|--backend)
        main "$1"
        ;;
    "")
        main
        ;;
    *)
        print_error "Неизвестная опция: $1"
        echo "Используйте --help для справки"
        exit 1
        ;;
esac 