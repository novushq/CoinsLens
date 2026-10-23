# ADR 0008: Custom paywall instead of kit paywall screen

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-7
- **Author:** agent:implement

## Context

Task spec mandates a custom Compose paywall with persona-tailored copy; the monetization skill says never build a bespoke paywall.

## Decision

Built the custom paywall as specified: persona headline/benefit, 3 plan cards with annual preselected and trial badge, Continue/Restore/terms/privacy/close.

## Consequences

Paywall copy and plan-card layout diverge from the kit screen; future kit updates to the stock paywall do not propagate here.
