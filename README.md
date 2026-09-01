**# SmartShopping**

SmartShopping is a microservices-based e-commerce application built using Java, Spring Boot, Spring Cloud, MySQL, JWT, Apache Kafka, and Docker.

The application is divided into independent microservices for authentication, products, orders, payments, inventory, and notifications.

**---**

**## Architecture**

\`\`\`text

                         Client / Postman

                                |

                                v

                     +----------------------+

                     |     API Gateway      |

                     |        :8080         |

                     |    JWT Validation    |

                     +----------+-----------+

                                |

                                v

                     +----------------------+

                     |    Eureka Server     |

                     |        :8761         |

                     |   Service Discovery  |

                     +----------+-----------+

                                |

              +-----------------+------------------+

              |                 |                  |

              v                 v                  v

       Auth Service      Product Service      Order Service

          :8081                :8082                 |

              |                   |                  |

              v                   v                  |

            MySQL               MySQL                |

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

                         |                           |                         |

                         v                           v                         v

                  Payment Service             Inventory Service       Notification Service

                         |

                         | payment-processed

                         v

                       Kafka

                         |

                         v

                  Notification Service

Microservices

Service  Responsibility Port

Service Registry  Eureka service discovery   8761

API Gateway Central entry point, routing and JWT validation 8080

Auth Service   User registration, login and JWT generation  8081

Product Service   Product CRUD operations 8082

Order Service  Order management and Kafka event publishing  -

Payment Service   Payment processing and Kafka event publishing   -

Inventory Service Inventory event processing -

Notification Service Order and payment notification processing -

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

1\. Service Registry

The Service Registry is implemented using Netflix Eureka.

Eureka runs on:

http\://localhost:8761

The microservices register themselves with Eureka.

Example configuration:

eureka.client.service-url.defaultZone=http\://localhost:8761/eureka/

eureka.client.register-with-eureka=true

eureka.client.fetch-registry=true

Eureka allows services to discover each other without directly hardcoding service host and port information.

2\. API Gateway

The API Gateway runs on:

http\://localhost:8080

Spring Cloud Gateway is used as the centralized entry point for client requests.

The Gateway performs:

Request routing

Eureka-based service discovery

JWT validation

Authentication filtering

Gateway Routes

/auth/\*\*           -> AUTH-SERVICE

/products/\*\*       -> PRODUCT-SERVICE

/orders/\*\*         -> ORDER-SERVICE

/payments/\*\*       -> PAYMENT-SERVICE

/inventory/\*\*      -> INVENTORY-SERVICE

/notifications/\*\*  -> NOTIFICATION-SERVICE

Example Gateway configuration:

spring.cloud.gateway.server.webflux.routes[0].id=product-service

spring.cloud.gateway.server.webflux.routes[0].uri=lb://PRODUCT-SERVICE

spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/products/\*\*

The lb:// URI allows the Gateway to locate the service through Eureka service discovery and load balancing.

3\. Authentication Service

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

auth\_db

User Registration

API:

POST /auth/register

Example request:

{

  "username": "dibya123",

  "email": "dibya123\@gmail.com",

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

4\. JWT Authentication

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

  | Authorization: Bearer \<JWT>

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

5\. Product Service

The Product Service manages products.

Port:

8082

Database:

product\_db

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

        extends JpaRepository\<Product, Long> {

}

6\. Order Service

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

7\. Kafka Integration

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

8\. Order Created Event Flow

When an order is created:

Order Service

     |

     | OrderCreatedEvent

     v

   Kafka

     |

     +-----------------------+-----------------------+

     |                       |                       |

     v                       v                       v

Payment Service       Inventory Service      Notification Service

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

9\. Payment Event Flow

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

10\. Inventory Service

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

11\. Notification Service

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

12\. Kafka Retry and Dead Letter Topic

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

13\. Duplicate Event Handling

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

14\. REST Communication

The application also uses synchronous REST communication where required.

The Order Service uses RestTemplate to communicate with Product Service.

Example:

restTemplate.getForObject(

    "http\://PRODUCT-SERVICE/products/" + order.getProductId(),

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

     |          |          |

     v          v          v

 Payment    Inventory   Notification

15\. MySQL Configuration

The application uses MySQL for persistence.

Example databases:

auth\_db

product\_db

order\_db

payment\_db

Database passwords are externalized using environment variables.

Example:

spring.datasource.password=${DB\_PASSWORD}

The actual database password should not be stored in GitHub.

16\. Environment Variables

Sensitive configuration is kept outside the source code.

Examples:

DB\_PASSWORD

JWT\_SECRET

Application configuration uses:

spring.datasource.password=${DB\_PASSWORD}

and:

jwt.secret=${JWT\_SECRET}

This prevents sensitive credentials from being committed to the repository.

17\. Docker and Docker Compose

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

smartshopping\_kafka-data

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

18\. Complete Request Flow

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

   | Authorization: Bearer \<JWT>

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

   |                   |                   |

   v                   v                   v

Payment             Inventory          Notification

Service              Service             Service

   |

   | PaymentProcessedEvent

   v

Kafka

   |

   v

Notification Service

19\. End-to-End Order Processing

The complete tested flow is:

1\. User logs in

        |

        v

2\. JWT is generated

        |

        v

3\. JWT is sent with protected request

        |

        v

4\. Gateway validates JWT

        |

        v

5\. Order request reaches Order Service

        |

        v

6\. Order Service gets product details

   from Product Service using REST

        |

        v

7\. Order is saved

        |

        v

8\. OrderCreatedEvent is published to Kafka

        |

        +-------------------+-------------------+

        |                   |                   |

        v                   v                   v

9\. Payment Service    Inventory Service   Notification Service

        |

        v

10\. Payment is created

        |

        v

11\. PaymentProcessedEvent is published

        |

        v

12\. Notification Service consumes event

        |

        v

13\. Payment success notification is processed

This complete end-to-end flow has been tested successfully.

20\. How to Run the Project

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

http\://localhost:8761

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

21\. Testing With Postman

Recommended testing sequence:

1\. Register User

       |

       v

2\. Login

       |

       v

3\. Receive JWT

       |

       v

4\. Add JWT to Authorization header

       |

       v

5\. Test Product APIs

       |

       v

6\. Create Order

       |

       v

7\. Verify Payment Event

       |

       v

8\. Verify Inventory Event

       |

       v

9\. Verify Notification Event

Authorization header:

Authorization: Bearer \<JWT\_TOKEN>

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

22\. Project Structure

SmartShopping

│

├── README.md

├── .gitignore

├── docker-compose.yml

│

├── api-gateway

│   └── src

│

├── auth-service

│   └── src

│

├── inventory-service

│   └── src

│

├── notification-service

│   └── src

│

├── order-service

│   └── src

│

├── payment-service

│   └── src

│

├── product-service

│   └── src

│

└── service-registry

    └── src

23\. Key Microservices Concepts Demonstrated

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

24\. Git Workflow

The project is maintained using Git and GitHub.

Basic workflow:

git status

Add changes:

git add .

Commit changes:

git commit -m "Your commit message"

Push changes:

git push

25\. Security

Never commit sensitive information such as:

Database passwords

JWT secrets

API keys

Personal Access Tokens

Use environment variables instead.

Example:

spring.datasource.password=${DB\_PASSWORD}

jwt.secret=${JWT\_SECRET}

Sensitive configuration should never be committed to GitHub.

26\. Project Highlights

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

## Final Implementation Checklist

The following hardening items were completed and tested during the project work:

- Eureka service discovery
- API Gateway routing with `lb://` services
- JWT authentication and role-based authorization
- Refresh/logout endpoints exercised through Postman
- Order -> Product synchronous REST communication
- Order -> Payment/Inventory/Notification asynchronous Kafka communication
- Kafka consumer groups
- Kafka Retryable Topics and DLT
- Duplicate-event/idempotent handling
- Payment failure -> Order `PAYMENT_FAILED` -> Inventory stock release
- Insufficient stock -> `inventory-failed` -> Order `FAILED`
- Resilience4j Product Service circuit breaker
- Global exception handling
- Correlation ID propagation over REST and Kafka
- MDC-based log correlation
- SLF4J logging
- Actuator health/info/metrics
- No remaining `System.out.println` / `System.err.println` in the application search

### Interview sequence

```text
Client
  -> API Gateway
  -> JWT authentication / authorization
  -> Eureka discovery
  -> Order Service
  -> REST Product Service
  -> Save Order
  -> Kafka order-created
  -> Payment + Inventory + Notification
  -> payment-processed
  -> Order status update
  -> payment failure compensation when needed
  -> Inventory stock release
  -> Correlation ID + SLF4J logs
  -> Actuator health / metrics
```

Conclusion

SmartShopping is a practical microservices project demonstrating how multiple Spring Boot services can work together using Eureka for service discovery, API Gateway for centralized routing and security, JWT for authentication, MySQL for persistence, RestTemplate for synchronous communication, Apache Kafka for asynchronous event-driven communication, and Docker Compose for Kafka infrastructure.

The complete order, payment, inventory, and notification event flow has been tested successfully, including failure handling, duplicate-event handling, correlation-ID propagation, circuit-breaker fallback, centralized exception handling, structured logging, and Actuator health/metrics.
# Detailed Implementation Guide

This section explains each implemented part step by step, including **why we use it, which dependency/configuration is required, how the request flows, and what to say in an interview**.

## A. Project Setup and Dependency Management

### Step 1: Create each service as an independent Spring Boot application

Services:
- service-registry
- api-gateway
- auth-service
- product-service
- order-service
- payment-service
- inventory-service
- notification-service

Each service has its own `pom.xml`, source code, configuration, and database responsibility.

### Step 2: Common dependency categories

Persistence:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
Used for JPA repositories, Hibernate, entities, and database persistence.

Web:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```
Used for REST controllers and HTTP APIs.

Validation:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
Used for request validation.

Eureka:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
Used for registration and discovery.

Kafka:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>
```
Used for producers and consumers.

Load balancer:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```
Used with `lb://SERVICE-NAME` and `@LoadBalanced` clients.

Resilience4j:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```
Used for the Product Service circuit breaker.

Actuator:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
Used for health, info, and metrics.

---

## B. Eureka Service Discovery - Deep Flow

### Why?

Without discovery, Order Service could be hard-coded to a URL such as `http://localhost:8082`. That becomes difficult when an instance moves or multiple instances exist.

### Configuration

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

### Step-by-step

1. Eureka Server starts on `8761`.
2. Each Eureka client starts.
3. Each client registers its service name and network details.
4. Eureka maintains the registry.
5. A caller asks for a logical service such as `PRODUCT-SERVICE`.
6. Spring Cloud resolves an available instance.
7. The caller sends the request to the resolved instance.

### Interview answer

> Eureka is a service registry. It removes the need for hard-coded service locations and allows services to discover other service instances dynamically.

---

## C. API Gateway - Deep Flow

### Why?

The Gateway gives the client one public endpoint instead of exposing every internal service port.

### Main properties

```properties
spring.application.name=api-gateway
server.port=8080
```

Example route:

```properties
spring.cloud.gateway.server.webflux.routes[0].id=product-service
spring.cloud.gateway.server.webflux.routes[0].uri=lb://PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/products/**
```

### Step-by-step

1. Client calls `localhost:8080`.
2. Gateway checks the request path.
3. Example `/orders/**` matches the order route.
4. `lb://ORDER-SERVICE` asks service discovery/load balancing for an available Order instance.
5. Gateway forwards the request.
6. The client does not need to know the internal port.

### Interview answer

> The API Gateway is the single entry point. It centralizes routing and is a suitable place for authentication, correlation IDs, rate limiting, and other cross-cutting concerns.

---

## D. Authentication - JWT - Deep Flow

### Authentication vs authorization

Authentication asks: **Who are you?**

Authorization asks: **What are you allowed to do?**

### Registration

```text
POST /auth/register
        |
        v
Validate request
        |
        v
BCrypt password hashing
        |
        v
Save user
```

The password is hashed with:

```java
passwordEncoder.encode(password)
```

The raw password should never be stored.

### Login

```text
POST /auth/login
        |
        v
Find user
        |
        v
Validate password
        |
        v
Generate JWT
        |
        v
Return token
```

### Protected request

```text
Client
  |
  | Authorization: Bearer <JWT>
  v
Security Filter Chain
  |
  v
JwtAuthenticationFilter
  |
  | Validate token + establish user/role
  v
Authorization rules
  |
  v
Controller
```

### Stateless session

```java
SessionCreationPolicy.STATELESS
```

means authentication is based on tokens rather than a server-side login session.

### Security rules implemented

```java
.requestMatchers(
    "/auth/register",
    "/auth/login",
    "/auth/refresh",
    "/auth/logout"
).permitAll()

.requestMatchers(HttpMethod.GET, "/products/**")
.hasAnyRole("USER", "ADMIN")

.requestMatchers(HttpMethod.POST, "/products/**")
.hasRole("ADMIN")

.requestMatchers(HttpMethod.PUT, "/products/**")
.hasRole("ADMIN")

.requestMatchers(HttpMethod.DELETE, "/products/**")
.hasRole("ADMIN")

.requestMatchers("/orders/**")
.hasAnyRole("USER", "ADMIN")

.anyRequest().authenticated()
```

### Authorization matrix

| Operation | USER | ADMIN |
|---|---:|---:|
| GET products | Yes | Yes |
| POST products | No | Yes |
| PUT products | No | Yes |
| DELETE products | No | Yes |
| Orders | Yes | Yes |

### Refresh token and logout

`/auth/refresh` and `/auth/logout` exist and were exercised in Postman. The exact storage/rotation/revocation behavior should be explained only from the Auth Service implementation.

### Interview answer

> We use stateless JWT authentication. The Auth Service authenticates the user and issues a token. The JWT filter validates the token on protected requests and establishes the user's identity and role. Spring Security then performs authorization using endpoint and role-based rules.

---

## E. Product Service - Deep Flow

### Responsibility

Product Service owns product data and exposes CRUD APIs.

### APIs

```text
POST   /products
GET    /products
GET    /products/{id}
PUT    /products/{id}
DELETE /products/{id}
```

### Database flow

```text
Controller
   |
   v
ProductService
   |
   v
ProductRepository
   |
   v
MySQL
```

### Why keep it separate?

The product domain has its own lifecycle and can scale independently from orders/payments.

---

## F. Order Service - Deep Flow

Order Service is the central business workflow service.

### Create order algorithm

1. Receive order request.
2. Call Product Service using `RestTemplate`.
3. Get product price.
4. Calculate `price * quantity`.
5. Set order status to `CREATED`.
6. Save order.
7. Build `OrderCreatedEvent`.
8. Generate a unique `eventId`.
9. Publish `order-created`.
10. Return saved order.

Example:

```java
double totalPrice =
    product.getPrice() * order.getQuantity();
```

### Why call Product Service before saving?

The Order Service needs the authoritative product price to calculate the order total and also verifies that the product exists.

---

## G. RestTemplate + Load Balancing - Deep Flow

Configuration:

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

Call:

```java
restTemplate.getForObject(
    "http://PRODUCT-SERVICE/products/" + productId,
    ProductResponse.class
);
```

### Step-by-step

1. Order Service asks for `PRODUCT-SERVICE`.
2. Eureka provides registered instances.
3. Spring Cloud LoadBalancer chooses an instance.
4. RestTemplate sends the HTTP request.
5. Product response is returned.

---

## H. Kafka - Deep Flow

### Why Kafka?

Kafka is used when services do not need a synchronous response from the consumer and we want loose coupling.

### Main topics

```text
order-created
payment-processed
inventory-failed
order-payment-failed
```

### Consumer groups

```text
order-created
   |
   +--> payment-group
   +--> inventory-group
   +--> notification-group
```

A different consumer group receives the event independently.

### Interview answer

> Kafka provides asynchronous, event-driven communication. It decouples the producer from consumers and allows multiple independent consumer groups to react to the same business event.

---

## I. OrderCreatedEvent - Deep Flow

Event fields:

```text
eventId
orderId
productId
quantity
totalPrice
```

Creation:

```java
OrderCreatedEvent event =
    new OrderCreatedEvent(
        savedOrder.getId(),
        savedOrder.getProductId(),
        savedOrder.getQuantity(),
        savedOrder.getTotalPrice());

event.setEventId(UUID.randomUUID().toString());
```

Then:

```text
Order Service
      |
      v
order-created
      |
      +--> Payment Service
      +--> Inventory Service
      +--> Notification Service
```

---

## J. Payment Service - Deep Flow

Payment Service has two important paths.

### HTTP payment creation

```text
POST /payments
      |
      v
Payment Service
      |
      v
Save Payment
      |
      v
Create PaymentProcessedEvent
      |
      v
Publish payment-processed
```

The service reads the current correlation ID from MDC and passes it explicitly to the Kafka producer.

### Event consumer

Payment Service also consumes `order-created` with its own consumer group. Before creating a payment, it checks whether a payment for the same order already exists.

This is an idempotency safeguard.

---

## K. Inventory Service - Deep Flow

### Reserve

```text
order-created
     |
     v
Inventory Service
     |
     v
find inventory by productId
     |
     +---- enough stock ----> subtract quantity
     |
     +---- not enough ------> InventoryFailedEvent
```

### Release

```text
order-payment-failed
        |
        v
Inventory Service
        |
        v
releaseStock(productId, quantity)
        |
        v
availableQuantity += quantity
```

### Why release?

If payment fails after stock was reserved, the reserved quantity must be made available again. This is a compensation step.

---

## L. Payment Failure Compensation - Deep Flow

```text
Payment Service
      |
      | payment-processed = FAILED
      v
Order Service
      |
      | status -> PAYMENT_FAILED
      v
order-payment-failed
      |
      v
Inventory Service
      |
      | release stock
      v
Inventory restored
```

This design keeps payment and inventory loosely coupled while allowing the system to compensate for a failed payment.

---

## M. Insufficient Stock Failure - Deep Flow

```text
Order Service
      |
      | order-created
      v
Inventory Service
      |
      | reserveStock
      X
InsufficientStockException
      |
      v
InventoryFailedEvent
      |
      v
inventory-failed
      |
      v
Order Service
      |
      v
Order status = FAILED
```

This path was tested using a very large quantity and the logs showed the failure event was published successfully with correlation ID `test-123`.

---

## N. Retry and Dead Letter Topic - Deep Flow

Payment Service uses:

```java
@RetryableTopic(
    attempts = "3",
    dltTopicSuffix = ".DLT"
)
```

### What happens?

1. Kafka event arrives.
2. Listener tries to process it.
3. Processing fails.
4. Spring Kafka retry mechanism handles the retry flow.
5. After the configured attempts, the event is sent to the DLT.
6. `@DltHandler` handles the dead-letter event.

### Why?

Transient failures can recover. Permanent/poison messages should not repeatedly block normal processing.

---

## O. Idempotency - Deep Explanation

### Payment Service

Before creating payment for an order:

```text
findByOrderId(orderId)
```

If a payment exists:

```text
Duplicate payment -> ignore
```

### Notification Service

Stores processed event IDs:

```java
processedEventRepository.existsById(event.getEventId())
```

If the event already exists, it is ignored.

### Order Service

Checks current status before applying another state transition.

### Interview answer

> Kafka consumers should be idempotent because duplicate delivery can happen. We protect business side effects using unique business identifiers, event IDs, and current-state checks.

---

## P. Global Exception Handling - Deep Flow

The services use `@RestControllerAdvice`.

Examples:

```text
ProductNotFoundException      -> 404
PaymentNotFoundException      -> 404
OrderNotFoundException        -> 404
InsufficientStockException    -> 409
OrderCancellationException    -> 409
Validation failure            -> 400
Unexpected exception          -> 500
```

### Why?

Without global handling, each controller would repeat try/catch and response formatting. Central handling makes API errors consistent.

---

## Q. Resilience4j Circuit Breaker - Deep Flow

Order Service has:

```java
@CircuitBreaker(
    name = "productService",
    fallbackMethod = "productServiceFallback"
)
```

### Normal

```text
Order -> Circuit Breaker -> Product Service -> Response
```

### Failure

```text
Order -> Circuit Breaker -> Product Service unavailable
                 |
                 v
              Fallback
                 |
                 v
ProductServiceUnavailableException
```

### Why?

It prevents repeated remote failures from causing uncontrolled cascading failures and gives the caller a predictable response.

---

## R. Correlation ID - Deep Flow

The header is:

```text
X-Correlation-Id
```

Example:

```text
test-123
```

### HTTP

```text
Request header
      |
      v
CorrelationIdFilter
      |
      v
MDC
      |
      v
SLF4J log
```

### REST propagation

Order Service's RestTemplate interceptor gets the MDC value and sends:

```text
X-Correlation-Id: test-123
```

to Product Service.

### Kafka propagation

Producer:

```java
record.headers().add(
    "X-Correlation-Id",
    correlationId.getBytes(StandardCharsets.UTF_8));
```

Consumer:

```java
record.headers()
      .lastHeader("X-Correlation-Id");
```

The consumer then places the value back into MDC while processing the event.

### Why?

HTTP thread context does not automatically become Kafka listener thread context. Therefore the ID must be explicitly propagated in message headers.

### End-to-end example

```text
Gateway [test-123]
      |
      v
Order Service [test-123]
      |
      +---- Product Service [test-123]
      |
      +---- order-created [test-123]
                    |
                    +--> Payment Service [test-123]
                    |
                    +--> Inventory Service [test-123]
```

This was tested successfully.

---

## S. SLF4J Logging - Deep Explanation

Raw console printing was removed from the application source.

Instead of:

```java
System.out.println("Order created");
```

we use:

```java
log.info("Order created. Order ID: {}", orderId);
```

For errors:

```java
log.error("Payment event failed", exception);
```

### Why placeholders?

SLF4J placeholders keep message construction efficient and structured.

The final workspace search found no remaining `System.out.println` or `System.err.println` matches.

---

## T. Actuator - Deep Explanation

Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Properties:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

Endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
```

### Health

```http
GET /actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

### Metrics

Useful examples:

```text
http.server.requests
hikaricp.connections.active
jvm.memory.used
process.cpu.usage
spring.kafka.listener
spring.kafka.template
resilience4j.circuitbreaker.calls
resilience4j.circuitbreaker.failure.rate
```

### Why?

Actuator provides operational visibility without creating custom monitoring endpoints for every metric.

---

## U. Docker and Kafka Infrastructure - Deep Flow

Kafka is run using Docker Compose.

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

Kafka broker:

```text
localhost:9092
```

Stop without removing data:

```bash
docker compose stop
```

Avoid `docker compose down -v` unless volume deletion is intentional.

---

## V. End-to-End Happy Path

```text
1. Register user
2. Login
3. Receive JWT
4. Client sends Bearer JWT
5. Gateway receives request
6. Security authenticates/authorizes
7. Order Service receives order
8. Order calls Product Service via REST
9. Product returns details
10. Order calculates total
11. Order saved as CREATED
12. order-created published
13. Payment consumes order-created
14. Inventory consumes order-created
15. Notification consumes order-created
16. Payment creates payment result
17. payment-processed published
18. Order consumes payment result
19. Order becomes PAID when payment succeeds
20. Notification processes success notification
```

---

## W. End-to-End Payment Failure Path

```text
1. Order created
2. Inventory reserves stock
3. Payment fails
4. payment-processed(status=FAILED)
5. Order becomes PAYMENT_FAILED
6. order-payment-failed published
7. Inventory receives event
8. Inventory releases reserved quantity
9. Same correlation ID can be followed in all logs
```

---

## X. End-to-End Insufficient Stock Path

```text
1. Order created
2. Inventory receives order-created
3. Inventory checks available quantity
4. Quantity is insufficient
5. Inventory publishes inventory-failed
6. Order consumes inventory-failed
7. Order becomes FAILED
8. Same correlation ID can be followed across the failure flow
```

---

## Y. Exact Interview Order to Explain the Project

When the interviewer says **"Explain your project end to end"**, use this order:

```text
Project overview
   -> services
   -> Eureka
   -> Gateway
   -> JWT authentication
   -> role-based authorization
   -> Product Service
   -> Order Service
   -> REST communication
   -> Kafka
   -> consumer groups
   -> Payment
   -> Inventory
   -> Notification
   -> Retry + DLT
   -> Idempotency
   -> Circuit Breaker
   -> Exception handling
   -> Correlation ID + MDC
   -> SLF4J
   -> Actuator
   -> Docker
   -> Testing
```

This order is useful because it starts with architecture and then moves from security, communication, business processing, resilience, and observability.

---

## Z. What Was Actually Tested

The project work included successful verification of:

```text
[✓] Gateway routing
[✓] Eureka discovery
[✓] Product CRUD / lookup
[✓] Order creation
[✓] Payment processing
[✓] Inventory reservation
[✓] Inventory release after payment failure
[✓] Insufficient stock failure
[✓] Duplicate-event handling
[✓] Retry/DLT configuration
[✓] Circuit-breaker fallback when Product Service is unavailable
[✓] JWT/security API flow
[✓] Correlation ID across REST
[✓] Correlation ID across Kafka
[✓] SLF4J logging cleanup
[✓] Global exception handlers
[✓] Actuator health
[✓] Actuator metrics
```

---

## Final Interview Notes

Do not say:

- "Eureka is the load balancer." Say Eureka is service discovery; Spring Cloud LoadBalancer resolves instances for `lb://` calls.
- "We have exactly-once Kafka processing." Say consumers are designed to be idempotent.
- "MDC automatically travels through Kafka." Say the correlation ID is explicitly propagated through Kafka headers and then restored into MDC.
- "We implemented OpenTelemetry/Jaeger/Zipkin." Do not claim this unless those tools are actually configured.
- "Passwords are encrypted." Say passwords are hashed with BCrypt.

The strongest project story is: **secure request -> gateway -> discovery -> synchronous product lookup -> save order -> asynchronous events -> payment/inventory/notification -> failure compensation -> retries/DLT -> idempotency -> circuit breaker -> correlation-based logging -> health/metrics.**

