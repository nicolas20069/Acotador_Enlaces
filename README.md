# Parcial — Acortador de enlaces hexagonal

Aplicación full-stack para acortar URLs, guardar registros en MongoDB y mantener caché en Redis. La estructura está dividida entre dominio, aplicación e infraestructura, con una API REST y un frontend en React + Vite.

## Stack técnico

- Java 21
- Spring Boot 3.5.6
- Maven
- MongoDB 7
- Redis 7
- React + Vite + TypeScript
- WebSocket

## Estructura principal

```text
parcial_segundoCorte/
├── backend/
│   ├── src/main/java/com/example/demo/
│   │   ├── app/
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   ├── src/test/java/
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docker-compose.yml
├── README.md
├── .gitignore
└──
```

## Requisitos previos

- Java 21 instalado y disponible en `JAVA_HOME`
- Maven 3.9+
- Docker Desktop o Docker Engine en ejecución
- Node.js 20 o 22

## 1) Levantar dependencias

Desde la raíz del proyecto:

```powershell
cd "c:\Users\nicom\Desktop\SEPTIMO SEMESTRE\Programacion Avanzada\Segundo corte\parcial_segundoCorte"
docker compose up -d
docker compose ps
```

Esto deja corriendo:
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`

## 2) Ejecutar el backend

```powershell
cd backend
mvn spring-boot:run
```

Si todo está bien, la API queda en:
- http://localhost:8080

## 3) Ejecutar el frontend

```powershell
cd frontend
npm install
npm run dev
```

La interfaz quedará en:
- http://localhost:5173

## Variables de entorno

El backend usa estas variables por defecto en [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml):

```yaml
MONGODB_URI: mongodb://localhost:27017/acortador
REDIS_HOST: localhost
REDIS_PORT: 6379
PUBLIC_BASE_URL: http://localhost:8080
```

Si quieres cambiar puertos o apuntar a otra base, puedes sobrescribirlas antes de arrancar:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/acortador"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PUBLIC_BASE_URL="http://localhost:8080"
```

## Validación de conexiones

### MongoDB

```powershell
docker exec -it parcial2_mongodb mongosh acortador --eval "db.runCommand({ ping: 1 })"
```

Resultado esperado:

```json
{ "ok" : 1 }
```

También puedes consultar documentos:

```powershell
docker exec -it parcial2_mongodb mongosh acortador --eval "db.links.find().limit(1)"
```

### Redis

```powershell
docker exec -it parcial2_redis redis-cli PING
```

Resultado esperado:

```text
PONG
```

## Verificación funcional

### 1. Crear enlace

```powershell
curl -X POST http://localhost:8080/api/links -H "Content-Type: application/json" -d '{"url":"https://www.youtube.com/"}'
```

Respuesta esperada:
- HTTP 201 Created
- JSON con `id`, `originalUrl` y `shortUrl`

### 2. Listar enlaces

```powershell
curl http://localhost:8080/api/links
```

Respuesta esperada:
- HTTP 200 OK
- lista con propiedades `fromCache`, `countdownEnabled` y `ttlMillis`

### 3. Redirección

```powershell
curl -I http://localhost:8080/r/<codigo>
```

Respuesta esperada:
- HTTP 302 Found
- cabecera `Location: https://...`

### 4. WebSocket

Conectarse a:
- ws://localhost:8080/ws/cache

Debe devolver un JSON similar a:

```json
{ "active": true, "remainingMillis": 30000 }
```

## Pruebas del backend

```powershell
cd backend
mvn test
```

Debe terminar con éxito y sin fallos.

## Nota importante sobre el cambio de nombre

El proyecto se renombró de `parcial-acortador-hexagonal` a `parcial_segundoCorte`. Los nombres de contenedores reales que quedaron en [docker-compose.yml](docker-compose.yml) son:

- `parcial2_mongodb`
- `parcial2_redis`

Por eso, cualquier comando del README anterior que use nombres viejos debe reemplazarse por los actuales. El resto de la configuración de conexión sigue apuntando a `localhost:27017` y `localhost:6379`, que son los puertos esperados por la app.

## Checklist rápido

1. Java 21 instalado
2. Docker en ejecución
3. `docker compose up -d`
4. `docker exec -it parcial2_mongodb mongosh acortador --eval "db.runCommand({ ping: 1 })"`
5. `docker exec -it parcial2_redis redis-cli PING`
6. `cd backend && mvn spring-boot:run`
7. `cd frontend && npm install && npm run dev`
8. Revisar `http://localhost:8080` y `http://localhost:5173`

Esto te permite verificar que todo quede funcionando de manera adecuada antes de entregar. 
