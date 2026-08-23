# Test architecture

<!-- Agent drafting scope:
Read AGENTS.md first. Then inspect appium-tests/README.md and the current sources
under appium-tests/src/test/kotlin/rule/, pages/, actions/, tests/ and testdata/.

Fill only this file. Keep its existing headings. Map claims to real source paths,
avoid duplicating volatile test inventory, and mark anything you cannot confirm
as UNVERIFIED. Do not modify other files.
-->

This document starts as an AI draft. Verify every claim against the repository
before relying on it.

## Layers

[TODO: identify the suite layers from the current source tree. For each layer, document its responsibility, what it owns, and what it must not contain. Cite the policy and representative code rather than relying on generic Appium conventions.]

## Where things live

[TODO: map each layer and test data to its current repository directory. Point readers to the authoritative test inventory instead of duplicating a volatile class list.]

## Waits and retries

[TODO: document the repository's synchronization policy and the concrete wait or retry helpers that implement it. Explain the intended use and prohibited misuse of each helper with source citations.]

## Failure artifacts

[TODO: document what is captured when a test fails, which code captures it, and where each artifact is written. Verify every path against the current source.]
