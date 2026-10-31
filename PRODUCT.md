# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

React + Vite + TypeScript in `frontend/` (React Router, TanStack Query, plain CSS with design tokens). The Vite dev server proxies `/api` to the Spring Cloud Gateway on `http://localhost:8080`; the backend has no CORS config.

## Users

Primary: event-goers (attendees) who browse and search events, book seats for a session, pay, and keep their tickets. In practice the people opening it are reviewers of this project (recruiters, graders, teammates) walking through the attendee journey to see the backend work. Organizer, sales and admin users exist in the backend but are out of scope for the first version.

## Product Purpose

A web frontend for the Event Ticketing microservices platform (GUC MS3: user, event, booking, ticket and sales services behind an API gateway). It exists as a portfolio/demo piece: it should feel like a real ticketing product while making the interesting backend behavior visible — JWT auth, event search, the booking saga (COMPLETING → PAYMENT_PENDING → PAID / PAYMENT_FAILED → REFUNDED), payment, and issued tickets. Success: a reviewer can register, find an event, book it, pay, and see their tickets in a few minutes without reading the code, and comes away understanding what the system does.

## Positioning

Not a generic ticket marketplace clone: the product's truth is a distributed system whose state changes asynchronously (RabbitMQ choreography between services). The UI may show that honestly — a booking moves through real states rather than flipping instantly to "done".

## Operating Context

Runs locally against `docker-compose` (gateway on :8080). Data comes from `seed.py` and whatever reviewers create. Reviewers usually use a desktop browser; mobile should still work.

## Capabilities and Constraints

- Auth: `/api/auth/register`, `/api/auth/login` return a JWT validated by the gateway.
- Events: list, search, full-text search, sessions, ratings, top-rated.
- Bookings: create, estimate, confirm, cancel, complete, items; booking status follows the saga above.
- Payment: `POST /api/sales/booking/{id}`.
- Tickets: issued per booking, history, unused-upcoming, nearby.
- Gateway lacks routes for `/api/promotions`, `/api/sale-promotions`, `/api/favorite-venues`; those need adding before the frontend uses them.
- Later slices (undecided timing): organizer event management, analytics dashboards, refunds, promotions.

## Evidence on Hand

No brand assets, logos, photography or copy exist. Event content comes from the backend seed data; any illustrative imagery must be authored or clearly placeholder. No real customers, venues, prices or reviews may be invented as claims.

## Product Principles

1. The attendee journey (find → book → pay → ticket) is the spine; everything else is secondary.
2. Show the system's real state; never fake instant success over an async saga.
3. A reviewer should never need the README to complete the main flow.
4. Credible as a real product, honest as a student project: no invented commercial claims.
