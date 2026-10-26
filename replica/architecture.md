# Architecture: standalone 5-band equalizer (a rebuild of the sound settings of Cardo Connect)

Based on a **partial, unverified** recon (`recon.md`). Scope decisions from the
user: a standalone phone-side equalizer in the style of screen S02, not an
app that talks to Cardo headsets (no public protocol, private API out of
scope); and it must equalize all of the phone's audio, not only its own
playback. Target: a product to sell (assumed, still unconfirmed).

## Stack

| layer | choice | why |
| --- | --- | --- |
| mobile | Kotlin, Jetpack Compose, native Android | the app is a thin UI over Android audio effects; the user asked for Android; Expo would add a bridge for no gain |
| audio engine | `android.media.audiofx.DynamicsProcessing` (API 28+), with `LoudnessEnhancer` for the High Volume profile | DynamicsProcessing lets us set our own band frequencies (100, 300, 1k, 3k, 10k Hz). The classic `Equalizer` class exposes the device's own fixed bands, which will not match S02 |
| playback target | **all audio on the phone** (user decision). Three official routes, chosen by a spike on real devices (milestone 0): A) effect on the global output mix (session 0); B) effect attached per audio session, learned from the `OPEN_AUDIO_EFFECT_CONTROL_SESSION` broadcasts that players send; C) capture other apps' audio with `AudioPlaybackCapture` (API 29+, needs `MediaProjection`), then re-play it through our own equalized player | no route is known to work everywhere (see the first risk below); a foreground service keeps the effect alive |
| foreground service | Android foreground service with a persistent notification | the effect or the capture must survive the app being closed |
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

- **Equalizing everything (biggest risk, now the core of the product).** An
  effect only changes sound that goes through the session it is attached to.
  From general knowledge of Android, to be confirmed by the spike, not checked
  here: effects on the global mix (route A) are deprecated and ignored on
  many recent phones; per-session attach (route B) only reaches apps that
  broadcast their session; capture (route C) adds latency, needs a visible
  screen-capture consent, and apps can opt out of being captured (protected
  or DRM audio comes back silent). Calls and some system sounds are likely
  out of reach on every route. Expect "most music and video apps", not
  "everything", and say so in the store listing. If no route works on the
  phones that matter, the product does not exist as imagined: find out in
  milestone 0, before any screen is built.
- **Play policy.** Foreground service type, screen-capture consent text and
  the audio-effect permission (`MODIFY_AUDIO_SETTINGS`) must be declared and
  justified at Play review. A capture-based app gets extra scrutiny.
- **Battery and latency.** Route C re-renders all audio: measure battery use
  and delay (video lip-sync) on a mid-range phone.
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

0. **Spike, no UI.** One screen with a single toggle and a fixed +8 dB bass
   boost, tried on routes A, B and C, on at least three phones (different
   makers, Android 12 to 15) and one Bluetooth headset, with a music app, a
   video app and a streaming app. Output: a table of route x device x app
   showing what is heard. Go or no-go on the product, and the route to build.
1. **Vertical slice.** S02 with the 5 bars, chevrons and number readout;
   tables `eq_current` and `app_settings`; `EqualizerEngine.setBandGain`
   on the route the spike chose, for a music app you already have installed.
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

- Answered: the app must equalize all audio (not its own player). Which
  phones and Android versions must it work on? That sets the spike's targets.
- Confirm that S02 comes from Cardo Connect, and send S01 (profile list).
- Confirm the target ("a product to sell") and the minimum Android version.
