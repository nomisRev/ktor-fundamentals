# A `TypeHint` or `Warning` on a slide keeps its `DrawnAnnotation`s from settling on a direct load

**Status:** open
**Repository:** slidev-theme-kotlin 0.13.0 (observed from ktor-fundamentals)

## Problem

On a slide that wraps its code fence in `<TypeHint>` or `<Warning>` and also
carries a `<DrawnAnnotation>`, the annotation never draws when the slide is
the first one the page loads (a deep link, a reload, or `slidev export`
jumping to it). The pill or the yellow highlight renders, the annotation's
mark and label are measured and placed at the right spot, but the component
stays at `settled: false`, so its group never gets `is-active` and the ink
stays at opacity 0. Leaving the slide and coming back does not recover it.

The same slide draws correctly when it is reached from an adjacent slide that
was loaded first: navigating from slide 9 to slide 10 settles within about
500 ms. A `<SmartCast>` wrapper does not trigger the problem (lesson 5,
"Handlers depend on the service" settles and exports fine either way).

`slidev export` shows the consequence: with `--range 11,10` slide 10 exports
complete, with `--range 10,104` or `--range 104,10` slide 10 is captured as
Slidev's "Loading slide…" fallback.

## Diagnosis so far

- `DrawnAnnotation.settleAfterAnimations` awaits `animation.finished` for
  every running or pending animation under the slide root. On a navigation,
  the sampled animations include the pill's `type-hint-enter-active`
  transition as `running(pending)` for about 150 ms and then it runs.
- Calling the component's `settleAfterAnimations(settleRun)` by hand from
  the console on the stuck slide settles it immediately and the ink appears,
  so the target search and the geometry are fine; only the first settle pass
  is abandoned.
- Likely cause: on a direct load the message element's enter transition is
  created while its host is not yet laid out, so the `Animation` stays
  `pending` and its `finished` promise never resolves, or the run is
  superseded (`settleRun` bumped by the highlight component's own mutation)
  and the replacement run never starts.

## Repro in this deck

```bash
npm run dev
open http://localhost:3030/10      # "A module is an extension of Application"
```

The two `this:` pills show; the label "One piece of the application, apart
from `main`" does not. Press left, then right: still absent. Open
`http://localhost:3030/9` and press right: the label draws.

Slides affected here: lesson 1 slide 10, lesson 3 slides 55, 57, 60, 61,
lesson 6 slide 115, lesson 7 slides 135 and 158 (`Warning`), lesson 8 slide
167, lesson 9 slide 196.

## Acceptance

- [ ] A slide with `TypeHint` or `Warning` and a `DrawnAnnotation` draws the
      annotation on a direct load and in `slidev export --range <n>`.
