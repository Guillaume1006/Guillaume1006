# Recon map: Cardo Connect (Android), PARTIAL

Scope: Audio Settings only: sound profiles and, if it exists, an equalizer
For: a product to sell (assumed: the user did not confirm)
Date: 2026-10-26

**Status: partial and unverified.** The network blocked the primary pages
(cardosystems.com, apps.apple.com, cyclenews.com: proxy 403). Everything below
comes from web search summaries, not from reading the pages or the app. No
screen was seen. Every row is tagged `UNVERIFIED` or `GUESS`. Redo this with
the pages or the user's own account before `/replica-architect`.

## Sources

| # | source | URL | notes |
| --- | --- | --- | --- |
| 1 | Cardo marketing page | https://cardosystems.com/pages/sounds-like-summer/ | via search summary only. Lists 5 profiles under Audio Settings |
| 2 | press, JBL 45mm Audio Set (2019) | https://www.cyclenews.com/2019/03/article/cardo-systems-announces-audio-set-new-45mm-accessory-with-sound-by-jbl | via search summary only. 3 profiles: Standard, Bass Boost, Vocal |
| 3 | Cardo support pages | https://cardosystems.com/pages/support/smarth | seen in results, content not read |
| 4 | Play listing mirrors | https://www.similarweb.com/app/google/com.cardo.smartset/ , https://appfollow.io/android/cardo-connect/com.cardo.smartset?country=es | third-party mirrors, not play.google.com. Mention "Audio Mixing" in release notes |
| 5 | App Store listing | https://apps.apple.com/us/app/-/id1333553210 | listed in results, not read |

## Core loop

UNVERIFIED: the user pairs a Cardo headset, opens Audio Settings and picks the
sound profile that suits the ride. (The app's paid value is the hardware; the
app itself is free.)

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Audio Settings | app, then Audio Settings (source 1) | choose a sound profile | profile list, selected marker | none seen |
| S02 | Equalizer | GUESS: not found in any source | adjust bands | unknown | none seen |

Screens that certainly exist but were not studied: device list and pairing,
Quick Access home, settings. Out of the slice.

## Flows

```
F01 Pick a sound profile      UNVERIFIED
    S01 Audio Settings -> tap a profile
    happy path clicks: unknown (likely 2 to 3)
    edge: no headset connected, headset without JBL kit, disconnect mid-change
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| Profile list row | 5 profiles (ON, OFF, Bass Boost, High Volume, Vocal) | selected, unselected; others unknown | S01 |

## Inferred data model

```
SoundProfile  id (on | off | bass_boost | high_volume | vocal), label
              evidence: source 1
              confidence: medium
DeviceSettings  device_id, selected_profile
              evidence: none; the setting is applied on the headset
              confidence: guess
EqPreset      bands[], gains[]
              evidence: none
              confidence: guess
```

Relationships: GUESS: Device 1-1 DeviceSettings, DeviceSettings n-1 SoundProfile.

## Feature matrix

See `features.csv`. Must: 4, should: 3, could: 3, skip: 6. Rows are
unverified.

## Out of scope (cannot or should not be cloned)

- Pairing and the Bluetooth link to Cardo units: proprietary protocol, private API.
- The audio processing itself: it runs on the headset chip. A clone only sends a setting.
- JBL profile activation (QR code on the kit): hardware and licence tied.
- Dynamic Mesh Communication intercom, firmware updates, FM radio: outside the slice.
- Cardo's name, logo, JBL branding and copy.

## Size

Screens 2 (1 unconfirmed), flows 1, entities 3 (2 guesses). Hard parts:
anything touching the headset (no public protocol); an equalizer that is not
confirmed to exist. Size: S as a phone-side equalizer app; not sizeable as a
Cardo-compatible clone until the protocol question is answered.

## Open questions for the user

1. Does the equalizer exist in your Cardo Connect? Screenshot S01 and S02.
2. Is the goal a Cardo-compatible app or a standalone equalizer for the phone?
3. Confirm the target ("a product to sell").
