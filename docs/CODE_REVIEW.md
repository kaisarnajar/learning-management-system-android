# Code review and implementation plan — November 2026

A full review of the Android app (≈20,000 lines of Kotlin in `app/src/main`, 73 test files) covering security,
architecture, code quality, logic, performance, tests and configuration. Every finding below was confirmed by
reading the code; none is speculative. Line numbers are as of commit `9887fa0`, before any fix.

## Overall health

The codebase is in good shape. The architecture is consistent (one activity, Compose, ViewModel + StateFlow,
manual DI in `AppContainer`, Retrofit services per area), the network layer is careful (token refresh behind a
mutex on its own dispatcher, the Bearer token sent only to the academy's own host, cleartext disabled), money is
integer paise throughout, staff areas are gated by role both in the navigation graph and in link routing, and most
submits already guard against double taps. CI builds both flavors, runs lint and detekt with frozen baselines, unit
and Robolectric tests, and an R8 release build. Baseline before this work: build, lint, detekt and 226 unit tests
all green.

No Critical issues were found. The findings are mostly lifecycle edge cases (cancellation, rotation, double taps),
a few navigation gaps, and dead code left behind by earlier refactors.

## Findings

Severity: **High** — security or data correctness a user can hit in normal use. **Medium** — wrong behaviour or a
real exposure in a plausible situation. **Low** — rare edge case, cosmetics, dead code.

### Security

| ID | Sev | Where | Issue | Fix |
|---|---|---|---|---|
| S1 | High | `data/repo/AuthRepository.kt:43`, `data/SessionStore.kt:112` | Sign-out waited for the server revoke (up to the 20 s connect timeout offline) before clearing the session, in the Account ViewModel's scope. Leaving the screen cancelled it and the user stayed signed in. Clearing the session also tears down the graph, which could cancel the DataStore write. | Bound the revoke (5 s); run sign-out and all session writes `NonCancellable`. |
| S2 | Medium | `data/SessionStore.kt:100`, `AuthRepository.kt:72` | A `/auth/me` answer that arrived after user A signed out and B signed in was written into B's session (A's name, e-mail, role — possibly the admin UI). | `updateUser` ignores a different user id. |
| S3 | Medium | `AndroidManifest.xml` | `allowBackup="false"` does not stop Android 12+ device-to-device transfer: the session DataStore (access and refresh tokens) is copied to a new phone. | Add `dataExtractionRules` excluding everything from cloud backup and device transfer. |
| S4 | Low | `data/ApiCall.kt:41` | A `SerializationException` is logged with `Log.w` in release; kotlinx's message quotes part of the JSON response, which can hold profile or payment data. | Log the exception only in debug builds. |

### Logic and correctness

| ID | Sev | Where | Issue | Fix |
|---|---|---|---|---|
| L1 | Medium | `ui/explore/ExploreScreens.kt:217` | `BlogPostViewModel.comment` has no busy guard: two taps in one frame post the comment twice. | Guard and set busy before launching. |
| L2 | Medium | `ui/explore/ExploreScreens.kt:392` | `AskFatwaViewModel.submit` same: a fatwa question can be sent twice. | Same. |
| L3 | Medium | `data/repo/CartStore.kt:90`, `ui/bookstore/BookstoreScreens.kt:340` | After an order, putting the left-out books back ignores failures and runs in the checkout ViewModel scope: a failure or leaving the screen silently drops them from the cart. | Run the restore `NonCancellable`; report how many couldn't be put back. |
| L4 | Low | `data/repo/CartStore.kt:98` | `clearLocal` (sign-out) can be undone by a cart change still in flight, showing the previous user's cart. | Generation counter bumped by `clearLocal`; stale answers are dropped. |
| L5 | Low | `ui/bookstore/BookstoreScreens.kt:206` | "Add to cart" tapped twice before the server answers adds 2 copies. | The button sets the quantity to 1 instead of adding 1. |
| L6 | Low | `ui/pay/PayScreen.kt:84` | A waiver restored after process death that the fresh quote no longer offers is still sent, so the server refuses the payment. | Drop the restored coupon when the quote doesn't list it. |
| L7 | Low | `ui/pay/PaymentProof.kt:181` | The UPI app link and the QR code disagree: a blank payee name is sent as is, and the note limit differs. | Use the QR code's payee and note rules for the link. |
| L8 | Low | `ui/courses/CoursesScreens.kt:328` | Rotating while an application is being sent calls the old NavController, so the Pay screen never opens (the page then offers "Pay"). | Signal "go to pay" through state the screen observes. |
| L9 | Medium | `ui/nav/AppRoot.kt:232`, `ui/common/Files.kt:24` | `mailto:`, `tel:` and `//host` links in content are classed as outside links but `openWeb` prefixes them with the site URL, opening a 404 page. | Hand non-web schemes to the phone; treat `//host` as https. |
| L10 | Medium | `ui/nav/AppRoot.kt:385`, `ui/account/AccountScreens.kt:532` | Tapping a notification whose link has no app screen (or is absolute) does nothing. | Route notification taps through the same link opener as content links (in-app screen, else the website). |
| L11 | Medium | `ui/common/PagedList.kt:72` | A failed filter or search change leaves the old list under the new filter; Retry then appends page 2 of the new filter to it. | Retry after a failed reset reloads from page 1. |
| L12 | Medium | `ui/account/WaiverScreens.kt:80` | A failed waiver request inserts the error banner above the form; unkeyed items shift and the form loses everything typed. | Key the LazyColumn items. |
| L13 | Low | `WaiverScreens.kt:22`, `ReviewsViewModel.kt:28`, `AccountScreens.kt:346,357`, `AuthScreens.kt:117,125,196` | Waiver request, review, profile save/photo and sign-in have no same-frame double-tap guard; the keyboard's Done on sign-in skips the button's checks. | Guard before launching; gate Done like the button. |
| L14 | Low | `ui/nav/AppRoot.kt:371` | After checkout from a book opened outside the bookstore, Back loops between Orders and the finished checkout. | Pop up to the cart when opening Orders. |
| L15 | Low | `ui/account/AccountScreens.kt:597` | The "Account deleted" confirmation can never show: clearing the session discards the screen first. | Show a toast from the ViewModel before the session is cleared. |
| L16 | Low | `ui/account/ProfilePhoto.kt:11` | Picking the same photo again after a failed upload does nothing; the camera button stays enabled during an upload. | Reset the pick after handing it on; disable while busy. |
| L17 | Medium | `ui/manage/bookstore/BooksAdmin.kt:197` | The admin book form keeps the picked cover in `remember`: rotation loses it while "remove cover" survives, so Save can delete the cover. | Keep it in `rememberFormFiles` like every other admin form. |
| L18 | Low | `ui/manage/money/TransactionsScreens.kt:107`, `SettingsScreens.kt:151` | After rotation mid-request the success callback clears the old composition's fields; the form still holds the values and a second tap records a duplicate. | Clear the fields through state that survives rotation. |
| L19 | Low | `ui/manage/money/PaymentsScreens.kt:93` | The approve dialog can read "from null". | Fall back to "this student". |

### Performance

| ID | Sev | Where | Issue | Fix |
|---|---|---|---|---|
| P1 | Medium | `ui/manage/money/FinanceScreens.kt:157` | The Finance "Student" filter builds a dropdown with every student (the waiver finance screen already moved to a search). | Use `StudentFilter`. |

### Code quality and dead code

| ID | Sev | Where | Issue | Fix |
|---|---|---|---|---|
| Q1 | Low | `ui/home/HomeScreen.kt:133` | A second copy of `ResendState`/`resendOutcome` duplicates `ui/account/ResendState.kt`. | Use the account copy. |
| Q2 | Low | `ui/courses/LearnScreen.kt:368`; `ui/pay/PayScreen.kt:211`; `ui/bookstore/BookstoreScreens.kt:350`; `data/repo/AcademyRepository.kt:3-4,44,83`; `data/remote/AcademyApi.kt:5,18`; `data/staff/money/MoneyApi.kt:359` | Dead code: `ProgressRing`, two unused `LocalContext`, `clearCaches` (left from the in-app server switch), unused imports and a stale comment, an unused `students` query parameter. | Remove. |

`AuthRepository.removePhoto` / `DELETE auth/me/photo` is also unused, but it matches a website feature the app may
add ("Remove photo"); it is kept.

### Testing

| ID | Sev | Issue | Fix |
|---|---|---|---|
| T1 | Medium | No test of what `PayViewModel.send` posts (amount, coupon, month/year, waived fee, payment type). | Add `PayViewModel` payload tests. |
| T2 | Medium | No test of sign-out or of a stale profile (S1, S2). | Added with the fix. |
| T3 | Medium | No test of the admin payment approval flow. | Add a Compose + MockWebServer test: one Confirm sends exactly one approve, then reloads. |
| T4 | Low | No tests for the paging retry, cart restore failures, notification links, outside links. | Added with each fix. |

### Configuration

Checked and clean: secrets come from `local.properties` or the environment and are gitignored (`*.jks`,
`local.properties`); production release refuses to build without the upload key; cleartext traffic is off; the
FileProvider shares only `cache/documents/` and is not exported; R8 keeps the serializers; HTTP logging is BASIC in
debug and off in release. There is no database (no migrations). Only S3 needed a change.

## Implementation plan

Each line is one commit on `dev`, in this order (security and correctness first). Tests go in the same commit as
the fix they cover.

1. S1 + S2 — session: non-cancellable sign-out and writes, bounded revoke, ignore another user's profile; tests.
2. S3 — data extraction rules.
3. S4 — no response data in release logs.
4. L1, L2, L13 — double-submit guards (comments, fatwa questions, waivers, reviews, profile, sign-in); tests.
5. L3, L4, L5 — cart: restore after order, clear vs in-flight change, add-to-cart sets 1; tests.
6. L6, L7 — pay: stale restored coupon, UPI link matches the QR; T1 payload tests.
7. L9, L10 — links: phone apps for `mailto:`/`tel:`, notifications open their link; tests.
8. L11 — paging retry after a failed filter change; test.
9. L12, L16 — waiver form keys, profile photo re-pick.
10. L14, L15, L8 — navigation: checkout back stack, account-deleted message, apply → pay after rotation.
11. L17, L18, L19, P1 — admin: book cover across rotation, form reset, approve wording, finance student search.
12. T3 — payment approval test.
13. Q1, Q2 — duplicate and dead code.

Then: full build, lint, detekt, unit tests, staging release (R8) build; install the staging debug app on the
connected phone and walk the main flows; merge `dev` into `master` (fast-forward) and push both.

## Outcome

Every finding above was fixed, on `dev`, one commit per plan step:

| Commit | Findings |
|---|---|
| `2f4febf` | S1, S2, T2 — sign-out and session writes non-cancellable, revoke bounded to 5 s, another user's profile ignored |
| `22eb965` | S3 — data extraction and backup rules exclude everything |
| `935d7fc` | S4 — release builds log only the exception type |
| `bcf431c` | L1, L2, L13 — double-tap guards on comments, fatwa questions, waivers, reviews, profile, sign-in |
| `e808833` | L3, L4, L5 — cart restore after an order, sign-out vs an in-flight change, add-to-cart sets one copy |
| `1bffcbf` | L6, L7, T1 — stale restored waiver dropped, UPI payee as the QR's, payment payload tests |
| `116d825` | L9, L10 — `mailto:`/`tel:` to their apps, unsafe schemes ignored, notifications use the content link opener |
| `3949dd8` | L11 — paging: failed search/filter clears the stale list, Retry repeats what failed |
| `c447541` | L12, L16 — waiver list keyed, photo re-pick, camera off while uploading |
| `d4b766f` | L14, L15, L8 — Orders replaces cart and checkout, account-deleted toast, apply → Pay through state |
| `a1856ac` | L17, L18, L19, P1 — book cover in `rememberFormFiles`, `FormReset`, approve wording, finance student search |
| `8f1573a` | T3 — payment approval Compose test |
| `76ee516` | Q1, Q2 — one `ResendState`, dead code removed |
| `dd6fee4` | Keeps the new code within the detekt and lint limits (no baseline entries added) |

L7 was fixed for the payee name only. The UPI app link still sends no `tr` and keeps its 60-character note: some UPI
apps refuse a personal UPI ID that comes with a merchant-style transaction reference, so matching the QR code there
would risk failed payments. That is a judgement for whoever can try it with real UPI apps.

**Checks.** Build of both flavors, lint, detekt, 243 unit and Robolectric tests (226 before, 17 new), the
instrumented-test APK and the R8 staging release build: all pass.

**On the phone** (Samsung SM-A566E, staging debug build, already signed in to a staging developer account; nothing
was submitted): Home, notifications (both filters; a notification link opened My Courses), course page → Pay with a
waiver chosen, kept through rotating to landscape and back, Explore (fatwa, blog, bookstore and cart empty states),
the developer panel, Finance (the new student search: typing, picking and clearing), account and the fee-waiver form.
No crashes. Sign-out, account deletion and submitting forms were not tried on the phone, because they change the
staging account; the unit tests cover them.
