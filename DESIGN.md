---
name: Event Ticketing
description: Attendee web app where anything unresolved prints in pixels and anything settled sharpens into smooth type.
colors:
  paper: "#F2F0EB"
  paper-raised: "#FBFAF7"
  ink: "#0A0A0A"
  ink-2: "#4A4945"
  ink-3: "#6B6A65"
  hair: "#CFCCC4"
  accent: "#2B3BFF"
  accent-press: "#1F2BD1"
  danger: "#E5011A"
  danger-ink: "#B80018"
  danger-wash: "#FCE3E5"
  on-fill: "#FFFFFF"
typography:
  display-pixel:
    fontFamily: "Silkscreen, ui-monospace, monospace"
    fontSize: "96px"
    fontWeight: 400
    lineHeight: 1.05
    letterSpacing: "0em"
  display-smooth:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "60px"
    fontWeight: 800
    lineHeight: 1.05
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "32px"
    fontWeight: 800
    lineHeight: 1.1
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "24px"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  title-sm:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "18px"
    fontWeight: 800
    lineHeight: 1.2
  body:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
  body-lg:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "18px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.5
  wordmark:
    fontFamily: "Silkscreen, ui-monospace, monospace"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0em"
  status-pixel:
    fontFamily: "Silkscreen, ui-monospace, monospace"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0em"
  status-smooth:
    fontFamily: "Schibsted Grotesk, system-ui, sans-serif"
    fontSize: "22px"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.01em"
  ticket-code:
    fontFamily: "Silkscreen, ui-monospace, monospace"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: 1.1
    letterSpacing: "0em"
rounded:
  none: "0px"
spacing:
  s-1: "4px"
  s-2: "8px"
  s-3: "12px"
  s-4: "16px"
  s-5: "24px"
  s-6: "32px"
  s-7: "48px"
  s-8: "64px"
  gutter: "40px"
  gutter-sm: "16px"
  container-max: "1440px"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.on-fill}"
    typography: "{typography.body}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.accent-press}"
  button-secondary:
    backgroundColor: "{colors.paper-raised}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "44px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "44px"
  button-ghost-hover:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  button-danger:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.on-fill}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "44px"
  button-danger-outline:
    backgroundColor: "transparent"
    textColor: "{colors.danger-ink}"
    rounded: "{rounded.none}"
    padding: "0 24px"
    height: "44px"
  button-danger-outline-hover:
    backgroundColor: "{colors.danger-wash}"
  chip:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "5px 12px"
  chip-hover:
    backgroundColor: "{colors.hair}"
  chip-selected:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  nav-link:
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "4px 12px"
  nav-link-hover:
    backgroundColor: "{colors.hair}"
  nav-link-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  input:
    backgroundColor: "{colors.paper-raised}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "0 12px"
    height: "44px"
  panel:
    backgroundColor: "{colors.paper-raised}"
    rounded: "{rounded.none}"
    padding: "24px"
  ticket-stub:
    backgroundColor: "{colors.paper-raised}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "24px"
  status-live:
    textColor: "{colors.accent}"
    typography: "{typography.status-pixel}"
  status-settled:
    textColor: "{colors.ink}"
    typography: "{typography.status-smooth}"
  status-failed:
    textColor: "{colors.danger-ink}"
  strip-cell:
    backgroundColor: "{colors.hair}"
    height: "10px"
  strip-cell-done:
    backgroundColor: "{colors.ink}"
  strip-cell-next:
    backgroundColor: "{colors.accent}"
  strip-cell-failed:
    backgroundColor: "{colors.danger}"
  toast:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.none}"
    padding: "12px 12px 12px 16px"
  toast-error:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.on-fill}"
  error-block:
    backgroundColor: "{colors.danger-wash}"
    textColor: "{colors.danger-ink}"
    rounded: "{rounded.none}"
    padding: "24px"
---

# Design System: Event Ticketing

## Overview

**Creative North Star: "The Low-Res Proof"**

The app is a printer's proof of a distributed system. Anything still unresolved (a booking waiting on the saga, a live state, a code the system issued) is set in a coarse bitmap face; anything settled is set in a fine grotesk. When a booking state resolves, its status word visibly breaks up and sharpens into smooth type, so the asynchronous backend is something you watch rather than something hidden behind an instant "done".

The material is newsprint: a warm paper ground, true black ink, 2px ink rules dividing every region, square corners, no shadows and no gradient fills. One synthetic electric blue carries actions and live state; signal red appears only when something failed or is about to be destroyed. Imagery is never photography: every event and ticket gets a halftone plate of square dots generated from its id. Density is high and list-like (a sortable table of what's on, a rail of bookings), with the lead event set like a flyer in large pixel type.

Motion is short, stepped and print-like. Presses compress, the live journey cell blinks like an LED, and the one expressive transition is the status settle: dither out, sharpen in. Everything that moves stops for reduced-motion users, and nothing animates in response to a keyboard shortcut.

**Key Characteristics:**
- Pixel type (Silkscreen) for unresolved and identity text, smooth type (Schibsted Grotesk) for everything people read or type.
- Status resolution, pixel to smooth, as the signature move on every booking and ticket state.
- One accent (#2B3BFF) for actions and live state; red only for failure and destruction.
- 2px ink rules, square corners, flat surfaces; depth comes from rules and the paper / paper-raised step.
- Generated halftone squares, seeded by id, as the only image material.

## Colors

A warm newsprint neutral set, one electric synthetic blue, and a signal red that is held back for bad news.

### Primary
- **Electric Proof Blue** (#2B3BFF): primary buttons, live status words, the active journey cell, the top rule of an in-flight booking in the rail, the rail's call to action, the focus ring, the text caret, text selection, and checked tier radios. **Pressed Blue** (#1F2BD1) is its hover state on pointer devices.

### Tertiary
- **Signal Red** (#E5011A): fills only. Destructive buttons, error toasts, the failed journey cell, and the checkbox that confirms a destructive action.
- **Deep Signal Red** (#B80018): red as text or stroke on paper (5.9:1). Failed status words, form errors, failed journey labels, the outline danger button, the error block border.
- **Red Wash** (#FCE3E5): the error block ground and the hover fill of the outline danger button.

### Neutral
- **Newsprint** (#F2F0EB): the page ground, the sticky top bar, unselected tier options.
- **Proof White** (#FBFAF7): raised surfaces that sit on newsprint: buttons, inputs, panels, the bookings rail, ticket stubs, table row hover, halftone plate grounds.
- **True Ink** (#0A0A0A): all primary text, every 2px rule, selected chips and the active nav link (inverted), default toasts, halftone dots, the scrollbar thumb.
- **Ink 2** (#4A4945): secondary text (8.2:1 on newsprint): metadata, table headers, fact labels, footnotes.
- **Ink 3** (#6B6A65): tertiary text (5.0:1 on newsprint): placeholders, closed rows, voided ticket codes, disabled stepper glyphs, the `kbd` hint.
- **Hairline** (#CFCCC4): 1px row dividers, empty journey cells, unselected tier borders, chip and nav hover fill, dither checker in loading rows.
- **On Fill** (#FFFFFF): text on blue or red fills, and on blue selection.

### Named Rules
**The One Signal Rule.** Electric blue (#2B3BFF) marks only what you can act on and what is in flight. It is never a heading color, a decoration, or a category tint.

**The Red Means Broken Rule.** Signal red fills (#E5011A) and red text (#B80018) appear only for failure and destructive actions. Never for prices, urgency, emphasis or "hot" events.

## Typography

**Display Font:** Silkscreen (with ui-monospace, monospace)
**Body Font:** Schibsted Grotesk (with system-ui, sans-serif)

**Character:** A coarse bitmap face that reads like dot-matrix output, paired with a dense, confident grotesk. The bitmap says "not yet final" or "issued by the machine"; the grotesk says "this is settled, read it".

Pixel text is always weight 400, uppercase, letter-spacing 0, line-height 1 (1.05 for display titles, 1.1 for ticket codes), with font smoothing turned off so edges stay hard.

### Hierarchy
- **Display, pixel** (Silkscreen 400, 96px, 1.05): the lead event headline and the auth-screen mark. Steps down to 64px below 1180px and 48px below 720px. Other display steps: 64, 48, 32, 24px.
- **Display, smooth fallback** (Schibsted Grotesk 800, 60px, 1.05, -0.025em): the same slot when the pixel face cannot be used. Paired sizes: 96 → 60px, 64 → 44px, 48 → 34px, 32 → 26px, 24 → 20px; 96 and 64 drop to 34px below 720px.
- **Headline** (800, 32px, 1.1, -0.02em): page titles and the auth title.
- **Title** (800, 24px, 1.15, -0.015em): booking step titles.
- **Title small** (800, 18px, 1.2): section titles, the rail heading, the event name on a ticket stub, the estimate total.
- **Body** (400, 15px, 1.5): default text. Long prose caps at 52–62ch.
- **Body large** (18px): event descriptions (62ch) and the lead venue line (600).
- **Label** (600, 13px): field labels, table headers, journey cell labels (12px below 720px), small links.
- **Status word**: pixel at 16 / 24 / 32px (sm / md / lg) while live; smooth 800 at 17 / 22 / 30px with -0.01em once settled. The large pixel size drops to 24px below 720px.
- **Ticket code** (Silkscreen 24px, 1.1): the issued code on every stub, wrapping anywhere.
- **Wordmark** (Silkscreen 24px): the home link in the top bar.

### Named Rules
**The Whole-Pixel Rule.** Silkscreen is set only at whole multiples of 8px: 16, 24, 32, 48, 64 or 96px. Responsive changes step between these values; nothing scales the pixel face fluidly or lands between steps.

**The Unresolved-In-Pixels Rule.** The pixel face is reserved for the wordmark, display titles (event headlines, empty-state titles, the auth mark), status words in a waiting or live state, and ticket codes. Everything people read or type is Schibsted Grotesk.

**The Fallback Rule.** A title longer than 28 characters, or containing Arabic (U+0600–06FF, U+0750–077F, U+FB50–FDFF, U+FE70–FEFC), renders in Schibsted Grotesk 800 at the paired smooth size, with `dir="auto"` for Arabic. The pixel face has no Arabic glyphs and stops reading well past that length.

**The No-Tnum Rule.** Never enable tabular figures (`tabular-nums` / `tnum`) on text: this face widens commas, colons and full stops under tnum. Numeric columns align right instead.

## Layout

A full-width app shell capped at 1440px, with a 40px side gutter that drops to 16px at 720px and below. The top bar is sticky, 64px tall, ruled off in 2px ink, and holds the wordmark, nav, search (up to 380px) and account. Spacing follows an 8px-based scale: 4, 8, 12, 16, 24, 32, 48, 64px. Pages pad 32px top, 64px bottom.

Main compositions are asymmetric two-column grids with a fixed side column: listings with a 360px bookings rail (ruled on its left), the lead event with a 300px halftone plate (220px below 1180px), the event page with a 340px plate over a content / 360px sticky booking panel (sticks at 88px), the booking page with a 420px step panel beside the tickets, and the auth screen with a 480px form beside a full-height ink art panel. Ticket stubs fill a grid of columns at least 360px wide.

Breakpoints: at 1180px the 96px pixel title and the auth mark step down to 64px. At 1024px every two-column layout stacks to one column, the rail moves under the content with a top rule instead of a side rule, the search drops to its own row, and the auth halftone is hidden. At 720px the nav takes its own scrollable row, the account email and lead plate hide, pixel titles step down again (96/64 → 48, 48 → 32), tables swap columns via show/hide classes, and stubs go single-column with a 96px side.

## Elevation & Depth

The system is flat. There is no `box-shadow` anywhere and no gradient fill on any surface. Depth comes from two things only: 2px ink rules that divide regions, and the one-step tonal lift from Newsprint (#F2F0EB) to Proof White (#FBFAF7) for surfaces that hold controls or content. Toasts are separated by inverting to solid ink, not by lifting.

### Named Rules
**The Flat Proof Rule.** No shadows, no gradients, no surface blur. If a surface needs separation, give it a 2px ink rule or the paper-raised ground.

**The Dither Exception.** Hard-stop `repeating-conic-gradient` checkers are 1-bit dither patterns, not gradients: they are allowed as the loading-row texture and the status dither-out mask. The 3px blur is allowed only inside the sharpen-in transition.

## Shapes

Every corner is square (0px radius), including inputs and selects, which explicitly reset the platform radius. Lines carry the structure:

- **2px solid ink** (`--rule`): region dividers, button, chip, input and panel borders, table header underline, ticket stub frame, rail item tops.
- **1px hairline** (`--rule-hair`): table row dividers, hints, inline code, the danger-zone divider.
- **2px dashed ink**: perforation and absence. The ticket stub's tear line between main and side, and the frame of empty states.
- **2px hairline**: unselected tier options; the border turns ink when the option is checked.

Halftone plates are SVG grids of square dots (`shape-rendering: crispEdges`), never circles. Icons are drawn on a 16px pixel grid to match the bitmap face.

### Named Rules
**The Square Corner Rule.** Radius is always 0. A rounded corner anywhere is a bug.

**The Halftone-Only Rule.** The only image material is the generated halftone: square dots on a grid (24 × 16 cells by default), density from three soft blobs placed by a generator seeded with the event or ticket id, so each item keeps the same plate forever. Dots are capped below the cell size so the screen never closes into a solid. Ink dots on Proof White; inverted to paper dots on ink in the auth art.

## Components

### Buttons
Blunt, ruled and tactile: a 2px frame, a hard fill, a small compression on press.
- **Shape:** square (0px), 2px ink border, min-height 44px, 0 24px padding, 600 weight, 8px icon gap, no wrapping.
- **Primary:** Electric Proof Blue fill and border, white text. One per decision point (Book, Pay, Confirm).
- **Secondary (default):** Proof White fill, ink border and text.
- **Ghost:** transparent, ink border; on hover it inverts to ink with paper text.
- **Danger:** Signal Red fill and border, white text, for the destructive confirmation itself.
- **Danger outline:** transparent with Deep Signal Red border and text; Red Wash on hover. Used to open a destructive flow.
- **Hover / Focus / Active:** hover only on `(hover: hover) and (pointer: fine)`; primary darkens to Pressed Blue. Focus is a 2px blue outline offset 2px. Press scales to 0.97 over 120ms with the ease-out curve. Disabled drops to 45% opacity with a not-allowed cursor and no press.
- **Text links as buttons:** 600 weight, underlined at a 3px offset. Back links underline on hover.

### Chips
- **Style:** transparent with a 2px ink border, 5px 12px padding, 600 weight, square.
- **State:** selected (`aria-pressed="true"`) inverts to ink fill with paper text; unselected hover fills Hairline. Press scales to 0.97.

### Cards / Containers
- **Corner Style:** square (0px).
- **Background:** Proof White on Newsprint.
- **Shadow Strategy:** none; see Elevation & Depth.
- **Border:** 2px ink.
- **Internal Padding:** 24px for panels; the bookings rail pads 32px 24px.
- **Bookings rail:** a stacked list where each item has a 2px ink top rule; the in-flight booking's rule turns blue and it leads with a blue pixel status and a journey strip. The event name truncates on one line and underlines on hover.

### Ticket Stub (signature)
A box-office ticket. A two-column card (content | 132px side, 96px below 720px) with a 2px ink frame on Proof White. The main side holds the event name (800, 18px), date and venue in muted text, the ticket code in Silkscreen 24px, and the holder with the booking number. A 2px dashed ink tear line separates the side column, which holds a small status word and the ticket's halftone plate (8 × 10 cells). Cancelled and expired stubs strike the code through with a 3px line in Ink 3 and fade the plate to 30%.

### Inputs / Fields
- **Style:** 44px tall, 0 12px padding, 2px ink border, 0 radius, Proof White ground. Labels sit above in 13px / 600, hints below in 13px Ink 2. Placeholders are Ink 3.
- **Focus:** 2px blue outline, 2px offset (the search box shows it on the whole field via `:focus-within`).
- **Error:** form errors are Deep Signal Red, 600.
- **Tier options:** ruled options with a 2px Hairline border that turns ink when checked; radios use the blue accent color.
- **Stepper:** three 44px cells (44 / 56 / 44px) inside a 2px ink frame, the count in 800 18px between inner rules. Buttons press to 0.94.
- **Destructive confirm checkbox:** 18px with a red accent color.

### Navigation
- **Top bar:** sticky, Newsprint ground, 2px ink bottom rule. Wordmark in Silkscreen 24px. Nav links 600 weight with 4px 12px padding; the active link inverts to ink with paper text; others fill Hairline on pointer hover. The `/` key focuses search, shown as a `kbd` hint in Ink 3.
- **Mobile:** below 720px the nav moves to its own horizontally scrollable row, search to the row under it, and the account email hides.

### Tables
Dense and scannable. Headers are 13px / 600 Ink 2 over a 2px ink rule; rows are divided by 1px hairlines and lift to Proof White on pointer hover. Event names are bold links that underline on hover; closed rows go Ink 3. Numeric columns align right. Header sort buttons carry a 10px caret.

### Status Word (signature)
The status of every booking and ticket. While unresolved it is a pixel word in Electric Proof Blue; once settled it is a smooth 800-weight word in ink; a failed state is Deep Signal Red. It is a live region (`role="status"`). When a state goes from unresolved to resolved with a new label, the old pixel word is laid over the new one and the two cross: the pixel word dithers out (a stepped checker mask, three steps over 240ms) while the smooth word sharpens in (200ms, ease-out, 140ms delay, opacity 0 → 1 and blur 3px → 0). The leaving word is removed after 420ms and is hidden from assistive tech.

### Journey Strip (signature)
Five equal cells (Reserved, Confirmed, Checked in, Payment, Paid) separated by a 3px gap, 10px tall (16px with labels and a 4px gap). Empty cells are Hairline, done cells ink, the failed cell Signal Red. The next cell is blue: steady when it is the attendee's move, blinking (`steps(2)`, 1.1s, opacity 1 → 0.25) when the system is working. Labels are 13px / 600, Ink 3 until reached; a blue asterisk marks steps that the app sets itself.

### Feedback
- **Empty state:** a 2px dashed ink frame, 48px 32px padding, with its title in Silkscreen 32px.
- **Error block:** Red Wash ground, 2px Deep Signal Red border and text, 600 weight.
- **Loading:** 14px rows filled with a 4px Hairline checker that blinks in two steps (1s, alternating).
- **Toasts:** fixed bottom-right, up to 400px wide, solid ink with paper text (Signal Red with white for errors), square. They enter from 8px below and fully transparent over 200ms with the ease-out curve, using `@starting-style`.

### Motion
- **Curve:** one ease-out for everything that moves, `cubic-bezier(0.23, 1, 0.32, 1)`. Color changes use plain `ease` at 120ms.
- **Press:** buttons and chips scale to 0.97 (stepper buttons 0.94) over 120ms on `:active`.
- **Settle:** the status dither-out / sharpen-in sequence above is the only expressive transition.
- **Live:** the system-working journey cell blinks in two hard steps; nothing pulses or glides.
- **Stale data:** the price estimate dims to 55% over 150ms while it refetches.
- **Hover** effects exist only under `(hover: hover) and (pointer: fine)`.
- **Keyboard actions never animate** (the `/` search shortcut focuses instantly).
- **Reduced motion** (`prefers-reduced-motion: reduce`): the dither-out word is not rendered and the settled word appears without animation; the journey cell and loading rows stop blinking; toasts appear without a transition; press scaling is removed.

## Do's and Don'ts

### Do:
- **Do** set unresolved and machine-issued text (live status words, ticket codes) in Silkscreen, and switch it to Schibsted Grotesk 800 once it settles.
- **Do** keep Silkscreen at 16, 24, 32, 48, 64 or 96px only, weight 400, uppercase, letter-spacing 0, smoothing off.
- **Do** fall back to Schibsted Grotesk 800 at the paired smooth size for titles over 28 characters or containing Arabic.
- **Do** align numeric columns right instead of using tabular figures.
- **Do** divide regions with 2px ink rules and lift content surfaces to Proof White (#FBFAF7) on Newsprint (#F2F0EB).
- **Do** give every event and ticket a halftone plate of square dots seeded by its id.
- **Do** use the ease-out curve `cubic-bezier(0.23, 1, 0.32, 1)`, 120ms press at scale 0.97, and gate hover behind `(hover: hover) and (pointer: fine)`.
- **Do** remove movement and blinking under `prefers-reduced-motion: reduce`.

### Don't:
- **Don't** use the pixel face for body copy, form text, buttons, nav links, or at any size that is not a whole multiple of 8px.
- **Don't** turn on `tabular-nums` or `tnum` on text; the face widens commas, colons and full stops.
- **Don't** use Electric Proof Blue (#2B3BFF) for anything other than actions and live state.
- **Don't** use red (#E5011A fills, #B80018 text) for anything other than failure and destructive actions.
- **Don't** add shadows, gradient fills or rounded corners.
- **Don't** use photography, stock imagery or round halftone dots; plates are generated square dots only.
- **Don't** animate in response to keyboard actions, or fake an instant success: a state that is still in flight stays in pixels until the backend settles it.
