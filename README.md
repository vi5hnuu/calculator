# MathPro

A nine-mode Android calculator, built with Jetpack Compose.

Rewritten from a Flutter 4-function calculator to native Kotlin/Compose. Same
`applicationId`, so it installs as an in-place update; the release APK is **1.9 MB**
against Flutter's typical 8–15 MB.

## Modes

| Mode | What it does |
| --- | --- |
| Standard / Scientific | Full expression engine, 18 functions, memory keys, deg/rad, tape |
| Programmer | 32-bit signed integers, live HEX/DEC/OCT/BIN, bitwise ops |
| Percent | Tip & bill split, plus `% of` / `+ %` / `− %` tools |
| Worksheet | Multi-line sheet with named variables and `ans` |
| Convert | 8 categories of fixed physical units, incl. temperature |
| Graph | Plots `y = f(x)` on a Compose Canvas, with zoom |
| Finance | Loan/EMI with total interest and payable |
| Date | Calendar-aware difference in days, weeks, months, Y·M·D |

Plus a searchable, pinnable history drawer, five accent colours, and a light/dark/system
theme toggle. Settings and history survive restarts.

## Architecture

Single `:app` module with strict internal layering. Nothing above `domain` knows that
history is Room or that settings are DataStore; the maths engine has no Android dependency
at all and is tested as plain JVM code.

```
core/math/           Lexer → Preprocessor → ShuntingYard → RpnEvaluator, NumberFormatter
core/designsystem/   theme tokens, typography, shared components
domain/model/        immutable models
domain/repository/   interfaces only
domain/usecase/      per-mode logic (converter, loan, dates, worksheet, programmer)
data/local/          Room + DataStore
data/repository/     implementations
feature/<mode>/      Screen + ViewModel + UiState
```

MVVM with unidirectional data flow: each ViewModel exposes one immutable `UiState` via
`StateFlow`; screens are stateless composables. DI is Hilt.

The package boundaries are drawn so any `feature/` or `core/math` can be lifted into its own
Gradle module later without touching callers — deliberately not done up front, because nine
modes and ~6k LOC do not pay for the build complexity.

## Build

Requires JDK 17+ (Android Studio's bundled JDK works). Everything else comes from the
wrapper.

```bash
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleDebug
./gradlew assembleRelease      # R8 + resource shrinking
```

Release signing is opt-in: drop a `key.properties` at the repo root with `keyAlias`,
`keyPassword`, `storeFile`, `storePassword`. Without it `assembleRelease` still produces an
unsigned APK.

### A note on the toolchain

`gradle.properties` sets `android.builtInKotlin=false` and `android.newDsl=false`. AGP 9
ships its own Kotlin, but KSP — which Room and Hilt both need — is not yet compatible with
it, and the standalone `kotlin-android` plugin cannot read AGP 9's new DSL. Both flags come
out once KSP catches up; AGP 10 removes them either way.

## Testing

Everything runs on the JVM — `./gradlew testDebugUnitTest` needs no emulator. The Compose
keypad tests use Robolectric.

The guiding rule is that **a wrong number is worse than an error**. Where the two conflict,
the app fails loudly. Cases pinned by regression tests, each of which was a real bug caught
during review:

| Case | Why it is pinned |
| --- | --- |
| `0.1 + 0.2` → `0.3` | The formatter rounds at 10 dp; this is what lets the engine stay on `Double` instead of paying for `BigDecimal` arithmetic. |
| `2^-3` → `0.125` | The design mock's own engine returns NaN — it pops eagerly on prefix operators. |
| `round(2.5)` → `3` | `kotlin.math.round` breaks ties to even and gives 2. |
| Memory recall of `1e15` | Recalling via a *display* format re-lexed `1e+15` as `1 × e + 15` = 17.7. Memory uses `toLiteral`, which never emits an exponent or a comma. |
| `sin(30)` in DEG → `0.5` | The angle unit has to reach memory and worksheet too, not just the display. |
| Unbound name → error | Defaulting to 0 (as the mock does) turns a typo like `qtyy*3` into a confident `0`. |
| `1 << 32` → `0` | The JVM masks shift distances to 5 bits, so this returns `1` — the operand comes back untouched. |
| `+2+3`, `3--3`, `2---3`, `+0++2**3` | The cases the previous Dart engine had to be patched for. |

Converter factors are asserted against exact definitions (1 in = 25.4 mm) and for
self-consistency (1 ft² must equal (1 ft)²), not against the mock's rounded constants.

## Ads

A single anchored adaptive AdMob banner sits at the very bottom, below the keypad — it never
overlaps a key or a result, and it hides while the keyboard is open. It's the least intrusive
way to monetise a calculator.

The ad unit is chosen by build type, so **debug builds can never serve a live ad**:

| Build | Banner unit |
| --- | --- |
| debug | `ca-app-pub-3940256099942544/9214589741` — Google's public test unit |
| release | the live unit, from `BuildConfig.BANNER_AD_UNIT_ID` |

This matters: tapping your own live ads while developing gets the AdMob account suspended.
The test unit only ever returns "Test Ad" placeholders.

### Consent (UMP / GDPR)

`AdsConsentManager` runs Google's User Messaging Platform flow from `MainActivity` **before**
any ad loads: it requests the consent status, shows the GDPR form if one is required, and only
then initialises the Mobile Ads SDK. The banner is gated on `canRequestAds` and never composes
until consent permits it.

- **Release** is strict — a real ad shows only once UMP confirms consent (or that none is
  needed for the user's region).
- **Debug** additionally shows ads when consent can't be resolved (e.g. the consent endpoint
  is unreachable on a cold emulator), because debug only ever serves Google's *test* ads.

**Before publishing** you still need to, in the AdMob console: create a **GDPR consent message**
(Privacy & messaging) — without it the EEA form has nothing to display — add a **privacy
policy** URL to the Play listing, and put an **`app-ads.txt`** on your developer domain. To
exercise the EEA form on a device during development, see the commented snippet in
`AdsConsentManager.gatherConsentThenInitialize`.

## Deliberate decisions

- **`%` is modulo**, not percent — `10%3` = 1. Matches the mock and the previous Dart app.
  The percent use-cases (tip, discount, percent-of) have their own mode.
- **No currency conversion.** Every other unit here is a fixed physical definition that
  cannot go stale. Exchange rates would ship already wrong and drift daily; fetching live
  ones needs network access and still falls back to stale figures offline.
- **`KB` = 1024 B**, as operating systems report it. Said out loud in the UI, because SI
  says 1 kB = 1000 B.
- **Implicit multiplication is left-to-right**: `1/2pi` is `(1/2)×pi`. Matches the mock;
  contested between calculator vendors.
- **Portrait only**, as before.

## Design reference

Ported from `MathPro Calculator.dc.html`. The dark palette is taken from it directly; the
light theme is derived from the same tokens, since the mock is dark-only.
