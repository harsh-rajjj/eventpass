# EventPass — Progress

## Current position
- **Course:** finished S1–S13 · next to watch: S14 (after catch-up)
- **Project phase:** Kickoff ✅ (plan approved 2026-10-10) → now: **C1 — Project setup**
- **Section-gating limit:** concepts up to **S13** + approved extras (see Decisions)

## Decisions log
| Date | Decision |
|---|---|
| 2026-10-10 | Project: **EventPass** (event ticketing). Goal: one portfolio project to get shortlisted for US backend roles (remote + on-site). |
| 2026-10-10 | Java **25 (Temurin LTS)** replaces Java 22 (non-LTS). Maven. Windows. IDE: Antigravity. |
| 2026-10-10 | Lecture notes / HANDOFF blocks **will not be added**. Build the way real industry teams do; still respect the syllabus order for concepts. |
| 2026-10-10 | Approved extras outside the course: JUnit 5 + MockMvc tests per section, Testcontainers, GitHub Actions CI, Flyway, live deployment, polished README. |
| 2026-10-10 | Industry swaps: MySQL + Testcontainers instead of H2; Flyway instead of schema.sql/data.sql; `ProblemDetail` for errors; role as enum on `User` (no roles table). |

## Completed steps
| Step | Covers | Commit | Date |
|---|---|---|---|
| — | — | — | — |

## Open TEMP items
| ID | TEMP | Introduced | Replaced in |
|---|---|---|---|
| T1 | `AuditorAware` returns `"system"` | C3 (planned) | C4 |
| T2 | Event lists/search unpaged | C2 (planned) | S14 |
| T3 | Role checks only `authenticated()` | C4 (planned) | S14 |
| T4 | Default Spring 401/403 responses | C4 (planned) | S14 |
| T5 | JWT secret/expiry in constants class (dev-only value) | C4 (planned) | S18 |
| T6 | Booking limits / upload dir as constants | S17 (planned) | S18 |

## Weak spots
- (none yet)

## Quiz misses
- (none yet)

## Setup checklist
- [x] JDK 25 installed (Temurin 25.0.4.1 LTS, JAVA_HOME set, first on PATH)
- [x] Maven · Docker 29.1 · Git 2.49 · Postman
- [ ] GitHub repo created
