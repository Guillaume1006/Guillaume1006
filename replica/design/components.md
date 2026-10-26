# Components: standalone 5-band equalizer

Stack is native Android (Jetpack Compose), so the Tailwind mapping from the
skill is replaced by `android/Color.kt` and `android/Type.kt`, generated from
`tokens.json`. `tokens.css` is kept for a browser preview of the design only.

Evidence: screen S02 (the user's photo of a screen; origin not confirmed as
Cardo) and the profile list S01 (not seen). Everything marked *spec* is
ours, not measured. Icons: Material Symbols or Lucide (open); no icon is
taken from the reference. Labels are written fresh.

Measured on S02 (ratios of the 1080 x 592 photo): bar width 10% of screen
width, column pitch 21%, bar height 67% of screen height. Five columns, each
stacking: band label, chevron up, bar (with the 8 / 0 / -8 scale beside it
and the gain value centred), chevron down.

## EQ band

```
EQ band
  variants  one per frequency: 100 Hz, 300 Hz, 1 kHz, 3 kHz, 10 kHz
  states    default (value 0), focused (label bold, chevrons accent, as on 100 Hz),
            boosted (gain > 0), cut (gain < 0), disabled (equalizer off) [spec]
  layout    column: label, chevron up, bar, chevron down; bar width 10% of screen
  tokens    bar bg surface, label text/xs..sm, value text/lg, focus accent
  a11y      one node per band exposing role "slider", value "+3 dB", range -8..+8,
            actions: increase, decrease; label read as "300 hertz"
  used on   S02
```

## Chevron button

```
Chevron button
  variants  up, down
  sizes     touch target 48dp minimum, glyph about 24dp
  states    default (text-muted), focused (accent, seen on S02), pressed [spec],
            disabled at the limit: up at +8, down at -8 [spec]
  behaviour one tap = one dB; hold repeats after 400 ms, every 100 ms [spec]
  tokens    glyph text-muted, focus accent, radius pill for the ripple
  a11y      content description "Raise 300 hertz by 1 dB"; disabled state announced
  used on   S02
```

## Gain bar

```
Gain bar
  variants  none
  states    zero (value centred on the 0 line), positive, negative [spec: fill from
            the 0 line up or down, accent at 40% opacity]
  tokens    bg surface, radius sm, fill accent
  a11y      decorative; the band slider node carries the value
  used on   S02
```

## Gain scale

```
Gain scale
  content   labels 8, 0, -8 at the left of each bar (as seen)
  tokens    text/xs, text-muted
  a11y      hidden from screen readers
  used on   S02
```

## Profile row (S01, not seen)

```
Profile row
  variants  On, Off, Bass Boost, High Volume, Vocal (names from the recon), custom [spec]
  states    selected (accent mark + bold), unselected, disabled while no audio route [spec]
  tokens    surface, text, accent for the selected mark
  a11y      radio group semantics, "selected" announced
  used on   S01
```

## Equalizer on/off switch [spec]

```
Switch
  states    on, off, loading (while the audio route starts), error (route failed)
  tokens    accent when on, border-input when off
  a11y      real switch role, label "Equalizer", error text read aloud
  used on   S02 header, S01
```

## Status banner [spec]

For the feasibility result (see architecture.md): "Audio route not available on
this phone". Variants: info, warning, error (danger token). Role alert.

## Not built here

The primitives (Compose components in a preview screen) are not built: no
Android SDK in this environment. They are the first task of `/replica-build`,
after the milestone 0 spike. The design check is a screenshot of S02 next to
the reference.
