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
| S02 | Equalizer | unknown; reference screenshot supplied by the user (`screens/S02-equalizer-reference.png`) | adjust 5 frequency bands | 5 vertical bars labelled 100Hz, 300Hz, 1kHz, 3kHz, 10kHz; scale 8 / 0 / -8 on each; up and down chevrons above and below each bar; gain number centred in each bar | default (all 0); focus (100Hz label bold, its chevrons teal) |

Screens that certainly exist but were not studied: device list and pairing,
Quick Access home, settings. Out of the slice.

## Flows

```
F01 Pick a sound profile      UNVERIFIED
    S01 Audio Settings -> tap a profile
    happy path clicks: unknown (likely 2 to 3)
    edge: no headset connected, headset without JBL kit, disconnect mid-change

F02 Tune the equalizer         SEEN on S02 only; the route to it is unknown
    S02 Equalizer -> tap up or down chevron on a band (1 step per tap)
    happy path clicks: unknown (one per step of gain)
    edge: -8 and +8 limits, focus moving between bands, reset to 0
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| Profile list row | 5 profiles (ON, OFF, Bass Boost, High Volume, Vocal) | selected, unselected; others unknown | S01 |
| EQ band | 1 of 5 (100Hz, 300Hz, 1kHz, 3kHz, 10kHz) | value 0, focused (bold label, teal chevrons); non-zero not seen | S02 |
| Chevron button | up, down | default, focused (teal); pressed and limit-disabled not seen | S02 |
| Gain scale | labels 8, 0, -8 at the left of each bar | static | S02 |

## Inferred data model

```
SoundProfile  id (on | off | bass_boost | high_volume | vocal), label
              evidence: source 1
              confidence: medium
DeviceSettings  device_id, selected_profile
              evidence: none; the setting is applied on the headset
              confidence: guess
EqSetting     gains[5] (int, -8..8) for 100, 300, 1000, 3000, 10000 Hz
              evidence: S02 screenshot (labels, scale, values)
              confidence: medium for shape; guess for how it is stored or sent
EqPreset      name, EqSetting; evidence: none; confidence: guess
```

Relationships: GUESS: Device 1-1 DeviceSettings, DeviceSettings n-1 SoundProfile.

## Feature matrix

See `features.csv`. Must: 9, should: 4, could: 3, skip: 6. Rows are
unverified, except the S02 rows, which come from the user's screenshot.

## Out of scope (cannot or should not be cloned)

- Pairing and the Bluetooth link to Cardo units: proprietary protocol, private API.
- The audio processing itself: it runs on the headset chip. A clone only sends a setting.
- JBL profile activation (QR code on the kit): hardware and licence tied.
- Dynamic Mesh Communication intercom, firmware updates, FM radio: outside the slice.
- Cardo's name, logo, JBL branding and copy.

## Size

Screens 2, flows 2, entities 4 (2 guesses). Hard parts: anything touching the
headset (no public protocol); real-time audio processing on the phone if the
equalizer is standalone. Size: the UI is S, but equalizing all of the phone's audio
(user decision) is the hard part and is unproven: M at best, after a
feasibility spike (see architecture.md); not sizeable as a Cardo-compatible clone until the protocol
question is answered.

## Open questions for the user

1. Is S02 from Cardo Connect, or another app or device? The look (dark
   grey, vertical bars, chevrons) is not confirmed as Cardo's.
2. Send a screenshot of S01 (the profile list) and how S02 is opened.
3. Is the goal a Cardo-compatible app or a standalone equalizer for the phone?
4. Confirm the target ("a product to sell").
