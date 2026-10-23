# ADR 0004: Sheet state lives in ResultData

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-2
- **Author:** agent:implement

## Context

VM-003 requires a single UiState; the bottom sheet and hint detail need state too.

## Decision

Kept all sheet and expanded-hint state inside ResultData (sheet: AddSheetState?, expandedHint) with one intent function per control.

## Consequences

Sheet/hint state survives config change and stays unit-testable; no ephemeral UI state outside the VM.
