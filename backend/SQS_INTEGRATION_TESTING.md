# 🧪 Интеграционные тесты SQS с LocalStack

## 📋 Обзор

Интеграционные тесты для **SQS** обработки `OrderStatusUpdated` событий с использованием LocalStack для эмуляции AWS сервисов. Включает тестирование основной очереди, Dead Letter Queue, и мониторинга.

## 🏗️ Архитектура тестирования

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  LocalStack │    │    Tests    │    │   Backend   │
│             │    │             │    │             │
│ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │
│ │   SNS   │ │◄──┤ │ Publish │ │    │ │ SQS     │ │
│ └─────────┘ │    │ │ Message │ │    │ │ Listener│ │
│ ┌─────────┐ │    │ └─────────┘ │    │ └─────────┘ │
│ │   SQS   │ │◄──┤ ┌─────────┐ │───►│ ┌─────────┐ │
│ └─────────┘ │    │ │ Receive │ │    │ │ Order   │ │
│             │    │ │ & Test  │ │    │ │ Update  │ │
└─────────────┘    │ └─────────┘ │    │ │ Listener│ │
                   └─────────────┘    │ └─────────┘ │
                                      │ ┌─────────┐ │
                                      │ │Database │ │
                                      │ └─────────┘ │
                                      └─────────────┘
```

## 📁 Структура тестов

### **1. SqsVsHttpsIntegrationTest**
**Файл:** `src/test/java/com/ecommerce/shipping/integration/SqsVsHttpsIntegrationTest.java`

**Назначение:** Функциональное сравнение SQS и HTTPS обработки

**Тестовые сценарии:**
- ✅ `shouldProcessOrderStatusUpdateViaSqs()` - обработка через SQS
- ✅ `shouldProcessOrderStatusUpdateViaHttps()` - обработка через HTTPS
- ✅ `shouldProduceSameResultsForSqsAndHttps()` - сравнение результатов
- ✅ `shouldHandleInvalidMessageInDlq()` - обработка некорректных сообщений

### **2. SqsPerformanceTest**
**Файл:** `src/test/java/com/ecommerce/shipping/integration/SqsPerformanceTest.java`

**Назначение:** Тестирование производительности и нагрузки

**Тестовые сценарии:**
- ✅ `shouldComparePerformanceSqsVsHttps()` - сравнение производительности
- ✅ `shouldHandleBurstLoad()` - тестирование burst нагрузки

## 🚀 Запуск тестов

### **Автоматический запуск (рекомендуется)**
```bash
cd backend
chmod +x run-sqs-integration-tests.sh
./run-sqs-integration-tests.sh
```

### **Ручной запуск отдельных тестов**

#### Функциональные тесты
```bash
mvn test -Dtest=SqsVsHttpsIntegrationTest -Dspring.profiles.active=test
```

#### Тесты производительности
```bash
mvn test -Dtest=SqsPerformanceTest -Dspring.profiles.active=test
```

#### Unit тесты SQS
```bash
mvn test -Dtest=OrderStatusSqsListenerTest -Dspring.profiles.active=test
```

### **Запуск всех интеграционных тестов**
```bash
mvn test -Dtest="*IntegrationTest" -Dspring.profiles.active=test
```

## 📊 Что тестируется

### **Функциональность**
| Аспект | SQS | HTTPS | Результат |
|--------|-----|-------|-----------|
| **Доставка сообщений** | ✅ Гарантированная | ⚠️ Best effort | SQS лучше |
| **Retry логика** | ✅ Автоматическая | ❌ Ручная | SQS лучше |  
| **Dead Letter Queue** | ✅ Встроенная | ❌ Нет | SQS лучше |
| **Обработка ошибок** | ✅ Надежная | ⚠️ Зависит от endpoint | SQS лучше |
| **Простота реализации** | ⚠️ Сложнее | ✅ Проще | HTTPS лучше |

### **Производительность**
- **Latency:** Время от отправки до обработки
- **Throughput:** Количество сообщений в секунду
- **Burst handling:** Обработка пиковых нагрузок
- **Resource usage:** Использование памяти и CPU

### **Надежность**
- **Message durability:** Сохранность сообщений
- **Retry mechanisms:** Механизмы повторных попыток
- **Error handling:** Обработка ошибок
- **Monitoring:** Возможности мониторинга

## 🔧 Требования к окружению

### **Docker**
```bash
# Проверка установки Docker
docker --version
# Docker version 20.10.0+

# Проверка что Docker запущен
docker info
```

### **Java и Maven**
```bash
# Java 17+
java -version

# Maven 3.6+
mvn -version
```

### **Память**
- **Рекомендуется:** 4GB+ RAM для Docker
- **LocalStack:** использует ~1GB RAM
- **Тесты:** дополнительно ~512MB

## 📈 Интерпретация результатов

### **Пример вывода функциональных тестов**
```
✅ shouldProcessOrderStatusUpdateViaSqs: PASSED
✅ shouldProcessOrderStatusUpdateViaHttps: PASSED  
✅ shouldProduceSameResultsForSqsAndHttps: PASSED
✅ shouldHandleInvalidMessageInDlq: PASSED

Результат: SQS и HTTPS обрабатывают сообщения идентично ✅
```

### **Пример вывода тестов производительности**
```
=== Performance Comparison Results ===
Messages processed: 10
HTTPS processing time: 1250 ms
SQS processing time: 2100 ms
HTTPS avg per message: 125.0 ms
SQS avg per message: 210.0 ms
HTTPS is faster by: 850 ms

=== Burst Load Test Results ===
Burst size: 50 messages
Total processing time: 8500 ms
Average per message: 170.0 ms
Throughput: 5.88 messages/sec
```

### **Критерии для принятия решения**

#### **Выбирайте SQS если:**
- ✅ Надежность критична
- ✅ Ожидаются высокие нагрузки
- ✅ Нужна автоматическая retry логика
- ✅ Важен мониторинг и алерты
- ✅ Планируется scaling

#### **Выбирайте HTTPS если:**
- ✅ Простота важнее надежности
- ✅ Низкие нагрузки (< 100 событий/час)
- ✅ Минимальная latency критична
- ✅ Команда не готова к SQS complexity
- ✅ Быстрая разработка приоритет

## 🛠️ Настройка и конфигурация

### **LocalStack конфигурация**
```java
@Container
static LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:3.0"))
    .withServices(SNS, SQS);
```

### **Тестовые настройки**
```yaml
# application-test.yml
aws:
  region: us-east-1
  account-id: 820242910367
  sqs:
    order-status-update-queue-name: test-queue
    order-status-update-dlq-name: test-dlq
```

### **Тестовые данные**
```java
// Создание тестового ShippingInfo
ShippingInfo shippingInfo = new ShippingInfo();
shippingInfo.setOrderId(orderId);
shippingInfo.setStatus(ShippingStatus.PENDING);
shippingInfo.setCarrier("Test Carrier");
```

## 🐛 Troubleshooting

### **Docker проблемы**
```bash
# Очистка контейнеров
docker stop $(docker ps -q --filter "label=org.testcontainers")
docker rm $(docker ps -aq --filter "label=org.testcontainers")

# Очистка образов
docker rmi $(docker images -q localstack/localstack)
```

### **Тесты не запускаются**
```bash
# Проверка тестового профиля
mvn test -Dspring.profiles.active=test -X

# Принудительная пересборка
mvn clean compile test-compile
```

### **LocalStack недоступен**
```bash
# Проверка логов LocalStack
docker logs $(docker ps -q --filter "ancestor=localstack/localstack")

# Ручной запуск LocalStack
docker run -d -p 4566:4566 localstack/localstack:3.0
```

### **Тесты падают с timeout**
```java
// Увеличение timeout в тестах
await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
    // Проверки
});
```

## 📋 Чек-лист перед production

### **Функциональность**
- [ ] Все функциональные тесты проходят
- [ ] SQS и HTTPS дают идентичные результаты
- [ ] DLQ корректно обрабатывает ошибки
- [ ] Retry логика работает как ожидается

### **Производительность**
- [ ] Latency в приемлемых пределах (< 5 секунд)
- [ ] Throughput достаточный для нагрузки
- [ ] Burst load обрабатывается без ошибок
- [ ] Memory usage в норме

### **Мониторинг**
- [ ] CloudWatch метрики настроены
- [ ] Алерты на DLQ сообщения
- [ ] Логирование работает корректно
- [ ] Health checks проходят

## 🎯 Следующие шаги

1. **Запустите тесты** и проанализируйте результаты
2. **Выберите подход** на основе результатов тестирования
3. **Обновите CloudFormation** если нужны изменения
4. **Deploy в staging** для финального тестирования
5. **Настройте мониторинг** в production окружении

## 📞 Поддержка

При возникновении проблем с тестами:
1. Проверьте Docker и LocalStack
2. Убедитесь что используется тестовый профиль
3. Проверьте логи TestContainers
4. Попробуйте очистить Docker контейнеры 