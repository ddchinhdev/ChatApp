# Architecture

```mermaid
flowchart LR
    subgraph client ["Client"]
        browser["React Web App"]
    end
    subgraph gateway ["Web Gateway"]
        nginx["Nginx"]
    end
    subgraph service ["Application"]
        spring["Spring Boot API and STOMP"]
    end
    subgraph datastore ["Data Store"]
        mysql["MySQL 8.4"]
    end
    browser <-->|"HTTP and WebSocket"| nginx
    nginx -->|"Proxy API and WS"| spring
    spring -->|"JPA and Flyway"| mysql
```

STOMP simple broker và Swagger UI chạy bên trong cùng Spring Boot process nên không được vẽ thành container độc lập. Luồng realtime: SEND → JWT Principal → membership check → lưu DB → commit → publish → user queue. Receiver offline vẫn lấy lại message qua sync API.
