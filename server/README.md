# Server (Backend API)

Backend của hệ thống, viết bằng **Spring Boot**. Đảm nhiệm việc handle API request, DB operations và gọi cái Core Algo để chạy tối ưu.

## Tech Stack

*   **Core**: Java 17+, Spring Boot 3.4.1.
*   **DB**: H2 Database (In-memory, dev tiện), Spring Data JPA.
*   **Auth**: Spring Security + JWT.
*   **Build**: Maven.

## Setup & Run

1.  Vào folder server:
    ```bash
    cd server
    ```

2.  Build & Install dependencies (dùng wrapper có sẵn):
    ```bash
    # Linux/Mac
    ./mvnw clean install
    
    # Windows
    mvnw clean install
    ```

3.  Start server:
    ```bash
    # Linux/Mac
    ./mvnw spring-boot:run
    
    # Windows
    mvnw spring-boot:run
    ```

Server chạy default ở port **8080**: [http://localhost:8080](http://localhost:8080).

## Note
DB H2 là in-memory nên tắt server là mất data. Config trong `application.properties` nếu muốn đổi sang MySQL/Postgres.
