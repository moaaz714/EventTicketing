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
- **Health check:** `curl -i http://localhost:8080/api/auth/login -X POST -H 'Content-Type: application/json' -d '{"email":"x","password":"y"}'` should return 401 "Invalid credentials" (not 502).

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
