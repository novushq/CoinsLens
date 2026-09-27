# CoinLens — Architecture

> Status: **draft** — to be written by the architecture skill.

The module skeleton below is created by the factory and is not optional.

| Module | Type | Responsibility |
|---|---|---|
| `:app` | Android app | Activity, Application, Koin bootstrap, NavHost |
| `:core:model` | Kotlin JVM | Immutable domain models |
| `:core:common` | Kotlin JVM | `AppResult`, `AppError`, dispatchers, logging |
| `:core:domain` | Kotlin JVM | Use cases, repository interfaces |
| `:core:data` | Android lib | Repository impls, Room, network, DataStore |
| `:core:designsystem` | Android lib | Material 3 theme and shared components |
| `:core:navigation` | Android lib | Type-safe routes |
| `:feature:*` | Android lib | One screen cluster each, agent-owned |
