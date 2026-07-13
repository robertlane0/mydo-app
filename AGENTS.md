# Repository Guidelines

## Project Structure & Module Organization

This repository defines the Mydo app through Markdown specifications. `SPECIFICATION.md` is the entry point; use it to orient readers, then keep the detailed requirements in `specs/`. Files follow the `specsNN-topic.md` pattern, for example `specs/specs03-home-screen.md` and `specs/specs11-data-model.md`. Keep related requirements in the existing topic file rather than creating overlapping documents. There is no application source, asset directory, or generated output committed here.

## Development and Validation

There are currently no build, development-server, formatter, or automated test commands. Before submitting a documentation change, use lightweight checks:

```bash
git diff --check          # detect whitespace errors
rg "term to verify" specs # check terminology and cross-references
git diff -- specs/         # review the proposed requirement changes
```

Preview edited Markdown in a renderer when possible. Verify heading levels, links, lists, and code blocks render clearly.

## Writing Style & Naming Conventions

Use concise Markdown with sentence-case headings and direct, product-focused language. Preserve the established file naming scheme: lowercase, hyphenated topic names, prefixed by a two-digit section number (`specs08-search.md`). Prefer one requirement per bullet or short paragraph. Use backticks for UI labels, commands, data fields, and paths. Maintain consistent terminology across files; update the data model and user flows when a feature change affects them.

## Specification Review Guidelines

Treat review as the test suite. Check a change against the overview, navigation, design system, data model, and user-flow documents where relevant. Requirements should describe expected behavior, states, edge cases, and user-visible outcomes—not implementation guesses. Search `specs/` for renamed screens, fields, or concepts and update every affected reference.

## Commit & Pull Request Guidelines

The available history uses concise, scoped summaries such as `Codex: edited specifications`. Follow that style with an imperative description, for example `Specs: clarify task completion behavior`. Keep commits focused on one coherent feature or correction.

Pull requests should explain the user-facing change, list the specification files touched, and note cross-document updates. Link the relevant issue when one exists. Include screenshots only when the change includes rendered designs or UI artifacts.
