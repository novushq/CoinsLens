# ADR 0009: ViewModel owns all paywall logic

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-7
- **Author:** agent:implement

## Context

Guardrails VM-001/VM-002/VM-003 require immutable StateFlow UiState, no Context in ViewModels, single UiState value.

## Decision

PaywallViewModel exposes StateFlow<UiState<PaywallContent>> + onAction + one-shot NavigateBack events.

## Consequences

Logic is unit-testable with fakes and the composable stays thin; Activity is passed per-action and never held.
