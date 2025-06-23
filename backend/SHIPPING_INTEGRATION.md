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
- **SnsWebhookController** - получает webhook'и от SNS
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

### 4. SNS подписка

Подпишите монолит на топик `OrderStatusUpdated`:
- Протокол: HTTPS
- Endpoint: `https://your-domain.com/api/delivery/webhook`

## API Endpoints

### Получение информации о доставке

```
GET /shipping/{orderId}
GET /shipping/tracking/{trackingNumber}
```

### Webhook'и

```
POST /api/delivery/webhook          # SNS webhook
POST /api/delivery/shippo-webhook   # Shippo webhook
```

## Поток работы

1. **Создание заказа**: Монолит создает заказ и запись о доставке со статусом `PENDING`
2. **Отправка в лямбду**: Монолит публикует событие в `OrderUnconfirmed`
3. **Обработка в лямбде**: Лямбда создает отправление в Shippo
4. **Обновление статуса**: Лямбда публикует событие в `OrderStatusUpdated`
5. **Получение в монолите**: Монолит обновляет статус доставки
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
./mvnw test -Dtest=*WebhookControllerTest
./mvnw test -Dtest=*OrderStatusUpdateListenerTest
./mvnw test -Dtest=*ShippingServiceTest
./mvnw test -Dtest=*ShippingControllerTest
```

## Мониторинг

Логируйте следующие события:
- Получение SNS webhook'ов
- Обработка Shippo webhook'ов
- Обновления статуса доставки
- Ошибки обработки

## Безопасность

- Проверяйте подпись SNS webhook'ов
- Валидируйте данные от Shippo
- Используйте HTTPS для всех webhook'ов
- Ограничьте доступ к webhook endpoints 