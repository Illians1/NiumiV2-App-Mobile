---
name: commit-workflow
description: Use when preparing, reviewing, creating, amending, or finalizing a Git commit, or when the user asks for a commit message.
---

# Commit Workflow

## Overview

Create small, intentional, reviewable commits without disturbing unrelated work. Respect repository instructions first.

## Workflow

1. **Determine the requested action**
   - If the user only asks for a commit message or review, do not stage or commit.
   - Create or amend a commit only when explicitly requested.
   - Never push unless explicitly requested.

2. **Inspect the repository**
   - Run `git status --short`.
   - Review unstaged and staged diffs with `git diff` and `git diff --staged`.
   - Identify untracked files and pre-existing changes before staging anything.

3. **Choose the commit scope**
   - Include only changes relevant to the requested task.
   - Do not silently include unrelated, generated, temporary, credential, secret, or local-environment files.
   - Prefer `git add <specific-paths>` over `git add .`.
   - Preserve pre-existing staged changes unless they clearly belong to the requested commit.
   - If unrelated changes cannot be safely separated, stop and explain the ambiguity.

4. **Verify before committing**
   - Run the relevant tests, checks, build, lint, or typecheck required by the repository and the change.
   - Do not hide failures.
   - Run `git diff --staged --check`.
   - Review the complete staged diff before committing.

5. **Write the message**
   - Follow the repository's existing commit convention when one exists.
   - Otherwise use a concise imperative subject describing the change.
   - Add a body only when it provides useful context, rationale, or migration information.
   - Never add `Co-Authored-By` unless the user explicitly requires it.
   - Do not add AI attribution or generated-by metadata unless explicitly requested.

6. **Create the commit**
   - Commit only the reviewed staged changes.
   - Never use `--no-verify` unless explicitly requested and the reason is understood.
   - Never amend an existing commit unless explicitly requested.
   - Avoid destructive Git commands such as `reset --hard`, `clean -fd`, or forced checkout as part of this workflow.

7. **Verify after committing**
   - Run `git status --short`.
   - Inspect the new commit with `git log -1 --oneline`.
   - Report the commit identifier, summary, verification performed, and any remaining uncommitted changes.
   - Do not push automatically.

## Quick Reference

| Request | Action |
| --- | --- |
| "Propose un message de commit" | Inspect diff and propose text only |
| "Prépare le commit" | Review and stage relevant changes; do not commit unless clearly requested |
| "Commit ces changements" | Review, verify, stage relevant changes, then commit |
| "Amend le dernier commit" | Amend only after explicit request |
| "Commit et push" | Commit, then push only because push was explicitly requested |

## Red Flags

Stop before committing if:
- secrets or credentials appear in the diff;
- the staged diff contains unrelated work;
- required verification fails;
- the repository instructions prohibit the intended action;
- the commit would overwrite or discard someone else's work.
