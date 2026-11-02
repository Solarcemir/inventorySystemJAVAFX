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

**Retries.** The sale endpoints accept an optional `Idempotency-Key` header. Repeating a key returns the first result instead of selling again, per user and per sale, for 10 minutes; a failed attempt is forgotten so it can be retried. Keys live in memory, so a restart forgets them. The frontend keeps one key per sale until it succeeds, and disables create buttons while a request is running. Generate keys with `newIdempotencyKey`, because `crypto.randomUUID` does not exist on plain HTTP pages such as `http://192.168.x.x:8080`.

**Installation.** A separate record. When `COMPLETED`, its price counts as revenue and profit, with no cost. Billing one creates an invoice with a service line, which moves no stock and creates no `Sale`, so the revenue is not counted twice.

**Product search.** The Inventory and Invoices search bars share `productService.matchesSearch`. Every word typed must appear in the name, category, provider, description, location or ID, in any position and order, ignoring case, accents and separators.

**Activity log.** Written by the backend inside the same transaction as each action, under the logged-in user.

**Query performance.** Every query travels over the internet to Supabase, so no screen may issue more queries as data grows; `QueryCountTests` enforces this. Totals are calculated by the database in repository queries, and lists load their relations with `@EntityGraph`. Do not load sales into memory to add them up.

**Product images.** Files live in `Documents\CFT-Electronic\images` on the server PC, and `products.image_path` stores the filename. The app never renames, replaces, moves or deletes those files. Uploading a different picture under an existing name is rejected.

## Security

- `POST /api/login` checks the username and password against the `users` table, and on success opens a server-side session (HttpOnly, SameSite=Strict cookie, 12 hours).
- Passwords are stored as BCrypt hashes. One still in plain text is replaced by its hash at that user's first correct login, after which versions before 1.2.0 can no longer log that user in.
- To add a user or reset a forgotten password, write the new password in plain text in the `password` column. It is hashed at that user's next login.
- `AuthInterceptor` requires that session on every `/api` endpoint, except those marked `@PublicEndpoint`. `@AdminOnly` restricts an endpoint to admins.
- `RoleResponseAdvice` and `@JsonView(Views.Admin.class)` remove costs, profit and client spending from the responses sent to sellers.
- Sellers can sell, create and finalize invoices, edit or cancel open invoices (except the discount), and create and advance installations. Only admins can change discounts, edit closed invoices, reopen or delete invoices, change an installation's price, edit completed installations, or delete installations. These rules are enforced by the server, not only by hiding buttons.
- Queries only use Spring Data methods or `@Query` with bound parameters. Never build a query by joining strings, and never save an entity received from the browser without copying the allowed fields.
- Sessions live in memory, so restarting the server logs everyone out.
- After 5 wrong passwords for a username, that username must wait 30 seconds before each new attempt (`LoginThrottle`). Unknown usernames are treated the same way.
- Every response carries `X-Frame-Options`, `X-Content-Type-Options` and `Referrer-Policy`, and served images cannot run script.
- Error responses never include stack traces or database messages: those go only to the server log.
- Known limitation: the local network uses HTTP, so passwords travel unencrypted between the browsers and the server.

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
cd backend && mvn test                  # backend, on an in-memory database
cd web-version/frontend && npm test     # frontend helpers (search, retry keys, image addresses)
```

The backend tests use an in-memory database and a temporary image folder inside `target/`, whatever `backend/config/` contains.

## Building the installer

Run `installer\build.bat`. It needs Node, Maven, Inno Setup and `backend/config/application.properties`, and writes the installer to `installer/output/`.

`stop.bat`, also run by the uninstaller, stops only the Java process running this installation's `backend.jar`; other Java programs on the PC keep running.

Install it on the server PC only; the other PCs just use the browser. Installing does not touch the database or the images folder, but the installer does contain the database credentials, so do not share it.
