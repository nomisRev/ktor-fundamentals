<script setup>
defineProps({
  /** Show the Ktor mark on the server card; `false` draws a plain client/server exchange. */
  showKtor: { type: Boolean, default: true },
})
</script>

<template>
  <div
    class="http-exchange"
    role="img"
    :aria-label="showKtor
      ? 'A client sends the request GET /hello to a Ktor server, which answers 200 OK.'
      : 'A client sends the request GET /hello to a server, which answers 200 OK.'"
  >
    <svg class="http-exchange__wires" viewBox="0 0 1056 320" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <marker id="http-exchange-request-head" viewBox="0 0 12 12" refX="8" refY="6" markerWidth="16" markerHeight="16" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M1 1 L11 6 L1 11 Z" fill="#7954f6" />
        </marker>
        <marker id="http-exchange-response-head" viewBox="0 0 12 12" refX="8" refY="6" markerWidth="16" markerHeight="16" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M1 1 L11 6 L1 11 Z" fill="#eb55e6" />
        </marker>
      </defs>
      <path class="exchange-line exchange-line--request" d="M262 118 H 790" marker-end="url(#http-exchange-request-head)" />
      <path class="exchange-line exchange-line--response" d="M794 202 H 266" marker-end="url(#http-exchange-response-head)" />
    </svg>

    <div class="exchange-card exchange-card--client">
      <span class="exchange-card__title">Client</span>
      <span class="exchange-card__hint">browser · app · service</span>
    </div>

    <div class="exchange-message exchange-message--request">GET /hello</div>
    <div class="exchange-message exchange-message--response">200 OK</div>

    <div class="exchange-card exchange-card--server" :class="{ 'exchange-card--ktor': showKtor }">
      <picture v-if="showKtor" class="exchange-card__logo" aria-hidden="true">
        <source media="(prefers-color-scheme: dark)" srcset="/ktor_dark.svg">
        <img src="/ktor.svg" alt="">
      </picture>
      <span class="exchange-card__title">{{ showKtor ? 'Ktor' : 'Server' }}</span>
      <span class="exchange-card__hint">{{ showKtor ? 'the server' : 'any HTTP server' }}</span>
    </div>
  </div>
</template>

<style scoped>
.http-exchange {
  --exchange-ink: var(--fundamentals-ink, #1f2023);
  --exchange-purple: var(--fundamentals-purple, #7954f6);
  --exchange-pink: var(--fundamentals-pink, #eb55e6);
  --exchange-surface: rgb(255 255 255 / 90%);
  --exchange-muted: #665f71;
  position: relative;
  width: min(100%, 66rem);
  height: 20rem;
  margin: 2.2rem auto 1rem;
  isolation: isolate;
}

.http-exchange__wires {
  position: absolute;
  z-index: -1;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}

.exchange-line {
  fill: none;
  stroke-width: 3.5;
  stroke-linecap: round;
  opacity: 0.85;
}
.exchange-line--request { stroke: var(--exchange-purple); }
.exchange-line--response { stroke: var(--exchange-pink); }

.exchange-card {
  position: absolute;
  top: 50%;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  width: 22%;
  height: 9.4rem;
  border: 1px solid rgb(121 84 246 / 42%);
  border-radius: 1rem;
  background: var(--exchange-surface);
  box-shadow: 0 0.55rem 1.45rem rgb(31 32 35 / 9%);
  color: var(--exchange-ink);
  transform: translateY(-50%);
}
.exchange-card--client { left: 0; }
.exchange-card--server { right: 0; }
.exchange-card--ktor {
  border-color: rgb(235 85 230 / 52%);
  background: linear-gradient(135deg, rgb(121 84 246 / 12%), var(--exchange-surface) 55%, rgb(235 85 230 / 10%));
}

.exchange-card__title {
  font-size: 1.8rem;
  font-weight: 800;
  letter-spacing: -0.035em;
  line-height: 1;
}
.exchange-card--ktor .exchange-card__title { color: var(--exchange-purple); }
.exchange-card__hint {
  color: var(--exchange-muted);
  font-size: 1.05rem;
  font-weight: 650;
  letter-spacing: -0.01em;
  line-height: 1;
  text-align: center;
}
.exchange-card__logo { display: block; margin-bottom: 0.2rem; }
.exchange-card__logo img { width: 2.9rem; height: 2.9rem; object-fit: contain; }

.exchange-message {
  position: absolute;
  left: 25%;
  width: 50%;
  color: var(--exchange-ink);
  font-family: var(--slidev-font-mono);
  font-size: 1.55rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1;
  text-align: center;
}
.exchange-message--request { top: 21%; color: var(--exchange-purple); }
.exchange-message--response { top: 68%; color: #b731ae; }

html.dark .http-exchange { --exchange-surface: rgb(35 34 42 / 92%); --exchange-muted: #b9bbc2; }
html.dark .exchange-card { box-shadow: 0 0.55rem 1.45rem rgb(0 0 0 / 24%); }
html.dark .exchange-message--response { color: var(--exchange-pink); }
</style>
