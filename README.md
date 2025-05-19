# Anubix - Custom API Gateway with Advanced Load Balancing
![Untitled-2025-05-08-1519](https://github.com/user-attachments/assets/2044c9aa-4811-4066-bc16-dc163edc2731)

Anubix is a demonstration of a custom-built API Gateway with advanced load balancing capabilities, built with Spring Boot 3.2.5 and Spring Cloud 2023.0.1. The platform showcases a robust API Gateway implementation with custom load balancing, circuit breaking, rate limiting, and monitoring capabilities.

Built on Spring WebFlux and Project Reactor, Anubix leverages reactive programming to provide non-blocking, event-driven architecture. This enables high throughput and low latency, making it ideal for handling concurrent requests and real-time data processing.

## 🏗️ Core Components

### API Gateway (Main Component)

- **Reactive Architecture**:
  - Non-blocking I/O with WebFlux
  - Event-driven processing
  - Backpressure handling
  - Reactive streams support
- **Custom Load Balancer**: Implements multiple load balancing algorithms (Round Robin, Least Connections , adaptive)
- **Dynamic Service Discovery**: Automatic service registration and discovery via Eureka
- **Advanced Routing**: Intelligent request routing with path-based and service-based routing
- **Resilience Patterns**:
  - Circuit Breaking with Resilience4j
  - Rate Limiting per service
  - Retry mechanisms with configurable attempts
  - Request caching with Redis
- **Security**:
  - JWT validation
  - Request authentication
  - Rate limiting per client
- **Monitoring**:
  - Prometheus metrics integration
  - Grafana dashboards
  - Health checks
  - Request tracing

### Supporting Services (Demonstration)

The following services are included as demonstrative examples to showcase the API Gateway's capabilities:

- **Auth Service**: Demonstrates JWT authentication flow
- **Feature Service**: Example of service scaling and load balancing
- **Toggle Service**: Shows circuit breaking and resilience patterns
- **Analytics Service**: Demonstrates metrics collection and monitoring

## 🚀 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven

## ⚙️ Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/Petrichor0314/Anubix.git
   cd Anubix
   ```

2. **Create environment files**

   rename `.env.example` file in the root directory to `.env`:

3. **Build and Run**
   ```powershell
   .\rebuild.ps1
   ```

## 🔧 API Gateway Features

### 1. Load Balancing

- **Multiple Algorithms**:
  - Round Robin
  - Adaptive Algorithm
  - Least Connections
- **Dynamic Instance Management**:
  - Automatic instance discovery
  - Health-based instance selection
  - Instance weight management

### 2. Resilience Patterns

- **Circuit Breaking**:
  - Configurable failure thresholds
  - Automatic circuit state management
  - Fallback mechanisms
- **Rate Limiting**:
  - Per-service rate limits
  - Per-client rate limits
  - Configurable time windows
- **Retry Mechanism**:
  - Configurable retry attempts
  - Exponential backoff
  - Retry condition customization

### 3. Caching

- **Redis Integration**:
  - Response caching
  - Configurable TTL
  - Cache invalidation
- **Cache Strategies**:
  - Time-based expiration
  - Event-based invalidation
  - Selective caching

### 4. Monitoring & Observability

- **Metrics Collection**:
  - Request counts
  - Response times
  - Error rates
  - Circuit breaker states
- **Health Checks**:
  - Service health monitoring
  - Instance health tracking
  - Health-based routing

## 📊 Monitoring

### Prometheus

- Access at `http://localhost:9090`
- Scrapes metrics every 5 seconds
- Key metrics:
  - Request rates
  - Response times
  - Error rates
  - Circuit breaker states
  - Load balancer statistics

### Grafana

- Access at `http://localhost:3000`
- Default credentials: admin/admin
- Pre-configured dashboards:
  1. API Gateway Overview
  2. Load Balancer Metrics
  3. Circuit Breaker States
  4. Rate Limiter Statistics

## 🔍 Testing the API Gateway

### 1. Load Balancing

- Scale services to multiple instances
- Monitor request distribution
- Test different load balancing algorithms

### 2. Resilience Testing

- Simulate service failures
- Observe circuit breaker behavior
- Test rate limiting
- Verify retry mechanisms

### 3. Performance Testing

- Monitor response times
- Check caching effectiveness
- Verify load distribution

### 4. Testing Tools

#### Python Load Testing Script

Located in `simulate-requests/simulate_requests.py`, this script provides:

- Multi-threaded request simulation
- JWT authentication flow testing
- Random endpoint selection across services
- Mix of authenticated and unauthenticated requests
- Detailed request/response logging
- Configurable request delays and timeouts

Usage:

```bash
cd simulate-requests
python simulate_requests.py
```

#### Postman Collection

The project includes a Postman collection for manual testing of:

- API Gateway endpoints
- Service discovery
- Load balancing behavior
- Circuit breaker states
- Rate limiting
- Cache operations

### 5. Logging

The API Gateway includes extensive logging to facilitate testing and debugging:

- Request/response details
- Load balancer decisions
- Circuit breaker state changes
- Rate limiter events
- Cache operations
- Service discovery events
- Authentication/authorization events

## 🛠️ Development

### Project Structure

```
Anubix/
├── api-gateway/           # Custom API Gateway implementation
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── petrichor/
│   │   │   │           └── loadbalancer/
│   │   │   │               └── load_balancer/
│   │   │   │                   ├── algorithm/    # Load balancing algorithms
│   │   │   │                   ├── config/       # Gateway configuration
│   │   │   │                   ├── controller/   # API endpoints
│   │   │   │                   ├── factory/      # Factory patterns
│   │   │   │                   ├── model/        # Data models
│   │   │   │                   ├── registry/     # Service registry
│   │   │   │                   ├── security/     # Security components
│   │   │   │                   ├── service/      # Business logic
│   │   │   │                   ├── util/         # Utility classes
│   │   │   │                   └── ApiGatewayApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml         # Gateway configuration
│   ├── pom.xml           # Gateway dependencies
│   └── Dockerfile        # Gateway container definition
├── [other services]/     # Supporting microservices (auth, feature, toggle, analytics)
├── simulate-requests/    # Python load testing script
├── prometheus/          # Prometheus configuration
├── docker-compose.yml   # Docker services composition
├── pom.xml             # Parent Maven configuration
└── rebuild.ps1         # PowerShell build script
```

The API Gateway is the core component of this project, implementing custom load balancing, circuit breaking, and other advanced features. Other services are included as demonstrative examples to showcase the gateway's capabilities.

## 📈 Scaling

The API Gateway is designed to handle:

- Multiple backend service instances
- High request volumes
- Dynamic service scaling
- Configurable load balancing

## 🐛 Troubleshooting

### Common Issues

1. **Load Balancer Issues**

   - Check service registration in Eureka
   - Verify load balancing algorithm configuration
   - Monitor instance health

2. **Circuit Breaker Issues**

   - Check circuit breaker configuration
   - Monitor failure rates
   - Verify fallback mechanisms

3. **Rate Limiting Issues**
   - Verify rate limit configuration
   - Check client identification
   - Monitor rate limit metrics

### Logs

```bash
# View API Gateway logs
docker-compose logs -f api-gateway

# View all logs
docker-compose logs -f
```

## 🚀 Future Improvements

While this project demonstrates key API Gateway concepts, it's important to note that this is a learning/demonstration project and not production-ready. Here are some areas for potential improvement:

### Technical Enhancements

- Implement more sophisticated load balancing algorithms
- Add support for WebSocket connections
- Enhance security with OAuth2/OIDC
- Implement API versioning
- Add request/response transformation capabilities
- Implement more advanced caching strategies
- Add support for GraphQL

### Production Readiness

- Comprehensive test coverage
- Performance benchmarking
- Security auditing
- Documentation improvements
- Container optimization
- CI/CD pipeline
- Production-grade monitoring
- Backup and recovery procedures

### Scalability

- Horizontal scaling of the gateway itself
- Distributed rate limiting
- Global load balancing
- Multi-region deployment support
- Service mesh integration

## 📝 License

This project is open source and available under the MIT License. Feel free to use, modify, and distribute it as you wish.

## 👥 Contributing

Contributions are welcome! Whether it's:

- Bug fixes
- Feature enhancements
- Documentation improvements
- Performance optimizations
- Test cases
- Example implementations

Please feel free to:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📚 Additional Resources

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [Resilience4j Documentation](https://resilience4j.readme.io/docs)
- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
