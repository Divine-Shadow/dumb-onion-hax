# AGENTS.md


## Architecture and Development Guide

- Start with [documentation/engineering/architecture/README.md](documentation/engineering/architecture/README.md) for a concise index of key topics.
  - Read in full any sections that pertain to your assigned task, especially around the effect system.
- The full guide is available at [documentation/engineering/README_Architecture_And_Development_Guide.md](documentation/engineering/README_Architecture_And_Development_Guide.md)

## Documentation Structure

Documentation in this repository is organized as a navigable **tree of concise, linked pages**, each ideally fewer than 200 lines, designed to protect working memory and ensure precise discoverability. Navigation through the documentation follows a wiki-like structure, using meta-pages that provide clear entry points and breadcrumbs to related topics, enabling quick traversal and indexing of relevant information as needs arise.

When reviewing documentation, agents are expected to reference linked pages whenever they are relevant to their current task. Similarly, when creating or updating documentation, agents must ensure that new pages are properly linked within the existing documentation tree, ultimately connecting back, through one or more intermediary pages, to an `AGENTS.md` file. This structure ensures all documentation remains discoverable, logically organized, and cognitively manageable.

### Documentation Layout
- All additional guides are located in the `documentation` directory.
  - `engineering/` – development and architecture guides.
  - `domain/` – business domain references.
  - `philosophy/` – culture and rationale documents.
  - [Directory Configuration Service](documentation/engineering/directory_configuration_service.md) – user directory selection and persistence.
  - [Wrap Selection Service](documentation/engineering/wrap_selection_service.md) – map wrap conversion options.

## Supplemental Guidelines
- Follow and apply architectural and structural patterns established by the project.
- Follow and apply common coding and naming conventions used in the project. For example, to resolve shadowing, use the "underscore suffix" pattern. See `conversion/Impl.scala` and `compatibility/Impl.scala` for examples.
- Proactively review comments affected by your changes and update/delete them as needed. Delete obsolete and orphaned comments.
- Aside from compile errors, you must also resolve all warnings.
- When grepping, it's recommended to exclude target files as they can create noise that overwhelms the results.
- Do not use abbreviations in your naming.
- This project uses package chaining, ensure `package com.crib.bills.dom6maps` appears on its own line.
- Avoid primitives in anything other than value classes, see existing models for examples of this.

## Living Documents
Many projects have living progress documents associated with them. The workflow for these looks like:
- Agents implement the plan and steps outlined in the plans, there's some freedom for deviation around what makes the most sense once you're actually coding.
- Deviations, implementations, and challenges arising from insufficient information are added to the documentation
- The architects review the documentation, respond to questions, and annotate key steps
- The next round of agent work begins and the cycle repeats

Therefore, it's important to ensure that each agent session ends with leaving a clear blueprint that, once addressed by the architects, provides a clear path forward for the next agent round.

## Testing
- Run unit tests for the feature/fix. Example command: `sbt "project apps" "testOnly com.crib.bills.com.crib.bills.dom6maps.model.map.*"`
- If there are no tests yet for the feature/fix, run `sbt compile`.

## Before commit
- Sanity check: run `sbt compile`

## Commit Instructions
- Message format: [`Feature`]/[`Fix`] `Description`
- Example: "[Download Support] Initially added Service, Stub, and Impl"

## PR Instructions
- Title format: [`Feature`]/[`Fix`]: `Feature Name`/`Fix Description`
- Example: "Feature: Download Support"
- Start with a one-line summary and a "Testing Done" section
- Then add a paragraph with any available context around why the changes were made and any significant design decisions.

## CLI Tools
- See `Codex_Environment_Setup.sh` if you need more information about what CLI tools have been installed.