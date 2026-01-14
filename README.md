# EVRPTW ALNS Project

Project giải bài toán EVRPTW (Electric Vehicle Routing Problem with Time Windows) bằng thuật toán ALNS.

Gồm 3 folder chính:

### 1. [Client](./client/)
*   **Frontend Web**: Next.js 16, TailwindCSS, Leaflet.
*   Giao diện quản lý, xem bản đồ, setup bài toán. Chủ yếu ở mức concept

### 2. [Server](./server/)
*   **Backend API**: Spring Boot 3, H2 DB.
*   Middleware xử lý data, auth, và đã có 1 wrapper của thuật toán.

### 3. [Core Algo](./core_algo/)
*   **Optimization Engine**: Java 21, Maven.
*   Chứa logic thuần của thuật toán ALNS. Chạy độc lập để benchmark.

---

## Quick Start

1.  **Clone Repo**:
    ```bash
    git clone https://github.com/tranhuy105/GR2.git
    cd GR2
    ```

2.  **Core Algo** (Must Build First):
    *   Cần build ra jar trước để Server gọi được.
    *   `cd core_algo` -> `mvn package -q -DskipTests`

3.  **Server**:
    *   `cd server` -> `mvnw spring-boot:run`
    *   API: `localhost:8080`

4.  **Client**:
    *   `cd client` -> `npm install` -> `npm run dev`
    *   Web: `localhost:3000`

## Authors
**Hust GR2** - Trần Quang Huy.
