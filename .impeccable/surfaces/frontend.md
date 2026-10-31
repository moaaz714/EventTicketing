---
version: 1
slug: "frontend"
primary_target: "frontend"
related_targets: []
---

# Surface brief: attendee web app (`frontend/`)

Mode: Operate. Code-led build.

Scope: the attendee journey only (sign in or register → browse/search events → event detail and sessions → book → pay → tickets). Organizer, admin, analytics, promotions and refunds are later slices.

Audience and job: reviewers of the project walking the attendee path on a desktop browser (mobile must work). Success is find → book → pay → see a ticket in a few minutes without reading code, and noticing that bookings move through real async states.

Constraints: backend has no CORS (Vite proxy `/api` → gateway :8080); login returns only a JWT (decode `uid`, `role`); booking create needs `userId` and `status: "PENDING"`; no events are seeded; event names may be Arabic (no pixel glyphs).

Unresolved: how to reach the payment step given that `complete` requires `CHECKED_IN`, which no endpoint sets (see plan).

## Direction contract

THESIS: The app owns one idea: anything unresolved renders in pixels; anything settled renders in smooth type. A booking in flight through the saga literally sharpens when it resolves. It refuses the category default of photo cards in a soft grid with a purple accent.

OWN-WORLD: Newsprint ground (#F2F0EB), true ink (#0A0A0A), one synthetic accent, electric blue (#2B3BFF), for primary actions and live state; signal red (#E5011A) only for failure and destructive confirmation. 2px ink rules divide regions; no shadows, no gradients, square corners. Coarse bitmap display face (Silkscreen) at whole-pixel multiples; a fine grotesk (Schibsted Grotesk) for everything people read or type. Halftone/dot-matrix is the only image material: event tiles are generated halftone patterns seeded by event id.

STORY: The visitor sees tonight's lead event set like a flyer, scans a dense, sortable list of what's on, opens one, books seats, and watches the booking's status walk from pixel to smooth as it is confirmed, paid and ticketed. They leave understanding that the system is real and asynchronous.

FIRST VIEWPORT: Top bar (wordmark in bitmap, Events / My bookings / Tickets, search, account). Left 2/3: lead event as a pixel headline at ~96px with date, venue, from-price and a Book button; category filter chips; event table (date, event, venue, category, from). Right 1/3: Your bookings rail; the booking in motion leads with its status in blue bitmap and a 5-cell progress strip; settled bookings below in smooth bold.

FORM: Emigre bitmap specimen (bolder re-roll 1, dealt 2nd; chosen over the dealt leader). seed:b63deee5e195bc19. Signature move: status resolution, pixel → smooth, on every booking and ticket state, animated as a short dither-out when a state settles.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
