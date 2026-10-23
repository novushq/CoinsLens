# ADR 0010: Blank rewarded-unit default selects the fake

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-7
- **Author:** agent:implement

## Context

Spec: fake gateways when API keys are absent. revenuecat.apiKey already defaults blank; admob.rewardedUnitId defaulted to the Google sample unit so the fake never triggered.

## Decision

Default admob.rewardedUnitId to blank so absent keys mean FakeRewardedAdGateway.

## Consequences

Clean clones build fully offline in dummy mode; setting admob.rewardedUnitId opts into the real rewarded SDK with the manifest test app id.
