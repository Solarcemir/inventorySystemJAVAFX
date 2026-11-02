# CFT Electronic - Technical Documentation

Inventory, invoicing and installation management for CFT Electronic. It runs in the browser on the shop's local network, served by a single Spring Boot application.

The JavaFX client in `desktop-client/` is the old version. It no longer works with this backend and is kept only for reference.

## Architecture

```
  Server PC (localhost:8080)      Other PCs (http://192.168.x.x:8080)
              └──────────── browser ─────────────┘
                             │ HTTP + session cookie
  ┌──────────────────────────▼─────────────────────────┐
  │ backend.jar (Spring Boot 3.4, Java 17)             │
  │  • React app compiled into /static (SpaController) │
  │  • /api/**: Controller → Service → Repository      │
  └──────┬────────────────────────────────┬────────────┘
         │ JPA                            │ local disk
  ┌──────▼───────┐         ┌──────────────▼─────────────┐
  │ Supabase     │         │ Documents\CFT-Electronic\  │
  │ (PostgreSQL) │         │ images\                    │
  └──────────────┘         └────────────────────────────┘
```

| Layer | Technology | Location |
|---|---|---|
| Frontend | React 18, TypeScript, Vite, axios, Recharts | `web-version/frontend/` |
| Backend | Spring Boot, Spring Data JPA | `backend/` |
| Database | PostgreSQL on Supabase | Connection in `backend/config/` |
| Installer | Inno Setup with a bundled JRE | `installer/` |

## Modules

| Module | Page | API | Visible to |
|---|---|---|---|
| Home | `Dashboard.tsx` | `/api/*/count` | Everyone |
| Add product | `AddProduct.tsx` | `POST /api/products`, `/api/images` | Admin |
| Inventory | `Inventory.tsx` | `/api/products`, `/api/sales` | Everyone (edit and delete: admin) |
| Invoices | `Invoices.tsx` | `/api/invoices` | Everyone |
| Installations | `Installations.tsx` | `/api/installations` | Everyone |
| Clients | `Clients.tsx` | `/api/clients` | Everyone (edit, delete, total spent: admin) |
| Sales | `Sales.tsx` | `/api/sales` | Admin |
| Calendar | `Calendar.tsx` | `/api/sales`, `/api/calendar-notes` | Everyone (amounts: admin) |
| Analytics | `Analytics.tsx` | `/api/analytics/*` | Admin |
| Activity log | `ActivityLogPage.tsx` | `/api/logs` | Admin |

Each new page needs a route in `App.tsx` and in `SpaController.java`. Without the second one, reloading that page returns a 404.

## Business rules

**Direct sale.** Checks and lowers the stock, stores a `Sale` with its cost and profit, and adds to the client's `spentAmount`.

**Invoice.** While `OPEN` (at most 5 at a time) it does not touch the stock.

- `finalize` lowers the stock and creates one `Sale` per product line, with the invoice discount spread across the lines. Revenue excludes ITBMS.
- `reopen` and `delete` restore the stock and remove those sales.
- A sale that belongs to an invoice can only be reversed through that invoice.
- The client is free text. When it matches exactly one active client, the sales are linked to that client and count toward their total spent.

**Installation.** A separate record. When `COMPLETED`, its price counts as revenue and profit, with no cost. Billing one creates an invoice with a service line, which moves no stock and creates no `Sale`, so the revenue is not counted twice.

**Activity log.** Written by the backend inside the same transaction as each action, under the logged-in user.

**Product images.** Files live in `Documents\CFT-Electronic\images` on the server PC, and `products.image_path` stores the filename. The app never renames, replaces, moves or deletes those files. Uploading a different picture under an existing name is rejected.

## Security

- `POST /api/login` compares the username and password against the `users` table, and on success opens a server-side session (HttpOnly, SameSite=Strict cookie, 12 hours).
- `AuthInterceptor` requires that session on every `/api` endpoint, except those marked `@PublicEndpoint`. `@AdminOnly` restricts an endpoint to admins.
- `RoleResponseAdvice` and `@JsonView(Views.Admin.class)` remove costs, profit and client spending from the responses sent to sellers.
- Sessions live in memory, so restarting the server logs everyone out.
- Known limitations: passwords are stored in plain text, and the local network uses HTTP.

## Configuration

Database credentials are never committed. `src/main/resources/application.properties` only has `${DB_URL}`, `${DB_USERNAME}` and `${DB_PASSWORD}` placeholders.

| Where | Real values come from |
|---|---|
| Development and builds | `backend/config/application.properties` (ignored by git) |
| Installed app | `app\config\application.properties`, next to the JAR |
| Alternative | `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` environment variables |

To set up a new machine, copy `backend/config/application.properties.example` to `backend/config/application.properties` and fill it in from the Supabase dashboard.

`spring.jpa.hibernate.ddl-auto` is `none`: the app never changes the database schema by itself. A new entity field therefore needs its column added by hand first.

## Running it

**Safe demo, no real data.** Uses an in-memory database with sample data. Nothing is saved, and images go to `target/`. Logins: `admin / admin` and `seller / seller`.

```bash
cd web-version/frontend && npm install && npm run build
cd ../../backend
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.inventory.backend.DemoServer
```

Then open http://localhost:8080. For frontend work, also run `npm run dev` and open http://localhost:5173.

**Against the real database.** `mvn spring-boot:run` from `backend/` uses `backend/config/application.properties`, so every change it makes is real.

**Tests.**

```bash
cd backend && mvn test
```

They run on an in-memory database and a temporary image folder inside `target/`, whatever `backend/config/` contains.

## Building the installer

Run `installer\build.bat`. It needs Node, Maven, Inno Setup and `backend/config/application.properties`, and writes the installer to `installer/output/`.

Install it on the server PC only; the other PCs just use the browser. Installing does not touch the database or the images folder, but the installer does contain the database credentials, so do not share it.

## Author

Luis Hoi Fan - GitHub: [@solarcemir](https://github.com/solarcemir)
