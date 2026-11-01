# Testing the frontend against the real backend

Hand-off for the next session. The frontend in `frontend/` has only been verified against the mock gateway (`frontend/scripts/mock-gateway.mjs`), because the machine it was built on had no Docker, no Maven and only JDK 11. Your job is to run it against the real Docker stack, confirm the flows below, and fix what differs.

Read these first, in this order:

1. `frontend/README.md`: how the app is structured and run.
2. `frontend/src/api/journey.ts`: the booking state machine and the backend gaps it works around. Most real-backend surprises will land here.
3. `frontend/src/api/endpoints.ts`: every backend call the app makes.
4. `PRODUCT.md` and `DESIGN.md` at the repo root: product intent and the design system. Don't restyle; fix behaviour.

## 1. Get the code

```bash
git clone https://github.com/moaaz714/EventTicketing.git
cd EventTicketing
git checkout feat/frontend
```

On Windows, clone with `git -c core.longpaths=true clone ...` and then run `git config core.longpaths true`. Long paths broke a clone before.

## 2. Bring up the backend

The service Dockerfiles copy a prebuilt jar (`COPY target/*.jar`), so build the jars first. The project targets **Java 25**. If the machine has no JDK 25 or Maven, build inside a container:

```bash
docker run --rm -v "$PWD":/w -w /w maven:3.9-eclipse-temurin-25 mvn -q -DskipTests package
```

With a local JDK 25 and Maven installed, `mvn -DskipTests package` does the same. Then start the stack:

```bash
docker compose up --build -d
```

What runs, and what matters to the frontend:

| Service | Host port | Notes |
|---|---|---|
| api-gateway | **8080** | The only port the frontend talks to. |
| user, event, booking, ticket, sales services | 8081 to 8085 | Behind the gateway. Useful for debugging direct calls. |
| Postgres (5 instances) | 5432 to 5436 | One per service. |
| RabbitMQ | 5672, UI on **15672** (guest/guest) | Watch the saga messages here. |
| Mongo, Redis, Elasticsearch, Neo4j, Cassandra | 27017, 6379, 9200, 7474/7687, 9042 | Elasticsearch and Cassandra are slow to start. |

- **Startup time.** Expect several minutes on first run: image pulls, then Cassandra and Elasticsearch health checks.
- **Gateway timing.** The gateway only `depends_on` user-service, so it can come up before the others. If early requests return 502 or 503, wait, then `docker compose restart api-gateway`.
- **Health check:** `curl -i http://localhost:8080/api/auth/login -X POST -H 'Content-Type: application/json' -d '{"email":"x","password":"y"}'` should return 401 (not 502). The body is Spring's default `{"error":"Unauthorized"}`; user-service doesn't include its "Invalid credentials" message.

Seed users and bookings after the services have created their schemas. The script uses `docker exec` against the containers by name:

```bash
python3 seed.py
```

It creates users 1 to 5 (password `password123`; `ahmed@example.com` is an attendee, `eve@example.com` is admin) and bookings for user 1. It creates **no events**, and its bookings point at event ids 101 to 105, which don't exist. The app shows those as "Event no longer listed".

## 3. Run the frontend

Use Node 20.19+ or 22.12+ (built with Node 22.19).

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to the gateway (see `vite.config.ts`). There is no CORS config anywhere in the backend, so always go through the proxy; never point the browser straight at :8080.

If Docker runs on another host, or the gateway isn't on localhost:8080, set `GATEWAY_URL`:

```bash
GATEWAY_URL=http://192.168.1.50:8080 npm run dev
```

In PowerShell: `$env:GATEWAY_URL='http://192.168.1.50:8080'; npm run dev`. To open the dev server from another device, add `-- --host`.

## 4. Switching between the mock and the real backend

| Mode | Commands |
|---|---|
| Real gateway | `npm run dev` (proxy to `GATEWAY_URL`, default `http://localhost:8080`) |
| Mock gateway | `npm run mock` in one terminal, `npm run dev:mock` in another (proxy to :8090) |

The mock keeps state in memory and resets when it restarts. It mirrors the backend's behaviour as read from the code, gaps included. It does **not** validate JWT signatures, and it can't reproduce RabbitMQ timing. If the real backend behaves differently, update the mock to match after fixing the app.

## 5. Flows to verify

Run each on desktop (about 1440px wide) and mobile (390px), with the browser network panel open. Every request should go to `/api/...` on :5173 and return 2xx unless noted.

### A. Auth
1. Open `/events` while signed out. You should be redirected to `/login`.
2. Sign in as `ahmed@example.com` / `password123`. You should land on Events.
3. Register a new user (name, email, phone, password). You should be signed in straight away. A duplicate email or phone should show the backend's 409 message.
4. Sign out, then sign back in.

### B. Events
1. With no events, the Events page shows "No events yet" and **Create demo events**. Click it. Six events with sessions are created via `POST /api/events` and `POST /api/events/{id}/sessions`, then `POST /api/events/{id}/index`.
2. The lead event is the soonest upcoming one, set as a pixel headline with date, venue and "From EGP …".
3. The table shows a From price for every event. These are per-event `POST /api/bookings/estimate` calls for 1 standard ticket. Check that the backend's estimate isn't 0 (price = average session capacity ÷ 10).
4. Category chips filter the list. Sorting by Date, Event, Venue and Category works.
5. Search ("jazz", "cairo") calls `GET /api/events/search/full-text` (Elasticsearch). If that call fails, the app falls back to local name matching and says so. Record which happened.

### C. Booking, happy path
1. Open an event. Choose Standard or VIP and a quantity. The estimate updates (VIP is 2.5×). Click **Reserve N seats**.
2. You land on `/bookings/{id}` with status **RESERVED** in pixel type and the strip on step 2.
3. **Confirm booking** calls `PUT /api/bookings/{id}/confirm?eventId=`. Status becomes CONFIRMED.
4. **Check in** calls `PUT /api/bookings/{id}` with `{status:"CHECKED_IN"}`. There is no dedicated endpoint, so the strip label gets a `*`.
5. **Continue to payment** calls `PUT /api/bookings/{id}/complete`, then the app polls for up to 5 seconds for the saga to move the booking to `PAYMENT_PENDING`.
   - Watch RabbitMQ: `booking.completed` should be published, and sales-service should create a PENDING `TicketSale`.
   - If the booking never reaches PAYMENT_PENDING (expected, see gaps), the app sets it itself and marks Payment with `*`.
6. Pay with a credit card. An empty last-four field should show "Enter the last four digits of the card." Enter 4 digits and click **Pay**. This calls `POST /api/sales/booking/{id}` and should return a `TicketSale` with status COMPLETED.
7. Tickets are issued via `POST /api/tickets/batch`. The status settles: **the pixel word dithers out and "Paid" sharpens in, in smooth type**, and the title turns smooth. Tickets appear under the booking and on `/tickets`.

### D. Declined payment (compensation)
1. Repeat C up to payment, tick **Simulate a declined payment**, and click **Pay and decline**. This calls `POST /api/sales/booking/{id}?simulateFailure=true`, which returns a FAILED sale.
2. Expected: `payment.failed`, then booking-service sets the booking to CANCELLED and ticket-service voids its tickets. The page shows "Cancelled" in red with the red cell on Payment.
3. If no service cancels the booking within 5 seconds, the app marks it `PAYMENT_FAILED` with `*`. Record which path the real stack took.

### E. Cancel
1. Reserve a new booking and click **Cancel booking**, then **Yes, cancel booking**. This calls `PUT /api/bookings/{id}/cancel`. Status becomes Cancelled.

### F. Mobile (390px)
1. The header isn't sticky, and nav and search sit on their own rows.
2. The event table hides Venue and Category and shows the venue under the name.
3. The booking strip labels don't wrap, and the payment form is usable with one thumb.

### G. Accessibility and motion
1. Tab through every page: focus rings are visible (blue outline) and there are no traps.
2. "/" focuses search.
3. With reduced motion turned on, status changes swap instantly and nothing blinks.

## 6. Known backend gaps: check these first

Found while reading the backend code. The app works around them where it can.

1. **No CORS anywhere.** The browser must use the Vite proxy. A production build served from another origin would need CORS added to the gateway.
2. **JWT gateway filter** (`api-gateway/.../filter/JwtGatewayFilter.java`):
   - Only `/api/auth/**` is public, and a 401 has an empty body. The app treats any 401 as "signed out".
   - Downstream services read the user id from the JWT `uid` claim, not from `X-User-Id`.
   - The login response has no user object; the app decodes `uid` and `role` from the token.
3. **Missing gateway routes** for `/api/promotions`, `/api/sale-promotions` and `/api/favorite-venues` (`api-gateway/src/main/resources/application.yaml`). This frontend doesn't call them yet. Confirm none show up as 404 in the network panel.
4. **No endpoint sets `CHECKED_IN`**, but `complete` requires it. The app uses the generic booking update.
5. **Nothing moves a booking `COMPLETED` → `PAYMENT_PENDING`, or to `PAID`.** booking-service's `BookingSagaListener` only handles `payment.failed` and `ticket.issued`, and both listeners share one queue with different payload types; check its logs for conversion errors. The app polls, then sets the status itself and records it in `metadata.appSet`.
6. **Tickets aren't issued automatically.** The app issues them after a successful payment. If `ticket.issued` reaches booking-service, the booking may flip to `COMPLETED`; the app shows a `COMPLETED` booking that has a `saleId` as Paid.
7. **Booking creation** needs `userId` and `status: "PENDING"` in the body. There are no defaults.
8. **Errors:** only booking-service includes a `message` in error bodies. Other services return Spring's default `{status, error, path}`, so some error toasts will be generic.
9. **Dates** are `LocalDateTime` without a zone. The app treats them as local time.
10. **No pagination.** Every list is a full array, so `/api/tickets` returns every ticket; the app filters to the user's bookings.

## 7. What "done" looks like

- [ ] The stack is up; the gateway answers on :8080; `seed.py` ran.
- [ ] Flows A to E pass against the real gateway on desktop, and F and G pass on mobile.
- [ ] For each gap in section 6, it's noted whether the real backend behaved as described. Note especially gaps 5, 6 and the declined-payment path in D.
- [ ] Any frontend fix needed for the real backend is made in `src/api/journey.ts` or `src/api/endpoints.ts`, and the mock is updated to match.
- [ ] `npm run build` and `npm run lint` are clean (four existing react-refresh warnings are expected).
- [ ] Results are written back into this file under a new "Results against the real backend" section, or reported to the user, along with any backend changes you'd recommend. Don't change backend code unless the user asks.

## Results against the real backend

Run on 2026-11-01 from `feat/frontend` at `51f9d68`: Windows 11, Docker Desktop 28.3, JDK 25 and Maven 3.9 (local build), Node 24. Desktop at 1440×900 and mobile at 390×844. The frontend works against the real gateway wherever the backend works. The booking journey can't be completed, because four backend bugs block it. Flows C2 to C7 and D were not reached.

### Bringing the stack up

- The jars build cleanly with `mvn -DskipTests package`. `seed.py` ran; on Windows the command is `python seed.py`, since there is no `python3`.
- **ticket-service does not start under Compose.** It has three separate problems:
  1. `contracts/.../feign/TicketServiceClient` uses `${feign.ticket-service.url}`, and ticket-service scans that package but doesn't define the property. Workaround: set the env var `FEIGN_TICKET_SERVICE_URL=http://ticket-service:8080`.
  2. Nothing creates the Cassandra keyspace `etdb_tickets_ks`. `schema-action` only creates tables, the image ignores `CASSANDRA_KEYSPACE`, and k8s uses `eventticketingks`. Workaround: `docker exec eventticketing-cassandra cqlsh -e "CREATE KEYSPACE IF NOT EXISTS etdb_tickets_ks WITH replication = {'class':'SimpleStrategy','replication_factor':1}"`.
  3. **A code bug with no config workaround.** `ticket/messaging/BookingSagaListener` injects the Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper`, but Spring Boot 4 only provides Jackson 3 (`tools.jackson.databind.ObjectMapper`, which sales-service uses). Startup fails with "required a bean of type ObjectMapper". ticket-service stayed down for this run.
- ticket-service and sales-service log only to Loki (`loki.monitoring.svc.cluster.local`), so `docker logs` shows nothing useful. To debug, mount a console logback file and point `LOGGING_CONFIG` at it.
- Let `docker compose pull` finish before the first `up`. Mongo, Redis, Neo4j and Cassandra use `:latest`. Starting the stack while a pull was still running created Mongo's volume with 8.2, and the newer 9.0 image then refused it (exit 62).

### Flows

| Flow | Result |
|---|---|
| A1 Redirect | Pass |
| A2 Sign in | Pass. The JWT carries `uid` and `role`. |
| A3 Register | Pass. A duplicate email or phone gets a 409 with no message, so the app showed "Conflict". Fixed, see below. |
| A4 Sign out and back in | Pass |
| B1 Demo events | **Fail (backend).** `POST /api/events` saves the event, then returns 500 while indexing it in Elasticsearch. `EventSearchDocument` has `indexName = "com/team22/eventticketing/contracts/events"`; it was `"events"` before `64911a3`, which looks like a find-and-replace slip. The app stops after the first event. Once any event exists the button disappears, so the user can't retry. The other demo events were created through the API for the rest of the run. |
| B2 Lead event | Pass, except that "From EGP …" is missing (see B3). |
| B3 From prices | **Fail (backend).** Every estimate returns 503 "Event service unavailable". booking-service's Feign calls to event-service don't forward `Authorization`: its `FeignCorrelationConfig` only adds the correlation id, while the sales-service and user-service versions forward the token. event-service rejects the calls with a 401. The table shows "—". |
| B4 Chips and sorting | Pass. All four sorts set `aria-sort`. |
| B5 Search | `GET /api/events/search/full-text` returns 500 (the same index bug). The app fell back to local matching and said so: "jazz" found 2 events, "cairo" found 4. |
| C1 Reserve | **Blocked by B3.** The estimate error shows in the panel and Reserve stays disabled. |
| C2 to C7 | Not reached. Confirm hits the same auth bug: on seeded booking #5, `PUT /confirm` returns 503 "Event service unavailable", and the app shows that message. |
| D Declined payment | Not reached. |
| E Cancel | **Fail (backend).** `PUT /api/bookings/{id}/cancel` returns 500. `BookingRepository.cancelValidTicketsForBooking` runs `UPDATE tickets …` against the booking database, which has no `tickets` table. The toast shows the raw JDBC message. |
| F Mobile | F1 and F2 pass. F3: strip labels fit on one line at 390 px, though "Checked in" and "Payment" nearly touch; I couldn't test the label with a trailing `*`. The payment form couldn't be reached. |
| G Accessibility | Pass. Every stop on Events has the blue focus outline (search shows it on its form via `:focus-within`), and there are no traps. "/" focuses search. Reduced motion was checked in the CSS only, because the browser couldn't emulate it: the status dither and both blink animations are turned off. |

Also seen: with ticket-service down, `/tickets` shows the loading skeleton for about 25 s before "Internal Server Error" and **Try again**. That's four attempts at roughly 4 s each (the gateway's DNS timeout) plus retry backoff. No requests went to `/api/promotions`, `/api/sale-promotions` or `/api/favorite-venues`.

### Section 6 gaps on the real stack

1. **No CORS:** confirmed. Every call went through the proxy on :5173.
2. **JWT filter:** confirmed. The app decodes `uid` and `role` from the token.
3. **Missing gateway routes:** none of those calls were made, as expected.
4. **No endpoint sets `CHECKED_IN`:** not reached.
5. **Nothing moves a booking to `PAYMENT_PENDING` or `PAID`:** not reached in the UI, but the live RabbitMQ bindings show the saga can't reach booking-service. sales-service publishes `payment.*` to the `payment.events` exchange, which has **no bindings**. booking-service's `booking.saga-feedback` queue binds `payment.*` and `ticket.issued` on `booking.events` only. `booking.completed` does reach sales-service (`payment.saga-listener`), so the pending sale should still be created. Flow D should therefore end on the app's `PAYMENT_FAILED*` fallback.
6. **Tickets aren't issued automatically:** not reached, because ticket-service is down. It publishes `ticket.issued` to `ticket.events`, so booking-service wouldn't receive it either.
7. **Booking creation needs `userId` and `status`:** not reached.
8. **Only booking-service sends error messages:** confirmed. It is the only service with `server.error.include-message: always`.
9. **Dates without a zone:** event dates display as local time, correctly.
10. **No pagination:** caused no visible problem with this data set.

### Frontend changes from this run

- `src/api/endpoints.ts`: when the backend sends only the status text, login shows "Wrong email or password." for a 401, and register shows "That email or phone number is already registered." for a 409. A real backend message still takes precedence.
- `scripts/mock-gateway.mjs`: error bodies now include `message` only for `/api/bookings` routes, matching the real stack.
- `npm run build` is clean, and `npm run lint` shows only the four expected warnings.

### Recommended backend fixes (not made)

1. event-service `EventSearchDocument`: change it back to `indexName = "events"`. This fixes B1, B5 and `/index`.
2. booking-service `FeignCorrelationConfig`: forward `Authorization` the way sales-service does. This fixes B3, C1 and confirm.
3. booking-service cancel: drop `cancelValidTicketsForBooking`. ticket-service already marks tickets `CANCELLED` when it receives `booking.cancelled`. This fixes E.
4. ticket-service: inject `tools.jackson.databind.ObjectMapper`, define `feign.ticket-service.url` (or stop scanning `TicketServiceClient` in ticket-service itself), and create the keyspace at startup or in Compose.
5. Saga routing: either bind `booking.saga-feedback` to `payment.events` (`payment.*`) and `ticket.events` (`ticket.issued`), or publish those events to `booking.events`.
6. Set `server.error.include-message: always` in the other services, so their messages reach the UI.
7. Pin image versions in `docker-compose.yaml`, and give ticket-service and sales-service a console log appender outside k8s.

### Frontend follow-ups worth considering

- Make demo seeding resumable: skip events that already exist by name and add any missing sessions. Keep the button available while demo events are missing.
- Don't put raw 500 messages in toasts. booking-service includes exception text, SQL included.
