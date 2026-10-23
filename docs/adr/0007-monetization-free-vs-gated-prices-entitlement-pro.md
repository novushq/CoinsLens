# ADR 0007: Monetization: free vs gated, prices, entitlement pro

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-7
- **Author:** agent:implement

## Context

Monetization ADR (policy subscription_ads): what is free vs gated, price points, entitlement id.

## Decision

Weekly $4.99 / annual $29.99 with 3-day trial / lifetime $59.99 under entitlement id pro, store-driven prices, dummy-mode test prices labelled (test).

## Consequences

Free: quota-limited scans plus one rewarded-ad bonus scan at a time. Gated (pro): unlimited scans, folders, condition notes. Rewarded ads only for free users; Pro users never initialise or see ads.
