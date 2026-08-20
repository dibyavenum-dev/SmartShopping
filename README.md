# SmartShopping

SmartShopping is a microservices-based e-commerce application built using Java, Spring Boot, Spring Cloud, MySQL, JWT, Apache Kafka, and Docker.

The application is divided into independent microservices for authentication, products, orders, payments, inventory, and notifications.

---

## Architecture

```text
                         Client / Postman
                                |
                                v
                     +----------------------+
                     |     API Gateway      |
                     |        :8080         |
                     |    JWT Validation    |
                     +----------+-----------+
                                |
                                v
                     +----------------------+
                     |    Eureka Server     |
                     |        :8761         |
                     |   Service Discovery  |
                     +----------+-----------+
                                |
              +-----------------+------------------+
              |                 |                  |
              v                 v                  v
       Auth Service      Product Service      Order Service
          :8081                :8082                 |
              |                   |                  |
              v                   v                  |
            MySQL               MySQL                |
                                                     |
                                                     | REST
                                                     v
                                             Product Service
                                                     |
                                                     |
                                                     v
                                                   Kafka
                                                     |
                         +---------------------------+-------------------------+
                         |                           |                         |
                         v                           v                         v
                  Payment Service             Inventory Service       Notification Service
                         |
                         | payment-processed
                         v
                       Kafka
                         |
                         v
                  Notification Service
Microservices
Service	Responsibility	Port
Service Registry	Eureka service discovery	8761
API Gateway	Central entry point, routing and JWT validation	8080
Auth Service	User registration, login and JWT generation	8081
Product Service	Product CRUD operations	8082
Order Service	Order management and Kafka event publishing	-
Payment Service	Payment processing and Kafka event publishing	-
Inventory Service	Inventory event processing	-
Notification Service	Order and payment notification processing	-
Technologies Used
Java 21
Spring Boot
Spring Cloud
Spring Cloud Gateway
Netflix Eureka
Spring Security
JWT
BCrypt
Spring Data JPA
MySQL
Apache Kafka
Spring Kafka
RestTemplate
Docker
Docker Compose
Maven
Git
GitHub
Postman
1. Service Registry

The Service Registry is implemented using Netflix Eureka.

Eureka runs on:

http://localhost:8761

The microservices register themselves with Eureka.

Example configuration:

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

Eureka allows services to discover each other without directly hardcoding service host and port information.

2. API Gateway

The API Gateway runs on:

http://localhost:8080

Spring Cloud Gateway is used as the centralized entry point for client requests.

The Gateway performs:

Request routing
Eureka-based service discovery
JWT validation
Authentication filtering
Gateway Routes
/auth/**           -> AUTH-SERVICE
/products/**       -> PRODUCT-SERVICE
/orders/**         -> ORDER-SERVICE
/payments/**       -> PAYMENT-SERVICE
/inventory/**      -> INVENTORY-SERVICE
/notifications/**  -> NOTIFICATION-SERVICE

Example Gateway configuration:

spring.cloud.gateway.server.webflux.routes[0].id=product-service
spring.cloud.gateway.server.webflux.routes[0].uri=lb://PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/products/**

The lb:// URI allows the Gateway to locate the service through Eureka service discovery and load balancing.

3. Authentication Service

The Auth Service is responsible for:

User registration
User login
Password encryption
Password validation
JWT generation
Authentication

Port:

8081

Database:

auth_db
User Registration

API:

POST /auth/register

Example request:

{
  "username": "dibya123",
  "email": "dibya123@gmail.com",
  "password": "1234"
}

The raw password is never stored directly.

The password is encrypted using BCrypt:

passwordEncoder.encode(password)

The encrypted password is stored in MySQL.

User Login

API:

POST /auth/login

Example request:

{
  "username": "dibya123",
  "password": "1234"
}

The Auth Service:

Finds the user.
Validates the password using BCrypt.
Generates a JWT.
Returns the JWT to the client.
4. JWT Authentication

JWT is used to secure the protected microservices.

Authentication Flow
Register
   |
   v
Password encrypted using BCrypt
   |
   v
MySQL
Login Flow
Client
  |
  | POST /auth/login
  v
API Gateway
  |
  v
Auth Service
  |
  v
Validate Username + Password
  |
  v
Generate JWT
  |
  v
Return JWT
Protected API Flow
Client
  |
  | Authorization: Bearer <JWT>
  v
API Gateway
  |
  v
JWT Validation
  |
  +---- Invalid/No Token ----> 401 Unauthorized
  |
  v
Eureka
  |
  v
Target Microservice

The following APIs are accessible without an existing JWT:

POST /auth/register
POST /auth/login

Protected APIs require a valid JWT.

5. Product Service

The Product Service manages products.

Port:

8082

Database:

product_db

Base path:

/products
APIs
Create Product
POST /products
Get All Products
GET /products
Get Product By ID
GET /products/{id}
Update Product
PUT /products/{id}
Delete Product
DELETE /products/{id}
Product Fields
id
name
description
price
quantity
category

The Product Service uses Spring Data JPA.

Example:

public interface ProductRepository
        extends JpaRepository<Product, Long> {
}
6. Order Service

The Order Service is responsible for order management.

It contains:

Controller
Service
Repository
Entity
DTO
Producer
Event
Kafka configuration
RestTemplate configuration

The Order Service uses both:

Synchronous REST communication
Asynchronous Kafka event communication
Order Creation Flow

When an order is created:

Order Service gets product information from Product Service using REST.
The total price is calculated.
The order is saved in the database.
An OrderCreatedEvent is created.
A unique event ID is generated.
The event is published to Kafka.

Example event:

OrderCreatedEvent


eventId
orderId
productId
quantity
totalPrice
7. Kafka Integration

Apache Kafka is used for asynchronous event communication between microservices.

Kafka is running using Docker Compose.

Kafka image:

apache/kafka:4.0.0

Kafka ports:

9092 - Kafka broker
9093 - Kafka controller
Kafka Topics

The application uses topics including:

order-created
payment-processed

Retry and Dead Letter topics are also created for retry-enabled consumers.

Examples:

order-created-retry
order-created.DLT
8. Order Created Event Flow

When an order is created:

Order Service
     |
     | OrderCreatedEvent
     v
   Kafka
     |
     +-----------------------+-----------------------+
     |                       |                       |
     v                       v                       v
Payment Service       Inventory Service      Notification Service

The same order-created event can be consumed independently by multiple consumer groups.

Payment Service

Payment Service consumes:

order-created

using:

payment-group

It checks whether a payment already exists for the order.

If the payment does not already exist:

Payment created
     |
     v
Payment status = SUCCESS
     |
     v
PaymentProcessedEvent
     |
     v
Kafka
9. Payment Event Flow

Payment Service publishes:

payment-processed

The event contains:

eventId
paymentId
orderId
amount
status

Notification Service consumes the event using:

notification-payment-group

If the payment status is SUCCESS, the Notification Service processes the payment-success notification.

Flow:

Payment Service
      |
      | PaymentProcessedEvent
      v
    Kafka
      |
      v
Notification Service
      |
      v
Payment Successful Notification
10. Inventory Service

The Inventory Service consumes the:

order-created

Kafka topic.

It uses a separate consumer group:

inventory-group

The Inventory Service receives:

orderId
productId
quantity
totalPrice

This demonstrates how multiple independent services can consume the same Kafka event.

11. Notification Service

The Notification Service consumes events from Kafka.

It has two event consumers.

Order Created Consumer

Consumes:

order-created

using:

notification-group

It checks whether the event was already processed.

The processed event ID is stored so duplicate events can be ignored.

Example:

Event already processed
        |
        v
Duplicate event ignored
Payment Notification Consumer

Consumes:

payment-processed

using:

notification-payment-group

When payment status is SUCCESS, it processes the payment-success notification.

12. Kafka Retry and Dead Letter Topic

The Payment Service uses Kafka retry handling.

Example:

@RetryableTopic(
    attempts = "3",
    dltTopicSuffix = ".DLT"
)

If processing fails, Kafka retries the message before sending it to the Dead Letter Topic.

Flow:

Kafka Event
    |
    v
Consumer
    |
    X Processing Failed
    |
    v
Retry
    |
    X Failed Again
    |
    v
Retry
    |
    X Failed Again
    |
    v
DLT

The Payment Service has a DLT handler for failed OrderCreatedEvent messages.

The Notification Service order-event consumer also uses retry and DLT handling.

13. Duplicate Event Handling

Kafka consumers can potentially receive the same event more than once.

The application demonstrates idempotent processing.

Payment Service

Before creating a payment, it checks:

orderId

If a payment already exists for that order, the event is ignored.

Notification Service

The Notification Service stores processed event IDs.

Before processing:

existsById(event.getEventId())

If the event already exists:

Duplicate event ignored

This prevents duplicate processing.

14. REST Communication

The application also uses synchronous REST communication where required.

The Order Service uses RestTemplate to communicate with Product Service.

Example:

restTemplate.getForObject(
    "http://PRODUCT-SERVICE/products/" + order.getProductId(),
    ProductResponse.class
);

The service name:

PRODUCT-SERVICE

is resolved through Eureka.

Therefore, the application demonstrates both communication approaches.

Synchronous Communication
Order Service
     |
     | REST / RestTemplate
     v
Product Service
Asynchronous Communication
Order Service
     |
     | Event
     v
Kafka
     |
     +----------+----------+
     |          |          |
     v          v          v
 Payment    Inventory   Notification
15. MySQL Configuration

The application uses MySQL for persistence.

Example databases:

auth_db
product_db
order_db
payment_db

Database passwords are externalized using environment variables.

Example:

spring.datasource.password=${DB_PASSWORD}

The actual database password should not be stored in GitHub.

16. Environment Variables

Sensitive configuration is kept outside the source code.

Examples:

DB_PASSWORD
JWT_SECRET

Application configuration uses:

spring.datasource.password=${DB_PASSWORD}

and:

jwt.secret=${JWT_SECRET}

This prevents sensitive credentials from being committed to the repository.

17. Docker and Docker Compose

Docker is used to run Kafka for the SmartShopping application.

The Docker Compose file is located at:

D:\SmartShopping\docker-compose.yml

Kafka is started using:

docker compose up -d

Kafka container:

kafka

Kafka image:

apache/kafka:4.0.0

Ports:

9092
9093

Docker Compose creates and manages the Kafka data volume:

smartshopping_kafka-data
Verify Kafka

Check running containers:

docker ps

Check Kafka logs:

docker logs kafka

Stop the SmartShopping Kafka container:

docker compose stop

Start it again:

docker compose up -d

Do not use:

docker compose down -v

unless you intentionally want to remove Compose-managed volumes.

18. Complete Request Flow
Public Authentication Flow
Postman
   |
   | POST /auth/login
   v
API Gateway :8080
   |
   v
Eureka
   |
   v
Auth Service :8081
   |
   v
MySQL
   |
   v
JWT
   |
   v
Postman
Protected API Flow
Postman
   |
   | Authorization: Bearer <JWT>
   v
API Gateway :8080
   |
   | JWT Validation
   v
Eureka
   |
   v
Target Microservice
Complete Order Flow
Client
   |
   v
API Gateway
   |
   v
Order Service
   |
   | REST
   v
Product Service
   |
   | Product details
   v
Order Service
   |
   | Save Order
   |
   | OrderCreatedEvent
   v
Kafka
   |
   +-------------------+-------------------+
   |                   |                   |
   v                   v                   v
Payment             Inventory          Notification
Service              Service             Service
   |
   | PaymentProcessedEvent
   v
Kafka
   |
   v
Notification Service
19. End-to-End Order Processing

The complete tested flow is:

1. User logs in
        |
        v
2. JWT is generated
        |
        v
3. JWT is sent with protected request
        |
        v
4. Gateway validates JWT
        |
        v
5. Order request reaches Order Service
        |
        v
6. Order Service gets product details
   from Product Service using REST
        |
        v
7. Order is saved
        |
        v
8. OrderCreatedEvent is published to Kafka
        |
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
9. Payment Service    Inventory Service   Notification Service
        |
        v
10. Payment is created
        |
        v
11. PaymentProcessedEvent is published
        |
        v
12. Notification Service consumes event
        |
        v
13. Payment success notification is processed

This complete end-to-end flow has been tested successfully.

20. How to Run the Project
Step 1 - Start MySQL

Make sure MySQL is running and the required databases are available.

Step 2 - Start Kafka

Go to the SmartShopping root directory:

cd /d D:\SmartShopping

Start Kafka:

docker compose up -d

Verify:

docker ps

Kafka should be running on:

localhost:9092
Step 3 - Start Service Registry

Start:

service-registry

Verify Eureka:

http://localhost:8761
Step 4 - Start Auth Service

Start:

auth-service

Port:

8081
Step 5 - Start Product Service

Start:

product-service

Port:

8082
Step 6 - Start Other Microservices

Start:

order-service
payment-service
inventory-service
notification-service
Step 7 - Start API Gateway

Start:

api-gateway

Port:

8080
21. Testing With Postman

Recommended testing sequence:

1. Register User
       |
       v
2. Login
       |
       v
3. Receive JWT
       |
       v
4. Add JWT to Authorization header
       |
       v
5. Test Product APIs
       |
       v
6. Create Order
       |
       v
7. Verify Payment Event
       |
       v
8. Verify Inventory Event
       |
       v
9. Verify Notification Event

Authorization header:

Authorization: Bearer <JWT_TOKEN>
Security Test

Without token:

Request
   |
   v
Gateway
   |
   v
401 Unauthorized

With a valid token:

Request
   |
   v
Gateway
   |
   v
JWT Validation
   |
   v
Microservice
   |
   v
Successful Response
22. Project Structure
SmartShopping
│
├── README.md
├── .gitignore
├── docker-compose.yml
│
├── api-gateway
│   └── src
│
├── auth-service
│   └── src
│
├── inventory-service
│   └── src
│
├── notification-service
│   └── src
│
├── order-service
│   └── src
│
├── payment-service
│   └── src
│
├── product-service
│   └── src
│
└── service-registry
    └── src
23. Key Microservices Concepts Demonstrated

This project demonstrates:

Microservices Architecture
Service Discovery using Eureka
API Gateway
JWT Authentication
Spring Security
BCrypt Password Hashing
REST APIs
RestTemplate
Apache Kafka
Event-driven communication
Kafka Consumer Groups
Kafka Retry
Dead Letter Topics
Idempotent Event Processing
Spring Data JPA
MySQL
Docker
Docker Compose
Environment-based configuration
Maven
Git
GitHub
24. Git Workflow

The project is maintained using Git and GitHub.

Basic workflow:

git status

Add changes:

git add .

Commit changes:

git commit -m "Your commit message"

Push changes:

git push
25. Security

Never commit sensitive information such as:

Database passwords
JWT secrets
API keys
Personal Access Tokens

Use environment variables instead.

Example:

spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}

Sensitive configuration should never be committed to GitHub.

26. Project Highlights

SmartShopping demonstrates a complete microservices-based e-commerce workflow using both synchronous and asynchronous communication.

The project includes:

Java
   |
Spring Boot
   |
Spring Cloud
   |
Eureka
   |
API Gateway
   |
JWT + Spring Security
   |
MySQL
   |
Kafka
   |
Docker

The project was tested end-to-end with:

Authentication
      ↓
JWT
      ↓
API Gateway
      ↓
Order Service
      ↓
Product Service
      ↓
Kafka
      ↓
Payment Service
      ↓
Inventory Service
      ↓
Notification Service
Conclusion

SmartShopping is a practical microservices project demonstrating how multiple Spring Boot services can work together using Eureka for service discovery, API Gateway for centralized routing and security, JWT for authentication, MySQL for persistence, RestTemplate for synchronous communication, Apache Kafka for asynchronous event-driven communication, and Docker Compose for Kafka infrastructure.

The complete order, payment, inventory, and notification event flow has been tested successfully.