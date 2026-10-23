# ADR 0011: Export paths verified statically, not on device

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-5
- **Author:** agent:implement

## Context

No device commands allowed, so the bitmap/export paths cannot run here.

## Decision

Verify export paths by compilation + unit tests only

## Consequences

Share/save PNG bytes, the share sheet intent, and the MediaStore insert are implemented but not executed — no device/emulator per task rules. Unit tests cover ViewModel states and card text.
