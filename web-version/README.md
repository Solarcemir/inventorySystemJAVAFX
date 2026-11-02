# CFT Electronic - Web Frontend

React + TypeScript frontend for the Spring Boot backend in `../backend`. In production it is compiled into the backend and served from the same port.

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173, proxies /api to localhost:8080
npm run build   # type-checks and writes the bundle to backend/src/main/resources/static
```

See [DOCUMENTATION.md](../DOCUMENTATION.md) for the architecture, configuration and how to run the backend safely.
