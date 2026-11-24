# Darse Quran Academy — Android app design

This is the design the app is built to. It covers who the app is for, how it is
organised, how it looks, and the decisions behind each choice.

## 1. Who it is for, and what they come to do

| Person | Comes to the app to… | How often |
|---|---|---|
| **Enrolled student** | see what's new in their course (announcements, private messages from the teacher), check attendance and marks, **pay the monthly fee**, download receipts | several times a week |
| **Applicant** | find a course, apply, pay the enrollment fee, wait for approval | a few times, then becomes a student |
| **Visitor / well-wisher** | read the daily verse, blog posts, fatwas, library books; buy books | occasionally |

So the app is designed around the **enrolled student**. Public content is still
easy to reach, but it never pushes the student's own courses down the screen.

## 2. Principles

1. **"What do I need to do?" first.** The home screen opens with the student's
   own situation: a fee that is due, a payment waiting for verification, a new
   private message. Everything else comes after.
2. **One tap to pay.** Paying a fee is the only task with money attached and the
   one most often abandoned on the website. The app opens the student's UPI app
   with the amount already filled in (`upi://pay`), then asks for the reference.
3. **Calm and dignified.** It is an Islamic academy: generous whitespace, a
   serif display face, gold used sparingly as an accent, Arabic text given room
   and a proper Naskh font. No loud colours, no gamification.
4. **Recognisably the website.** Deep green and gold, the same logo, serif
   headings, so a student moving between the site and the app feels at home.
   Where the website pattern does not suit a phone (long hero banners, a
   hamburger menu with fourteen entries), the app uses the native pattern.
5. **Works for guests.** Everything public works without an account; signing in
   is asked for only at the moment it is needed (apply, pay, comment, cart).
6. **Honest states.** Every list has a loading skeleton, an empty state that
   says what to do next, and an error state with a retry. Nothing is silent.

## 3. Navigation

A bottom navigation bar with **four tabs** for everyone. As on the website,
where staff see the whole public site plus one header link into their area,
staff get the same four tabs plus **one** more, placed before Account:
**Teach** for teachers, **Admin** for admins, **Developer** for developers.
Every role starts on Home.

| Tab | Icon | Contents |
|---|---|---|
| **Home** | house | greeting, action cards (fee due, pending payment, profile incomplete, verify e-mail), daily inspiration, my courses strip, announcements, featured courses / posts / books |
| **Courses** | book | *My courses* (when signed in) and the *catalogue*; course detail; inside an enrolled course: announcements, attendance, grades, fees |
| **Explore** | compass | the academy's content: blog, fatwa (read and ask), digital library, announcements, teachers |
| **Teach** / **Admin** / **Developer** | (staff only) | the teacher's courses, blogs and fatwa questions; or the admin dashboard: what needs action, then every group of the website's admin sidebar. Admin pages opened from a link open inside this tab |
| **Account** | person | profile and registration number, payments, waiver requests, reviews, bookstore orders, settings, contact, about, policies, sign out, delete account. For guests: sign in / create account, plus the public entries |

The **bookstore** is reached from Home (featured books) and Explore; the
**cart** is a top-bar icon on bookstore screens. **Notifications** are a bell in
the Home top bar with an unread badge.

Detail screens open on top of the tabs with a back arrow; the bottom bar hides
on detail screens so content gets the full height.

## 4. Visual language

### Colour

Built as a Material 3 colour scheme from the website's palette.

| Role | Light | Dark | Source |
|---|---|---|---|
| primary | `#0F3D2E` deep green | `#8FD5B6` mint | website `--color-brand-primary`, the hero |
| onPrimary | white | `#00382A` | |
| primaryContainer | `#D3EFE1` | `#0F4D38` | |
| secondary (accent) | `#9A7B0A` gold (AA on white) | `#E9C349` | website `--color-brand-gold` |
| secondaryContainer | `#F8EBC0` | `#544200` | |
| background / surface | `#FBF9F4` warm paper | `#0E1412` | website `--color-surface-cream` |
| call to action (`Brand.cta`) | `#D4A017` gold, text `#1A1A1A` | same | website `--primary` |
| hero gradient | `#003527` → `#002117` | `#0C3326` → `#04170F` | website PageHero |
| surfaceContainer | `#F2EFE7` | `#1A211E` | |
| error | `#B3261E` | `#F2B8B5` | M3 default |

Primary buttons are **gold with dark text**, like the website's `bg-primary`
buttons (white on gold fails contrast; `#1A1A1A` on gold is about 7.6:1). Gold
**text** stays the darker `#9A7B0A` (AA on white): the accent word of section
titles, prices, small caps. Links and the rest of the Material scheme stay
green.

### Type

| Use | Face | Why |
|---|---|---|
| Hero and page titles (display, headlineLarge/Medium) | **Source Serif 4** (semibold) | the website's PageHero title |
| Section and card headings (headlineSmall, titleLarge and below) | **Poppins** semibold | the website's `font-serif` maps to Poppins bold |
| Section headers | Poppins, uppercase, 0.06em, last word gold | the website's SplitSectionTitle |
| Body / UI | **Poppins** | the website's body face |
| Arabic (verses, hadith, Urdu titles) | **Amiri** | the website's Arabic face; classical Naskh, excellent diacritics |

Fonts are bundled in `res/font` (see FONTS.md). Poppins Bold is not bundled,
so bold maps to SemiBold.

### Shape, space, elevation

- Corner radius: 12dp cards, 8dp inputs and full-width buttons, 16dp sheets
  and dialogs, full-round chips (close to the website's 8px cards and 6px
  buttons).
- 4dp spacing grid; screen padding 20dp; card padding 16dp.
- Cards on `surfaceContainerLowest` (white) with a hairline outline and a soft
  shadow, like the website's `card-elevated`.

### Signature elements

- **Green hero header** on Home and course detail: deep green with a subtle
  geometric pattern, white serif title, a short gold rule. This mirrors the
  website's page heroes.
- **Inspiration card**: cream card, thin gold border, the Arabic text centred in
  Amiri at 26sp with generous line height, the translation below in italic
  serif, reference in gold small caps.
- **Status chips**, worded and coloured as on the website: pending (amber),
  approved and completed (green), active and info (violet), rejected or
  declined (red), always with a text label as well as colour.

## 5. Key screens

### Home (signed in)
1. Top bar: logo mark + "Assalamu alaikum, *Name*", bell with badge.
2. **Action cards**, only when relevant: "Your March fee is due — Pay ₹349",
   "Payment under review", "Complete your profile to enrol", "Verify your e-mail".
3. **Daily inspiration** card.
4. **My courses**: horizontal cards with course title, teacher, roll number, a
   status chip; tapping opens the course.
5. **Announcements** from the academy (horizontal).
6. **Featured courses**, **From the blog**, **Recent fatwas**, **Books**.

Guests see the same screen without 2 and 4, and a gentle "Join the academy"
card instead.

### Course detail
Collapsing green header (category, title, level chip), then teacher card,
schedule (start date, duration), fees (enrollment, course fee per period),
description. A **sticky bottom bar** shows the single next action for this user:
*Sign in to apply · Apply · Pay enrollment fee · Payment under review · Request
declined — apply again · Open course*.

### Enrolled course ("Open course")
Header with roll number, then tabs: **Updates** (course announcements + private
messages, attachments open through the API), **Attendance** (percentage ring +
dated list), **Grades** (each exam with marks / max and a total), **Fees**
(paid periods, pay next period).

### Pay (bottom sheet, three steps)
1. **Amount**: base fee, optional waiver selector showing the amount for each,
   the period (month + year) for course fees.
2. **Pay**: "Pay ₹349 with a UPI app" (opens the chooser with payee, amount and
   note filled in), plus copyable UPI ID and bank details for manual transfer.
3. **Confirm**: the transaction reference (UTR) and an optional screenshot
   (system photo picker), then submit. The amount shown is sent as
   `expectedAmountPaise` so a changed price is caught, not charged.

A 100% waiver skips step 2.

### Bookstore
Two-column grid of book covers with price and MRP strike-through; book detail;
cart (quantity steppers, unavailable books called out); checkout with the
server's price quote (subtotal, shipping by weight, total), delivery address,
then the same UPI pay step as fees.

### Account
Profile header (photo, name, registration number, e-mail), then grouped lists:
*My learning* (payments & receipts, waiver requests, reviews), *Bookstore*
(orders), *Settings* (edit profile, change password, theme), *Academy* (contact,
about, privacy, terms), *Danger zone* (sign out, delete account).

### Readers
Blog post and fatwa answer render the sanitised HTML in native text with the
serif face; images in a gallery; blog likes and comments at the end.

## 6. Architecture

- Kotlin, Jetpack Compose, Material 3; single activity; Navigation Compose with
  type-safe routes.
- MVVM: a `ViewModel` per screen exposing one `StateFlow<UiState>`;
  repositories own network calls; a small manual DI container (`AppContainer`)
  instead of a framework, to keep the build simple.
- Networking: Retrofit + OkHttp + kotlinx.serialization. An `Authenticator`
  refreshes the access token **once, behind a mutex** (refresh tokens are single
  use), and signs the user out when refresh fails.
- Session: DataStore.
- Images: Coil.
- Server per build type: debug and minified builds start on staging (debug can
  switch to production); release starts on `https://darsequranacademy.com`.
- Google sign-in through Credential Manager, using `GOOGLE_WEB_CLIENT_ID` from
  `local.properties`; the button is hidden while that ID is not set.
