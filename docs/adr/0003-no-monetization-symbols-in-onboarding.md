# ADR 0003: No monetization symbols in onboarding

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-6
- **Author:** agent:implement

## Context

Monetization policy confines billing/ads to the paywall module.

## Decision

Onboarding navigates straight to Route.Capture with no entitlement checks or ad calls.

## Consequences

No paywall/ads code exists in this module, satisfying the monetization gate by construction.
