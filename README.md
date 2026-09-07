# TDM Insight — Native Android

Native Android rebuild of the TDM Insight Vancomycin TDM calculator, for
**CDE2313 Mobile Application Development**.

Platform: native Android · Kotlin · Jetpack Compose · Material 3

## How to open

1. Open Android Studio (Koala or newer recommended).
2. **File → Open** and select this `TDMInsight/` folder.
3. Let Gradle sync (it will download dependencies — needs internet the first time).
4. Run on an emulator or device (minSdk 24 / Android 7.0+).

## Architecture

```
UI (Compose screens) → Input State → Validation → TdmEngine → Result Model → Results UI
```

- **`engine/TdmEngine.kt`** — the calculation engine. Pure Kotlin, no
  Android/UI dependencies. Implements the three Vancomycin workflows
  (Pre, Post, Pre+Post), ported directly from the reference
  `runTdmCalc.js` equations (Cockcroft-Gault CrCl, Matzke population Ke,
  Sawchuk-Johnson two-point individualization). **Equations must be
  reviewed against lecturer-approved sources before submission.**
- **`engine/TdmEngine.kt` (`TdmValidator`)** — required-field, numeric,
  and cross-field validation (e.g. infusion duration vs. dosing interval,
  peak-must-exceed-trough, timing checks), separate from calculation logic.
- **`data/HistoryStore.kt`** — local-only calculation history, persisted as
  JSON in `SharedPreferences`. No backend/cloud dependency, per project scope.
- **`ui/AppViewModel.kt`** — shared state (history + last result) exposed to
  Compose screens via `StateFlow`.
- **`ui/screens/`** — `HomeScreen` (overview + history), `CalculatorScreen`
  (workflow selection + dynamic form per workflow), `ResultsScreen`
  (key results → PK parameters → recommendation → step-by-step explanation).
- **`ui/components/`** — reusable Compose pieces (number field, workflow
  selector, disclaimer banner, result cards, explanation stepper).

## Scope notes

Per the case study, this build intentionally has **no user authentication,
no cloud backend, and no analytics** — everything runs and stores data
locally on-device. The optional AI dosing-assistant chat feature from the
web prototype was also left out, since it depends on a cloud LLM backend
that's outside the core native-Android scope.

## Still to verify before submission

- [ ] Confirm the Vancomycin equations (Ke, Vd population estimate, target
      trough/AUC ranges) against your lecturer-approved reference — the
      constants here mirror the provided `runTdmCalc.js` but are **not**
      independently clinically validated.
- [ ] Add unit tests for `TdmEngine` (e.g. known input/output pairs, edge
      cases like division-by-zero guards) — required for the "Testing &
      Debugging" rubric criterion.
- [ ] Add app icon/branding polish, screenshots for the README, and the
      `docs/`, `apk/`, `presentation/`, `ai/` folders required by the
      assessment's repository structure.
