\# SmartShopping



SmartShopping is a microservices-based e-commerce application built using Java, Spring Boot, Spring Cloud, MySQL, JWT and Apache Kafka.



The application is divided into independent microservices for authentication, products, orders, payments, inventory and notifications.



\---



\## Architecture



```text

&#x20;                        Client / Postman

&#x20;                               |

&#x20;                               v

&#x20;                   +-----------------------+

&#x20;                   |     API Gateway       |

&#x20;                   |       :8080           |

&#x20;                   |   JWT Authentication  |

&#x20;                   +-----------+-----------+

&#x20;                               |

&#x20;                               v

&#x20;                   +-----------------------+

&#x20;                   |    Eureka Server      |

&#x20;                   |       :8761           |

&#x20;                   |   Service Discovery   |

&#x20;                   +-----------+-----------+

&#x20;                               |

&#x20;         +---------------------+----------------------+

&#x20;         |                     |                      |

&#x20;         v                     v                      v

&#x20;  Auth Service          Product Service         Order Service

&#x20;     :8081                   :8082                     |

&#x20;         |                                             |

&#x20;         v                                             |

&#x20;      MySQL                                            |

&#x20;                                                       |

&#x20;                                                       v

&#x20;                                                    Kafka

&#x20;                                                       |

&#x20;                                     +-----------------+----------------+

&#x20;                                     |                                  |

&#x20;                                     v                                  v

&#x20;                             Payment Service                    Other Consumers

&#x20;                                     |

&#x20;                                     v

&#x20;                             Payment Event

&#x20;                                     |

&#x20;                                     v

&#x20;                                   Kafka

&#x20;                                     |

&#x20;                                     v

&#x20;                             Order Service



Microservices

Service	Responsibility	Port

Service Registry	Eureka service discovery	8761

API Gateway	Central entry point, routing and JWT validation	8080

Auth Service	User registration, login and JWT generation	8081

Product Service	Product CRUD operations	8082

Order Service	Order management and order events	-

Payment Service	Payment processing	-

Inventory Service	Inventory management	-

Notification Service	Notification processing	-

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

Maven

Git

GitHub

Postman

1\. Service Registry



The Service Registry is implemented using Netflix Eureka.



http://localhost:8761



The microservices register themselves with Eureka.



Example configuration:



eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

eureka.client.register-with-eureka=true

eureka.client.fetch-registry=true



Eureka allows services to discover each other without directly hardcoding service host and port information.



2\. API Gateway



The API Gateway runs on:



http://localhost:8080



Spring Cloud Gateway is used as the centralized entry point for client requests.



The Gateway performs:



Request routing

Eureka-based service discovery

JWT validation

Authentication filtering

Gateway Routes

/auth/\*\*           -> AUTH-SERVICE





/products/\*\*       -> PRODUCT-SERVICE





/orders/\*\*         -> ORDER-SERVICE





/payments/\*\*       -> PAYMENT-SERVICE





/inventory/\*\*      -> INVENTORY-SERVICE





/notifications/\*\*  -> NOTIFICATION-SERVICE



Example Gateway configuration:



spring.cloud.gateway.server.webflux.routes\[0].id=product-service

spring.cloud.gateway.server.webflux.routes\[0].uri=lb://PRODUCT-SERVICE

spring.cloud.gateway.server.webflux.routes\[0].predicates\[0]=Path=/products/\*\*



The lb:// URI allows the Gateway to locate the service through service discovery and load balancing.



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

&#x20;   "username": "dibya123",

&#x20;   "email": "dibya123@gmail.com",

&#x20;   "password": "1234"

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

&#x20;   "username": "dibya123",

&#x20;   "password": "1234"

}



The Auth Service:



Finds the user.

Validates the password using BCrypt.

Generates a JWT.

Returns the JWT to the client.

4\. JWT Authentication



JWT is used to secure the microservices.



The authentication flow is:



Register

&#x20;  |

&#x20;  v

Password encrypted using BCrypt

&#x20;  |

&#x20;  v

MySQL



Login:



Login Request

&#x20;    |

&#x20;    v

Auth Service

&#x20;    |

&#x20;    v

Validate Username + Password

&#x20;    |

&#x20;    v

Generate JWT

&#x20;    |

&#x20;    v

Return JWT



For protected APIs:



Client

&#x20;  |

&#x20;  | Authorization: Bearer <JWT>

&#x20;  v

API Gateway

&#x20;  |

&#x20;  v

JWT Validation

&#x20;  |

&#x20;  +---- Invalid/No Token ----> 401 Unauthorized

&#x20;  |

&#x20;  v

Eureka

&#x20;  |

&#x20;  v

Target Microservice



The following authentication APIs are accessible without an existing JWT:



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



The Product Service uses Spring Data JPA:



public interface ProductRepository

&#x20;       extends JpaRepository<Product, Long> {

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

Consumer

Event

Kafka Configuration

RestTemplate Configuration



The Order Service uses both:



REST communication

Kafka event-based communication

7\. Kafka Integration



Apache Kafka is used for asynchronous event communication.



The Order Service contains Kafka-related components such as:



KafkaProducerConfig

OrderEventProducer

PaymentEventConsumer

OrderCreatedEvent

PaymentProcessedEvent

Order Event Flow



When an order is created:



Order Service

&#x20;     |

&#x20;     | OrderCreatedEvent

&#x20;     v

&#x20;   Kafka

&#x20;     |

&#x20;     v

Payment Service



After payment processing:



Payment Service

&#x20;     |

&#x20;     | PaymentProcessedEvent

&#x20;     v

&#x20;   Kafka

&#x20;     |

&#x20;     v

Order Service



This allows services to communicate asynchronously through events.



8\. REST Communication



The application also uses synchronous REST communication where required.



The Order Service contains:



RestTemplateConfig



and uses RestTemplate for REST-based communication.



Therefore, the application demonstrates both communication approaches:



Synchronous:

Service A

&#x20;  |

&#x20;  | REST / RestTemplate

&#x20;  v

Service B



and:



Asynchronous:

Service A

&#x20;  |

&#x20;  | Event

&#x20;  v

&#x20;Kafka

&#x20;  |

&#x20;  v

Service B

9\. Payment Service



The Payment Service is responsible for payment processing.



It participates in the order/payment event flow using Kafka.



Basic flow:



Order Created

&#x20;    |

&#x20;    v

&#x20;  Kafka

&#x20;    |

&#x20;    v

Payment Service

&#x20;    |

&#x20;    v

Payment Processing

&#x20;    |

&#x20;    v

PaymentProcessedEvent

&#x20;    |

&#x20;    v

&#x20;  Kafka

10\. Inventory Service



The Inventory Service is responsible for inventory-related operations.



It is registered with Eureka and can be accessed through the API Gateway.



Gateway path:



/inventory/\*\*

11\. Notification Service



The Notification Service handles notification-related functionality.



It is registered with Eureka and can be accessed through the API Gateway.



Gateway path:



/notifications/\*\*

12\. MySQL Configuration



The application uses MySQL for persistence.



Example databases:



auth\_db

product\_db



Database passwords are externalized using environment variables.



Example:



spring.datasource.password=${DB\_PASSWORD}



The actual database password should not be stored in GitHub.



13\. Environment Variables



Sensitive configuration is kept outside the source code.



Example:



DB\_PASSWORD

JWT\_SECRET



Application configuration uses:



${DB\_PASSWORD}



and:



${JWT\_SECRET}



This prevents sensitive credentials from being committed to the repository.



14\. Complete Request Flow

Public Authentication Flow

Postman

&#x20;  |

&#x20;  | POST /auth/login

&#x20;  v

API Gateway :8080

&#x20;  |

&#x20;  v

Eureka

&#x20;  |

&#x20;  v

Auth Service :8081

&#x20;  |

&#x20;  v

MySQL

&#x20;  |

&#x20;  v

JWT

Protected API Flow

Postman

&#x20;  |

&#x20;  | Authorization: Bearer <JWT>

&#x20;  v

API Gateway :8080

&#x20;  |

&#x20;  | JWT Validation

&#x20;  v

Eureka

&#x20;  |

&#x20;  v

Target Microservice

Order and Payment Flow

Client

&#x20;  |

&#x20;  v

API Gateway

&#x20;  |

&#x20;  v

Order Service

&#x20;  |

&#x20;  | OrderCreatedEvent

&#x20;  v

&#x20;Kafka

&#x20;  |

&#x20;  v

Payment Service

&#x20;  |

&#x20;  | PaymentProcessedEvent

&#x20;  v

&#x20;Kafka

&#x20;  |

&#x20;  v

Order Service

15\. How to Run the Project



Start the application components in the following order.



Step 1 - Start MySQL



Make sure MySQL is running and the required databases are available.



Step 2 - Start Service Registry



Start:



service-registry



Verify Eureka:



http://localhost:8761

Step 3 - Start Auth Service



Start:



auth-service



Port:



8081

Step 4 - Start Product Service



Start:



product-service



Port:



8082

Step 5 - Start Other Microservices



Start:



order-service

payment-service

inventory-service

notification-service

Step 6 - Start API Gateway



Start:



api-gateway



Port:



8080

16\. Testing With Postman



Recommended testing sequence:



1\. Register User

&#x20;      |

&#x20;      v

2\. Login

&#x20;      |

&#x20;      v

3\. Receive JWT

&#x20;      |

&#x20;      v

4\. Add JWT to Authorization header

&#x20;      |

&#x20;      v

5\. Call Product / Order / Payment APIs



Example:



Authorization: Bearer <JWT\_TOKEN>

Security Test



Without token:



Request

&#x20;  |

&#x20;  v

Gateway

&#x20;  |

&#x20;  v

401 Unauthorized



With valid token:



Request

&#x20;  |

&#x20;  v

Gateway

&#x20;  |

&#x20;  v

JWT Validation

&#x20;  |

&#x20;  v

Microservice

&#x20;  |

&#x20;  v

Successful Response

17\. Project Structure

SmartShopping

│

├── README.md

├── .gitignore

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

&#x20;   └── src

18\. Key Microservices Concepts Demonstrated



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

Spring Data JPA

MySQL

Maven

Environment-based configuration

Git and GitHub

19\. Git Workflow



The project is maintained using Git and GitHub.



Basic workflow:



git status





git add .





git commit -m "Your commit message"





git push



The repository is backed up on GitHub.



20\. Security



Never commit sensitive information such as:



Database passwords

JWT secrets

API keys

Personal Access Tokens



Use environment variables instead.



Example:



spring.datasource.password=${DB\_PASSWORD}

jwt.secret=${JWT\_SECRET}

