# Page object model

<!-- Agent drafting scope:
Read AGENTS.md first. Then inspect appium-tests/src/test/kotlin/pages/Element.kt,
at least three current page objects, and the action classes that use them.

Fill only this file. Keep its existing headings. Describe conventions only when
repository rules and current code support them. Compare how Element operations
locate and wait for elements, including exceptions. Quote real declarations,
and mark anything you cannot confirm as UNVERIFIED. Do not modify other files.
-->

This document starts as an AI draft. Verify every claim against the repository
before relying on it.

## Role of pages

[TODO: determine what page objects own in this repository and how the shared element abstraction works. Describe locator construction, supported operations, and how each operation locates or waits for an element, including any operation that uses a different expected condition. Cite the repository rule for prohibited locator strategies.]

## No asserts rule

[TODO: state the repository rule for assertions, interactions, and flow logic. Cite both the policy source and representative code, then explain the practical benefit without turning an observation into an uncited rule.]

## Example from this repo

[TODO: choose one current page object and walk through its element declarations, any dynamic locator factory, and the way callers determine screen readiness. Quote only declarations that exist in the repository.]

## How actions use pages

[TODO: choose one current action class and show how it uses a page catalog, where it waits, and where it asserts. Quote the real code and cite its path.]
