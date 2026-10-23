# ADR 0002: Denial does not block completion

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-6
- **Author:** agent:implement

## Context

PermissionResult could have blocked completion on denial.

## Decision

Permission denial still runs CompleteOnboardingUseCase and navigates to Capture.

## Consequences

Users who deny camera still reach Capture, where capture handles permission/galley fallback; no dead end.
