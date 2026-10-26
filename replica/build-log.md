# Build log

Environment limit: no Android SDK and Google's Maven is blocked, so the
`:app` module was never compiled or run, and there are no screenshots
(`replica/clone-screens/` is empty). Only `:core` (plain Kotlin) is built and
tested: `cd equalizer && gradle :core:test` runs 13 tests, all green.

| ID | date | status | what is missing | notes |
| --- | --- | --- | --- | --- |
| shell | 2026-10-26 | partial | never compiled | two modules; `:app` is only included when an Android SDK exists |
| S02 equalizer | 2026-10-26 | partial | compile, run, screenshot, compare with the reference | 5 bands, chevrons, value, scale, switch, reset; logic tested in `:core` |
| S01 profiles | 2026-10-26 | partial | the original screen was never seen | five radio rows built from the recon's list |
| engine (route A) | 2026-10-26 | partial | never run | `DynamicsProcessing` on the output mix; the spike must say if any phone honours it |

Known gaps against the definition of done:
- Component sizes use raw dp values (4, 12, 16, 48) in `EqScreen.kt` and
  `ProfileScreen.kt`; they should come from tokens (`EqSpace`) once compiled.
- No skeleton or loading state: nothing loads yet (in-memory data).
- Mobile width 390px and keyboard checks not done: nothing ran.
- The profile gains, loudness stage and band edges are our guesses.
- Foreground service, needed for an effect that outlives the app, is not
  written: it depends on which route the spike picks.

Harder than expected: "equalize everything" is not a screen, it is the
spike. The UI is the easy part; nothing here proves any sound changes.
