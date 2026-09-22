---
theme: kotlin
favicon: /ktor.svg
fonts:
  sans: JetBrains Sans
  mono: JetBrains Mono
  provider: none
  local:
    - JetBrains Sans
    - JetBrains Mono
transition: fade
layout: cover
class: fundamentals-cover
canvasWidth: 1440
aspectRatio: 16/9
colorSchema: both
highlighter: shiki
themeConfig:
  kodee: greeting
  drawnAnnotation:
    connect: false
  siteUrl: https://nomisrev.github.io/ktor-fundamentals/
  snippets:
    dir: src/main/kotlin/presentation/snippets
    package: presentation.snippets
    imports:
      - presentation.support.*
kodee: welcome
---

<!-- @formatter:off -->

# Ktor Fundamentals

## From first route to production HTTP services

Ktor 3.6.0 · Kotlin 2.4.20 · kotlinx.serialization 1.11.0

---
layout: intro
class: section-slide agenda-slide
kodee: wave
---

# Today’s route

1. Your first Ktor server
2. Server and configuration
3. Requests and responses
4. Typed parameters and HTML
5. Sessions and static content
6. Talking to other services
7. WebSockets and OpenAPI
8. Status pages, testing and metrics
9. Authentication and authorization
10. Deployment

---
src: ./lessons/lesson-1.md
---
---
src: ./lessons/lesson-2.md
---
---
src: ./lessons/lesson-3.md
---
---
src: ./lessons/lesson-4.md
---
---
src: ./lessons/lesson-5.md
---
---
src: ./lessons/lesson-6.md
---
---
src: ./lessons/lesson-7.md
---
---
src: ./lessons/lesson-8.md
---
---
src: ./lessons/lesson-9.md
---
---
src: ./lessons/lesson-10.md
---
---
src: ./lessons/closing.md
---

<!-- End of imported slides. -->
