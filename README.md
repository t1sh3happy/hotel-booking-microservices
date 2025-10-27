Hotel Booking Platform - Микросервисная система бронирования
Распределённое приложение для управления бронированиями номеров в отелях, построенное на архитектуре микросервисов с использованием Spring Boot и Spring Cloud.
📋 Описание проекта
Система состоит из четырёх микросервисов, обеспечивающих полный цикл бронирования: от регистрации пользователя до управления отелями и номерами. Реализована двухшаговая согласованность данных между сервисами с механизмами компенсации при сбоях.
🏗️ Архитектура
text
┌─────────────────┐
│   API Gateway   │  (Port 8080)
│  (Spring Cloud) │
└────────┬────────┘
         │
    ┌────┴─────────────────────┐
    │                          │
┌───▼──────────┐      ┌────▼──────────┐
│   Booking    │      │     Hotel     │
│   Service    │◄────►│   Service     │
│ (Random Port)│      │ (Random Port) │
└──────────────┘      └───────────────┘
         │                    │
         └──────────┬─────────┘
                    │
            ┌───────▼────────┐
            │ Eureka Server  │
            │   (Port 8761)  │
            └────────────────┘
Компоненты системы
1.	Eureka Server - Service Registry для динамического обнаружения сервисов
2.	API Gateway - Единая точка входа, маршрутизация запросов
3.	Booking Service - Управление бронированиями, аутентификация пользователей
4.	Hotel Service - Управление отелями и номерами, контроль доступности
🚀 Технологический стек
•	Java 17+
•	Spring Boot 3.x
•	Spring Cloud (Gateway, Eureka)
•	Spring Security + JWT
•	Spring Data JPA
•	H2 Database (in-memory)
•	WebFlux (reactive WebClient)
•	Swagger/OpenAPI 3.0
📦 Структура проекта
text
hotel-booking-platform/
├── eureka-server/          # Service Discovery
├── api-gateway/            # API Gateway
├── booking-service/        # Сервис бронирований
└── hotel-service/          # Сервис управления отелями
⚙️ Установка и запуск
Требования
•	JDK 17 или выше
•	Maven 3.8+
Сборка проекта
bash
# Сборка всех модулей
mvn clean install
Запуск сервисов
Важно! Запускайте сервисы в следующем порядке:
1. Eureka Server
bash
cd eureka-server
mvn spring-boot:run
Проверка: http://localhost:8761
2. Hotel Service
bash
cd hotel-service
mvn spring-boot:run
3. Booking Service
bash
cd booking-service
mvn spring-boot:run
4. API Gateway
bash
cd api-gateway
mvn spring-boot:run
Проверка: http://localhost:8080
Swagger документация
После запуска всех сервисов документация API доступна по адресу:
•	API Gateway Swagger UI: http://localhost:8080/swagger-ui.html
🔐 Аутентификация
Система использует JWT токены для аутентификации. Срок действия токена - 1 час.
Регистрация пользователя
bash
curl -X POST http://localhost:8080/user/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "john_doe",
    "password": "password123",
    "admin": false
  }'
Ответ:
json
{
  "id": 1,
  "username": "john_doe",
  "role": "USER"
}
Вход в систему
bash
curl -X POST http://localhost:8080/user/auth \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
Ответ:
json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
Использование токена
Все последующие запросы должны содержать заголовок:
text
Authorization: Bearer <access_token>
📚 API Endpoints
Аутентификация
Метод	Путь	Описание	Доступ
POST	/user/register	Регистрация пользователя	Публичный
POST	/user/auth	Вход в систему	Публичный
Управление пользователями (Админ)
Метод	Путь	Описание	Доступ
POST	/user	Создать пользователя	ADMIN
PATCH	/user	Обновить пользователя	ADMIN
DELETE	/user?userId={id}	Удалить пользователя	ADMIN
GET	/user	Список всех пользователей	ADMIN
GET	/user/{id}	Получить пользователя	ADMIN
Бронирования
Метод	Путь	Описание	Доступ
POST	/bookings	Создать бронирование	USER/ADMIN
GET	/bookings	Мои бронирования	USER/ADMIN
GET	/booking/{id}	Получить бронирование	USER/ADMIN
DELETE	/booking/{id}	Отменить бронирование	USER/ADMIN
GET	/bookings/all	Все бронирования	ADMIN
Отели
Метод	Путь	Описание	Доступ
GET	/hotels	Список отелей	USER/ADMIN
POST	/hotels	Создать отель	ADMIN
PUT	/hotels/{id}	Обновить отель	ADMIN
DELETE	/hotels/{id}	Удалить отель	ADMIN
Номера
Метод	Путь	Описание	Доступ
GET	/rooms/{id}	Получить номер	USER/ADMIN
GET	/rooms/recommend	Рекомендованные номера	USER/ADMIN
POST	/rooms	Создать номер	ADMIN
PUT	/rooms/{id}	Обновить номер	ADMIN
DELETE	/rooms/{id}	Удалить номер	ADMIN
🎯 Примеры использования
Создание отеля (требуется админ)
bash
# Сначала создайте админа при регистрации с "admin": true
TOKEN="<ваш_токен_админа>"

curl -X POST http://localhost:8080/hotels \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Grand Hotel Moscow",
    "city": "Moscow",
    "address": "Red Square, 1"
  }'
Создание номера
bash
curl -X POST http://localhost:8080/rooms \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "number": "101",
    "capacity": 2,
    "available": true,
    "timesBooked": 0,
    "hotel": {
      "id": 1
    }
  }'
Получение рекомендованных номеров
bash
TOKEN="<ваш_токен>"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/rooms/recommend
Ответ: Номера отсортированы по загруженности (наименее загруженные первыми)
json
[
  {
    "id": 1,
    "number": "101",
    "capacity": 2,
    "timesBooked": 0,
    "available": true
  },
  {
    "id": 2,
    "number": "102",
    "capacity": 2,
    "timesBooked": 3,
    "available": true
  }
]
Создание бронирования (ручной выбор номера)
bash
curl -X POST http://localhost:8080/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "requestId": "req-'$(uuidgen)'",
    "roomId": 1,
    "startDate": "2025-11-01",
    "endDate": "2025-11-05",
    "autoSelect": false
  }'
Создание бронирования (автоподбор номера)
bash
curl -X POST http://localhost:8080/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "requestId": "req-'$(uuidgen)'",
    "startDate": "2025-11-01",
    "endDate": "2025-11-05",
    "autoSelect": true
  }'
Система автоматически выберет наименее загруженный номер!
Просмотр своих бронирований
bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/bookings
Отмена бронирования
bash
curl -X DELETE http://localhost:8080/booking/1 \
  -H "Authorization: Bearer $TOKEN"
🔄 Распределённые транзакции
Двухшаговая согласованность
Система реализует паттерн Saga для обеспечения согласованности данных:
1.	PENDING - Бронирование создано в Booking Service
2.	Hold - Временная блокировка номера в Hotel Service
3.	Confirm - Подтверждение блокировки
4.	CONFIRMED - Бронирование успешно завершено
Компенсация при сбоях
При ошибке на любом этапе выполняется компенсирующая транзакция:
1.	Release - Освобождение временной блокировки
2.	CANCELLED - Бронирование отменено
Идемпотентность
Каждый запрос содержит уникальный requestId, что позволяет безопасно повторять операции без создания дубликатов.
Retry и Timeout
•	Timeout: 5 секунд на запрос к Hotel Service
•	Retry: 3 попытки с экспоненциальной задержкой (300ms, 600ms, 1200ms)
•	Max backoff: 2 секунды
Сквозная корреляция
Все операции в рамках одного бронирования маркируются correlationId для трассировки в логах:
text
[uuid] Booking PENDING created
[uuid] Auto-selected room ID: 1
[uuid] Booking CONFIRMED
🗄️ Структура базы данных
Booking Service
users
•	id (PK)
•	username (UNIQUE)
•	passwordHash
•	role (USER/ADMIN)
bookings
•	id (PK)
•	userId (FK)
•	roomId
•	startDate
•	endDate
•	status (PENDING/CONFIRMED/CANCELLED)
•	requestId (UNIQUE)
•	correlationId
•	createdAt
Hotel Service
hotels
•	id (PK)
•	name
•	city
•	address
rooms
•	id (PK)
•	hotel_id (FK)
•	number
•	capacity
•	available (boolean)
•	timesBooked (счётчик для статистики)
room_reservation_locks
•	id (PK)
•	requestId (UNIQUE)
•	roomId
•	startDate
•	endDate
•	status (HELD/CONFIRMED/RELEASED)
🛡️ Безопасность
JWT Configuration
Токены подписываются симметричным ключом HMAC. Секретный ключ настраивается через свойство:
text
security:
  jwt:
    secret: your-secret-key-change-in-production
⚠️ Важно! Измените секрет перед деплоем в продакшн!
Разграничение доступа
•	USER - Может создавать и просматривать свои бронирования
•	ADMIN - Полный доступ ко всем операциям + управление пользователями и отелями
📊 Мониторинг
Eureka Dashboard
Статус всех зарегистрированных сервисов: http://localhost:8761
Actuator Endpoints
API Gateway предоставляет метрики через Spring Boot Actuator:
bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
🧪 Тестирование
Запуск тестов
bash
# Все тесты
mvn test

# Только один сервис
cd booking-service
mvn test
Типы тестов
•	Unit тесты - JUnit для бизнес-логики
•	Integration тесты - MockMvc для контроллеров
•	WebClient тесты - WireMock для внешних вызовов
🐛 Troubleshooting
Сервисы не регистрируются в Eureka
1.	Проверьте, что Eureka Server запущен первым
2.	Убедитесь, что порт 8761 не занят
3.	Проверьте логи сервиса на ошибки подключения
JWT токен недействителен
1.	Проверьте, что секретный ключ одинаковый во всех сервисах
2.	Убедитесь, что токен не истёк (1 час с момента выдачи)
3.	Проверьте формат заголовка: Authorization: Bearer <token>
Ошибка при создании бронирования
1.	Проверьте, что номер существует и доступен (available=true)
2.	Убедитесь, что даты не пересекаются с существующими бронированиями
3.	Проверьте корректность requestId (должен быть уникальным)
Gateway не маршрутизирует запросы
1.	Проверьте, что все сервисы зарегистрированы в Eureka
2.	Убедитесь, что пути в application.yml Gateway корректны
3.	Проверьте логи Gateway на ошибки маршрутизации
📝 Конфигурация
Порты по умолчанию
Сервис	Порт
Eureka Server	8761
API Gateway	8080
Hotel Service	Random (регистрируется в Eureka)
Booking Service	Random (регистрируется в Eureka)
Настройка Hotel Service Client
В booking-service/src/main/resources/application.yml:
text
hotel:
  base-url: http://hotel-service
  timeout-ms: 5000
  retries: 3
🚀 Деплой в продакшн
Контрольный список
•	  Измените JWT секрет на случайную строку длиной 32+ символа
•	  Замените H2 на PostgreSQL/MySQL
•	  Настройте внешний конфигурационный сервер (Spring Cloud Config)
•	  Добавьте централизованное логирование (ELK Stack)
•	  Настройте мониторинг (Prometheus + Grafana)
•	  Добавьте Circuit Breaker (Resilience4j)
•	  Настройте HTTPS для Gateway
•	  Ограничьте доступ к Actuator endpoints
👨💻 Автор
Создано в учебных целях для демонстрации микросервисной архитектуры.
📄 Лицензия
MIT License
________________________________________
Версия: 1.0.0
Дата обновления: Октябрь 2025


