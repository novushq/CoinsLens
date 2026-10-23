# ADR 0001: Skip means COLLECTOR

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-6
- **Author:** agent:implement

## Context

Spec says skip defaults to COLLECTOR but is silent on skip-after-tapping-a-persona.

## Decision

Skip always completes with Persona.COLLECTOR, discarding any tapped selection.

## Consequences

Tapping Skip never persists a half-made choice; the quiz page always starts from COLLECTOR.
