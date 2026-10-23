# ADR 0006: Robolectric for SavedStateHandle route test

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-2
- **Author:** agent:implement

## Context

navigation toRoute decodes via android.os.Bundle, which is unmocked in plain JVM unit tests (RuntimeException: Method putCharSequence not mocked).

## Decision

Run ResultViewModelTest under Robolectric.

## Consequences

Slightly slower module tests, consistent with RoomRepositoriesTest precedent.
