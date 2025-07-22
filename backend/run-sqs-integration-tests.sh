#!/bin/bash

# Скрипт для запуска интеграционных тестов SQS vs HTTPS с LocalStack

set -e

echo "🚀 Запуск интеграционных тестов SQS vs HTTPS"
echo "=============================================="

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Установите Docker для запуска LocalStack."
    exit 1
fi

# Проверка что Docker запущен
if ! docker info &> /dev/null; then
    echo "❌ Docker не запущен. Запустите Docker Desktop."
    exit 1
fi

echo "✅ Docker готов к использованию"

# Очистка предыдущих контейнеров
echo "🧹 Очистка предыдущих контейнеров TestContainers..."
docker stop $(docker ps -q --filter "label=org.testcontainers") 2>/dev/null || true
docker rm $(docker ps -aq --filter "label=org.testcontainers") 2>/dev/null || true

# Переход в директорию backend
cd "$(dirname "$0")"

echo "📦 Компиляция проекта..."
./mvnw clean compile test-compile

echo "🧪 Запуск интеграционных тестов..."

# Функциональные тесты
echo "1️⃣ Запуск функциональных тестов SQS vs HTTPS..."
./mvnw test -Dtest=SqsVsHttpsIntegrationTest -Dspring.profiles.active=test

if [ $? -eq 0 ]; then
    echo "✅ Функциональные тесты прошли успешно!"
else
    echo "❌ Функциональные тесты завершились с ошибкой"
    exit 1
fi

# Тесты производительности  
echo "2️⃣ Запуск тестов производительности..."
./mvnw test -Dtest=SqsPerformanceTest -Dspring.profiles.active=test

if [ $? -eq 0 ]; then
    echo "✅ Тесты производительности прошли успешно!"
else
    echo "❌ Тесты производительности завершились с ошибкой"
    exit 1
fi

# Unit тесты SQS
echo "3️⃣ Запуск unit тестов SQS..."
./mvnw test -Dtest=OrderStatusSqsListenerTest -Dspring.profiles.active=test

if [ $? -eq 0 ]; then
    echo "✅ Unit тесты SQS прошли успешно!"
else
    echo "❌ Unit тесты SQS завершились с ошибкой"
    exit 1
fi

# Очистка контейнеров после тестов
echo "🧹 Очистка контейнеров после тестов..."
docker stop $(docker ps -q --filter "label=org.testcontainers") 2>/dev/null || true
docker rm $(docker ps -aq --filter "label=org.testcontainers") 2>/dev/null || true

echo ""
echo "🎉 Все интеграционные тесты SQS завершены успешно!"
echo "=============================================="
echo ""
echo "📊 Результаты тестирования:"
echo "✅ Функциональность: SQS и HTTPS работают идентично"
echo "📈 Производительность: Сравнение времени обработки выполнено"
echo "🔧 Unit тесты: SQS Listener работает корректно"
echo ""
echo "🚀 SQS интеграция готова к production!"
echo ""
echo "📋 Следующие шаги:"
echo "1. Выберите предпочтительный подход (SQS или HTTPS)"
echo "2. Обновите CloudFormation конфигурацию при необходимости"
echo "3. Deploy в staging окружение для финального тестирования" 