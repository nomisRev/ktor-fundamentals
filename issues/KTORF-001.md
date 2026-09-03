# Publish ktor-fundamentals on GitHub Pages with the static content and analytics

**Status:** open
**Repository:** ktor-fundamentals
**Depends on:** slidev-theme-kotlin release (see ../../slidedev-theme-kotlin/issues/002)

## Problem

The deck exists locally (two commits, lesson 1 written, lesson 2 in progress)
but has no git remote, so nothing is deployed and
https://nomisrev.github.io/ktor-fundamentals/ does not exist. The headmatter
already sets `siteUrl` to that address and `deploy.yml` already builds with
`--router-mode hash` after `./gradlew build`, so the deployment is ready
except for the pieces below. `package.json` depends on the theme through
`file:../slidedev-theme-kotlin`, which cannot resolve in the GitHub Action.

## Work

- [ ] Create the `nomisRev/ktor-fundamentals` repository, push, and enable
      GitHub Pages with the Actions source.
- [ ] Depend on the released theme version instead of `file:`.
- [ ] Add `info:` (one paragraph) and `themeConfig.analytics.goatcounter: nomsrev`
      to the headmatter.
- [ ] Commit the generated snippets under `src/main/kotlin/presentation/snippets/`
      together with the lesson that produced them (lesson 2 is uncommitted).
- [ ] Register the deck on the blog: add `src/content/talks/ktor-fundamentals.md`
      in new-blog with `slides: https://nomisrev.github.io/ktor-fundamentals/`.
      That single field adds the deck to the blog's `robots.txt`,
      `sitemap-index.xml`, `llms.txt`, and the talk page.
- [ ] After the first deploy, verify `sitemap.xml`, `handout/`, `llms.txt`, and
      `llms-full.txt` return 200 under `/ktor-fundamentals/`.

## Acceptance

- [ ] The deck, its handout, and its sitemap are live and listed by the blog.
- [ ] Slide paths appear in the GoatCounter dashboard under `/ktor-fundamentals/`.
