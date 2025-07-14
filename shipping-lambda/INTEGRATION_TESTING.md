# Интеграционные тесты для системы мониторинга исключений

## Обзор

Интеграционные тесты проверяют полный flow системы мониторинга исключений:
- **Монолит → SNS → Lambda → DynamoDB → CloudWatch**
- Публикация метрик в CloudWatch
- Система алертов для критических исключений
- Группировка похожих исключений
- Обработка высокочастотных ошибок

## Структура тестов

### 1. ExceptionWatcherIntegrationTest
**Файл**: `src/test/java/com/ecommerce/lambda/integration/ExceptionWatcherIntegrationTest.java`

**Назначение**: Тестирование полного flow обработки исключений

**Тестовые сценарии**:
- ✅ `shouldProcessFullExceptionFlow()` - полный цикл обработки
- ✅ `shouldPublishCloudWatchMetrics()` - публикация метрик
- ✅ `shouldGroupSimilarExceptions()` - группировка похожих исключений
- ✅ `shouldHandleFatalExceptionAlert()` - алерты для FATAL ошибок
- ✅ `shouldHandleHighFrequencyExceptions()` - высокочастотные исключения
- ✅ `shouldHandleMultipleServicesAndEnvironments()` - мультисервисная обработка

**Инфраструктура**:
- LocalStack с SNS, DynamoDB, CloudWatch
- Автоматическое создание таблиц и топиков
- Изолированное тестирование

### 2. ExceptionMonitoringIntegrationTest
**Файл**: `backend/src/test/java/com/ecommerce/common/integration/ExceptionMonitoringIntegrationTest.java`

**Назначение**: Тестирование интеграции монолита с SNS

**Тестовые сценарии**:
- ✅ `shouldPublishExceptionToSnsWhenControllerThrows()` - публикация из контроллера
- ✅ `shouldIncludeFullRequestContextInException()` - контекст запроса
- ✅ `shouldNotPublishWhenPublisherDisabled()` - отключение публикации
- ✅ `shouldHandleMultipleExceptionsInSequence()` - последовательные исключения
- ✅ `shouldHandleExceptionPublisherFailureGracefully()` - обработка ошибок
- ✅ `shouldWorkWithRealSnsIntegration()` - реальная интеграция с SNS
- ✅ `shouldPreserveCorrelationIdAcrossRequests()` - сохранение correlation ID

**Инфраструктура**:
- Spring Boot Test с MockMvc
- LocalStack SNS для реальной интеграции
- Моки для изоляции компонентов

### 3. CloudWatchMetricsIntegrationTest
**Файл**: `src/test/java/com/ecommerce/lambda/integration/CloudWatchMetricsIntegrationTest.java`

**Назначение**: Тестирование публикации метрик в CloudWatch

**Тестовые сценарии**:
- ✅ `shouldPublishBasicExceptionMetrics()` - базовые метрики
- ✅ `shouldPublishCriticalExceptionMetrics()` - критические метрики
- ✅ `shouldPublishFatalExceptionMetrics()` - FATAL метрики
- ✅ `shouldPublishGroupedExceptionMetrics()` - группированные метрики
- ✅ `shouldPublishHighFrequencyExceptionMetrics()` - высокочастотные метрики
- ✅ `shouldPublishMetricsWithVersionAndTags()` - метрики с версией и тегами
- ✅ `shouldAggregateMetricsOverTime()` - агрегация во времени
- ✅ `shouldHandleMultipleEnvironments()` - мультисредовые метрики

**Проверяемые метрики**:
- `ExceptionCount` - общее количество исключений
- `ExceptionCountByType` - по типу исключения
- `ExceptionCountByLevel` - по уровню (ERROR, WARN, FATAL)
- `ExceptionCountByService` - по сервису
- `ExceptionCountByEnvironment` - по среде
- `CriticalExceptionCount` - критические исключения
- `FatalExceptionCount` - фатальные исключения
- `GroupedExceptionCount` - группированные исключения
- `HighFrequencyException` - высокочастотные исключения

### 4. AlertingIntegrationTest
**Файл**: `src/test/java/com/ecommerce/lambda/integration/AlertingIntegrationTest.java`

**Назначение**: Тестирование системы алертов

**Тестовые сценарии**:
- ✅ `shouldTriggerImmediateAlertForFatalException()` - немедленный алерт для FATAL
- ✅ `shouldTriggerHighFrequencyAlert()` - алерт высокочастотных исключений
- ✅ `shouldNotTriggerAlertForLowFrequencyErrors()` - отсутствие алерта для редких ошибок
- ✅ `shouldTriggerAlertForCriticalExceptions()` - алерты для критических исключений
- ✅ `shouldNotTriggerAlertForWarnings()` - отсутствие алерта для предупреждений
- ✅ `shouldTriggerAlertForMultipleServiceFailures()` - алерты для множественных сбоев
- ✅ `shouldCreateAlertMetricsWithCorrectDimensions()` - правильные dimensions
- ✅ `shouldHandleTimeWindowForAlerts()` - временные окна для алертов

**Пороги алертов**:
- **FATAL исключения**: немедленный алерт
- **ERROR исключения**: алерт при >10 в течение 5 минут
- **Высокочастотные исключения**: алерт при >10 одинаковых исключений в группе
- **WARN исключения**: алерт не срабатывает

## Технические детали

### Используемые технологии
- **TestContainers**: изолированное тестирование с Docker
- **LocalStack**: эмуляция AWS сервисов (SNS, DynamoDB, CloudWatch)
- **Awaitility**: асинхронное тестирование с ожиданием
- **Spring Boot Test**: интеграционное тестирование монолита
- **MockMvc**: тестирование HTTP endpoint'ов
- **Mockito**: мокирование зависимостей

### Конфигурация LocalStack
```java
@Container
static LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:3.0")
).withServices(SNS, DYNAMODB, CLOUDWATCH);
```

### Настройка AWS клиентов
```java
AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
    .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
    .credentialsProvider(credentialsProvider)
    .region(Region.US_EAST_1)
    .build();
```

### Асинхронное тестирование
```java
await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
    assertThat(response.datapoints()).isNotEmpty();
    assertThat(response.datapoints().get(0).sum()).isEqualTo(1.0);
});
```

## Запуск тестов

### Все интеграционные тесты
```bash
cd shipping-lambda
mvn test -Dtest="*IntegrationTest"
```

### Конкретный тест
```bash
# Полный flow
mvn test -Dtest=ExceptionWatcherIntegrationTest

# CloudWatch метрики
mvn test -Dtest=CloudWatchMetricsIntegrationTest

# Система алертов
mvn test -Dtest=AlertingIntegrationTest
```

### Backend интеграционные тесты
```bash
cd backend
mvn test -Dtest=ExceptionMonitoringIntegrationTest
```

## Требования к окружению

### Docker
Для запуска LocalStack требуется Docker:
```bash
docker --version
# Docker version 20.10.0 или выше
```

### Память
Рекомендуется выделить Docker не менее 4GB RAM для стабильной работы LocalStack.

### Порты
LocalStack использует порты:
- 4566 (основной endpoint)
- 4571 (legacy)

## Отладка

### Логи LocalStack
```java
@Container
static LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:3.0")
).withServices(SNS, DYNAMODB, CLOUDWATCH)
 .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));
```

### Проверка состояния сервисов
```bash
# Проверка DynamoDB таблиц
aws dynamodb list-tables --endpoint-url=http://localhost:4566

# Проверка SNS топиков
aws sns list-topics --endpoint-url=http://localhost:4566

# Проверка CloudWatch метрик
aws cloudwatch list-metrics --endpoint-url=http://localhost:4566
```

### Увеличение timeout'ов
Для медленных систем можно увеличить timeout'ы:
```java
await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
    // Проверки
});
```

## Покрытие тестов

### Функциональное покрытие
- ✅ Полный flow обработки исключений
- ✅ Публикация в SNS из монолита
- ✅ Обработка Lambda функцией
- ✅ Сохранение в DynamoDB
- ✅ Публикация метрик в CloudWatch
- ✅ Группировка похожих исключений
- ✅ Система алертов
- ✅ Обработка ошибок

### Сценарии покрытия
- ✅ Одиночные исключения
- ✅ Массовые исключения
- ✅ Критические исключения (FATAL)
- ✅ Высокочастотные исключения
- ✅ Мультисервисная обработка
- ✅ Различные среды (prod, dev, test)
- ✅ Временные окна группировки
- ✅ Обработка ошибок инфраструктуры

## Метрики производительности

### Время выполнения тестов
- **ExceptionWatcherIntegrationTest**: ~2-3 минуты
- **CloudWatchMetricsIntegrationTest**: ~3-4 минуты
- **AlertingIntegrationTest**: ~2-3 минуты
- **ExceptionMonitoringIntegrationTest**: ~1-2 минуты

### Оптимизация
- Параллельный запуск тестов в разных модулях
- Переиспользование LocalStack контейнеров
- Сокращение timeout'ов для стабильных тестов

## Заключение

Интеграционные тесты обеспечивают:
- **Полное покрытие** критических путей системы
- **Раннее обнаружение** проблем интеграции
- **Документирование** ожидаемого поведения
- **Защиту от регрессий** при изменениях
- **Уверенность** в работоспособности системы

Тесты автоматически запускаются в CI/CD pipeline и служат основой для валидации системы мониторинга исключений перед развертыванием в продакшн. 