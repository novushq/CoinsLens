# ADR 0012: Reuse app FileProvider authority

- **Status:** accepted
- **Date:** 2026-10-23
- **Task:** C-5
- **Author:** agent:implement

## Context

A provider declared in feature/share/src/main/AndroidManifest.xml broke :app:processDebugMainManifest (duplicate FileProvider, conflicting authorities); :app manifest is outside my fence.

## Decision

Reuse the app shell FileProvider

## Consequences

PNG shares through <appId>.fileprovider instead of the spec's <appId>.share.
