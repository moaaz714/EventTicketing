# Event Ticketing — web frontend

The attendee app for the Event Ticketing microservices: sign in, browse and search events, book seats, walk the booking through the saga, pay, and keep your tickets.

Built with React 19, Vite, TypeScript, React Router, TanStack Query and plain CSS tokens. Fonts (Silkscreen, Schibsted Grotesk) are bundled, nothing is loaded from a CDN.

## Run it

Against the real backend (gateway on `:8080`):

```bash
npm install
npm run dev
```

Open http://localhost:5173. The dev server proxies `/api` to the gateway, because the backend has no CORS configuration. Point it elsewhere with `GATEWAY_URL=http://host:port npm run dev`.

Without the Docker stack, use the mock gateway, which mirrors the backend's behaviour (including its gaps, below):

```bash
npm run mock        # terminal 1, serves :8090
npm run dev:mock    # terminal 2
```

Sign in with a seeded user: `ahmed@example.com` / `password123`.

Testing against the real Docker stack (backend build, seeding, flows to verify, known gaps): see [TESTING.md](TESTING.md).

## First run

`seed.py` creates users and bookings but **no events**. The Events page offers **Create demo events**, which creates six events and their sessions through `POST /api/events` and `POST /api/events/{id}/sessions` (flagged `details.demo = true`).

## The booking journey, as the backend implements it

| Step | Call | Notes |
|---|---|---|
| Reserve | `POST /api/bookings` (+ `/items`) | Body must include `userId` and `status: "PENDING"`. Price comes from `POST /api/bookings/estimate`. |
| Confirm | `PUT /api/bookings/{id}/confirm?eventId=` | Event must be `UPCOMING`. |
| Check in | `PUT /api/bookings/{id}` `{status: "CHECKED_IN"}` | No dedicated endpoint exists. |
| Open payment | `PUT /api/bookings/{id}/complete` | Publishes `booking.completed`; sales-service opens a pending sale. |
| Pay | `POST /api/sales/booking/{id}` | Requires `PAYMENT_PENDING`. `?simulateFailure=true` runs the compensation path. |
| Tickets | `POST /api/tickets/batch` | Issued after a successful payment. |

Backend gaps the UI works around, visibly: nothing moves a booking from `COMPLETED` to `PAYMENT_PENDING`, or from payment to `PAID`. The app waits a few seconds for the saga, then sets the status through the generic booking update and records it in `metadata.appSet`. Those steps are marked with `*` on the booking page. See `src/api/journey.ts`.

## Design

Visual world "Low-Res Proof": anything unresolved renders in pixel type; when a booking state settles, its status dithers out and sharpens into smooth type. Tokens live in `src/styles/tokens.css`; the full system is documented in `../DESIGN.md`.

## Scripts

| Script | What it does |
|---|---|
| `npm run dev` | Vite dev server, proxied to the gateway on :8080 |
| `npm run dev:mock` | Vite dev server, proxied to the mock on :8090 |
| `npm run mock` | Mock gateway on :8090 |
| `npm run build` | Type-check and build to `dist/` |
| `npm run lint` | ESLint |
