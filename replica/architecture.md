# Architecture: standalone 5-band equalizer (a rebuild of the sound settings of Cardo Connect)

Based on a **partial, unverified** recon (`recon.md`). Scope decision from the
user: a standalone phone-side equalizer in the style of screen S02, not an
app that talks to Cardo headsets (no public protocol, private API out of
scope). Target: a product to sell (assumed, still unconfirmed).

## Stack

| layer | choice | why |
| --- | --- | --- |
| mobile | Kotlin, Jetpack Compose, native Android | the app is a thin UI over Android audio effects; the user asked for Android; Expo would add a bridge for no gain |
| audio engine | `android.media.audiofx.DynamicsProcessing` (API 28+), with `LoudnessEnhancer` for the High Volume profile | DynamicsProcessing lets us set our own band frequencies (100, 300, 1k, 3k, 10k Hz). The classic `Equalizer` class exposes the device's own fixed bands, which will not match S02 |
| playback target | a session id we control, with the app's own player (Media3 ExoPlayer) in the first version | effects on the global session (id 0) are deprecated and unreliable on recent Android; this is the biggest risk below |
| database | Room (SQLite), on device | one user, no sync needed; presets and settings only |
| settings | same Room tables (`app_settings`) | no second store |
| web / server | none | no flow needs one |
| auth | none | no accounts |
| payments | none in this slice | selling it (Play Billing) is a later milestone, not in `features.csv` |
| email, jobs | none | nothing to send or schedule |
| hosting | Google Play (release), GitHub Actions for build and tests | |
| UI tokens | from `/replica-design`, own colours and icons | clean-room: no Cardo assets |

## Schema

Tables: 4 (`eq_presets`, `eq_preset_bands`, `eq_current`, `app_settings`).
Access rules: none needed (single user, on device). Full SQL in
`replica/schema.sql`. Choices:

- Gains are whole dB, `check (gain_db between -8 and 8)`, from S02.
- Bands are fixed to 100, 300, 1000, 3000, 10000 Hz by a check constraint.
- `eq_current` holds the live sliders, so the screen can show a setting that
  matches no preset.
- `app_settings` is a one-row table (`check (id = 1)`).
- The five built-in profiles (On, Off, Bass Boost, High Volume, Vocal) are
  seeded. Their gain values are ours, to tune by ear; Cardo's are unknown.
- Deleting a custom preset sets `active_preset_id` to null, never cascades.

Mapping from the recon entities: SoundProfile and EqPreset are
`eq_presets` + `eq_preset_bands`; EqSetting is `eq_current`; DeviceSettings
becomes `app_settings` (there is no device).

## API

No server, so no routes. The app's internal interfaces, one per flow:

| interface | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `ProfileRepository.list()` | lists built-in and custom profiles | UI | none | profiles | F01 |
| `ProfileRepository.select(id)` | applies a profile to the live bands and the engine | UI | preset id | new live setting | F01 |
| `EqualizerEngine.setBandGain(hz, dB)` | changes one band in real time | UI | band, -8..8 | applied value | F02 |
| `EqualizerEngine.setEnabled(on)` | bypasses the effect (profile Off) | UI | boolean | state | F01 |
| `EqualizerEngine.setLoudness(dB)` | extra gain for High Volume | profile | 0..12 | applied value | F01 |
| `EqRepository.resetFlat()` | sets every band to 0 | UI | none | live setting | F02 |
| `ProfileRepository.saveCustom(name)` | saves the live bands as a preset | UI | name | new preset | F02 |

Webhooks in/out: none. Jobs: none (the engine restores the last setting when
playback starts).

## The parts that bite

- **Where the effect is attached (biggest risk).** An equalizer only changes
  sound that goes through an audio session it is attached to. Effects on the
  global output (session 0) are deprecated and not honoured on many recent
  devices, and other apps' audio is not ours to process. First version: the
  app plays audio itself through ExoPlayer and attaches the effect to that
  session. A system-wide equalizer is a different, riskier product.
- **Band frequencies.** Only `DynamicsProcessing` takes custom cutoffs. It
  needs Android 9 (API 28) or newer. The minSdk follows from that.
- **Device differences.** Some phones and Bluetooth routes ignore or alter
  effects. Test on at least three devices and one Bluetooth headset.
- **Gain and clipping.** +8 dB on several bands plus High Volume loudness can
  clip. Add a limiter stage and cap the total boost.
- **Real-time updates.** Chevron taps change one dB step each; apply them
  without rebuilding the effect, or the sound drops out on every tap.
- **Audio focus and lifecycle.** The effect must be released with the
  player, and restored from `eq_current` on the next start.
- **No headset protocol.** Anything that must run on a Cardo unit is out:
  the profile DSP lives on the headset chip.
- **Time zones, idempotency, races:** not applicable (no server, no clock
  logic). The only race is two quick taps; serialize them on one thread.
- **Legal.** Do not reuse Cardo's name, logo or JBL branding; the rebrand
  and trademark checks happen in `/replica-brand`.

## Build order

1. **Vertical slice.** S02 with the 5 bars, chevrons and number readout;
   tables `eq_current` and `app_settings`; `EqualizerEngine.setBandGain`;
   one hard-coded test track playing through ExoPlayer. Proves the audio
   path on a real device before anything else.
2. **Must-haves** (8): S01 profile list; profiles On, Off, Bass Boost;
   -8..+8 range, chevrons, numeric readout, 5 bands; tables `eq_presets`,
   `eq_preset_bands`; `ProfileRepository.list/select`.
3. **Should-haves** (4): Vocal and High Volume profiles (loudness stage),
   restore last setting on start, reset to flat.
4. **Could-haves** (3): save a custom preset, ambient-noise auto volume
   (needs a microphone permission and a design decision), audio mixing
   (no meaning without intercom; probably drop).
5. **Fixes from replica-entrepreneur:** once it has run on real reviews.

## Open questions

- Is the first version allowed to play audio itself (own player), or must it
  equalize other apps' sound? That decides the whole audio design.
- Confirm that S02 comes from Cardo Connect, and send S01 (profile list).
- Confirm the target ("a product to sell") and the minimum Android version.
