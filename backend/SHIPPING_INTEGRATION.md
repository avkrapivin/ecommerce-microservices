# Интеграция доставки с микросервисной архитектурой

## Обзор

После выноса логики доставки в отдельную лямбду, монолит больше не знает о внутренних процессах доставки. Для решения этой проблемы реализована следующая архитектура:

## Архитектура

### 1. Поток данных

```
Монолит → SNS OrderUnconfirmed → Лямбда → SNS OrderStatusUpdated → Монолит
```

### 2. Компоненты

#### В монолите:
- **OrderStatusUpdateListener** - обрабатывает события обновления статуса от лямбды
- **OrderStatusSqsListener** - опрашивает SQS очередь и обрабатывает сообщения
- **DlqProcessor** - управляет сообщениями из Dead Letter Queue
- **ShippoWebhookController** - получает webhook'и от Shippo
- **ShippingService** - управляет записями о доставке и предоставляет API
- **ShippingController** - предоставляет API для получения информации о доставке

#### В лямбде:
- **ProcessDeliveryHandler** - обрабатывает события готовности к доставке
- **ShippoService** - интегрируется с Shippo API
- **OrderStatusUpdateEvent** - публикует события обновления статуса

## Настройка

### 1. Переменные окружения

Добавьте в `application.yml`:

```yaml
aws:
  region: us-east-1
  account-id: YOUR_ACCOUNT_ID
  sns:
    order-status-updated-topic-name: OrderStatusUpdated
```

### 2. SNS топики

Убедитесь, что созданы следующие SNS топики:
- `OrderUnconfirmed` - для отправки заказов в лямбду
- `OrderStatusUpdated` - для получения обновлений статуса от лямбды

### 3. Webhook URL'ы

Настройте webhook'и в Shippo:
- URL: `https://your-domain.com/api/delivery/shippo-webhook`
- События: `track_updated`, `transaction_created`

### 4. SQS интеграция

Монолит автоматически опрашивает SQS очередь `OrderStatusUpdateQueue` каждые 5 секунд.
- При ошибках обработки сообщения отправляются в DLQ после 3 попыток
- DLQ мониторится и логируется для ручной обработки

## API Endpoints

### Получение информации о доставке

```
GET /shipping/{orderId}
GET /shipping/tracking/{trackingNumber}
```

### Webhook'и

```
POST /api/delivery/shippo-webhook   # Shippo webhook
```

### Admin API для управления DLQ

```
GET /admin/dlq/status               # Статус DLQ
GET /admin/dlq/count                # Количество сообщений в DLQ
POST /admin/dlq/reprocess/{id}      # Переобработка сообщения
POST /admin/dlq/requeue/{id}        # Возврат в основную очередь
```

## Поток работы

1. **Создание заказа**: Монолит создает заказ и запись о доставке со статусом `PENDING`
2. **Отправка в лямбду**: Монолит публикует событие в `OrderUnconfirmed`
3. **Обработка в лямбде**: Лямбда создает отправление в Shippo
4. **Обновление статуса**: Лямбда публикует событие в SNS топик `OrderStatusUpdated`
5. **SQS обработка**: SNS отправляет сообщение в SQS, монолит опрашивает очередь и обновляет статус
6. **Webhook от Shippo**: Shippo отправляет обновления статуса напрямую в монолит

## Статусы доставки

- `PENDING` - Ожидает обработки
- `PROCESSING` - В процессе
- `LABEL_CREATED` - Лейбл создан
- `SHIPPED` - Отправлено
- `IN_TRANSIT` - В пути
- `DELIVERED` - Доставлено
- `FAILED` - Ошибка
- `CANCELLED` - Отменено

## Тестирование

Запустите тесты:

```bash
./mvnw test -Dtest=*ShippoWebhookControllerTest
./mvnw test -Dtest=*OrderStatusUpdateListenerTest
./mvnw test -Dtest=*ShippingServiceTest
./mvnw test -Dtest=*ShippingControllerTest
./mvnw test -Dtest=*SqsIntegrationTest
```

## Мониторинг

Логируйте следующие события:
- Обработка SQS сообщений
- Сообщения в Dead Letter Queue
- Обработка Shippo webhook'ов
- Обновления статуса доставки
- Ошибки обработки

## Безопасность

- Используйте IAM роли для доступа к SQS
- Валидируйте данные от Shippo
- Используйте HTTPS для Shippo webhook'ов
- Ограничьте доступ к admin DLQ endpoints 