# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.


## 5. Follow the Architecture

**Every new feature follows [ARCHITECTURE.md](ARCHITECTURE.md). No exceptions without asking.**

**Package by feature: one feature, one module.** All of a feature's code lives in its module: controllers (every endpoint of the feature, whatever its URL), services, entities, repositories, DTOs, mappers, config and constants. Anything owned by another module is used only through that module's `api/` ports. If a port is missing a method, add it to that module's `api/` and implement it there; never move the feature's code into the other module. Break reverse dependencies with an `api/event/` event instead of a cycle. Put shared HTTP and security plumbing in `common/`. (Reference: `impersonation` owns `/api/auth/impersonation/*`, uses `session/api` to open and end sessions, and hears about expiry via `SupportSessionExpiredEvent`.)

Before writing code for a feature:
- Decide which module owns it (new or existing), and what goes in `api/` (interfaces + DTOs only) versus `internal/`.
- List the feature's endpoints and confirm every one lands in that module. If one seems to belong elsewhere, stop and ask.
- Portal-owned data: give the table `portal_id`, take `portalId` in every repository method, and get it only from `RequestPortalContext.requirePortalId()`, never from the request body. Another portal's record → 404.
- Permissions: annotate portal endpoints with `@RequirePermission(PermissionConstants.X)`. New permission names go in `PermissionConstants` and are granted in `MemberConstants.SYSTEM_PROFILES`, with a Flyway insert for existing portals.
- Use the exact layout and names: `{Entity}LookUpService` / `{Entity}WriteService`, `{Entity}Service` + `impl/{Entity}ServiceImpl`, `{Entity}CreateDTO`, `{Entity}Mapper`, `{Module}Constants`.

While writing it:
- Call other modules only through their `api/`. Never import another module's `internal/`.
- Use constructor injection only. No `@Autowired` fields, no `@RequiredArgsConstructor`.
- Put named constants in `internal/constants/{Module}Constants.java`, never `private static final` in a service, controller or mapper.
- Put third-party code behind an interface in `external/{provider}/`.
- Return every endpoint as `AppResponse<T>` and throw business errors as `AppException` with a stable `code`.

Before finishing, verify:
```
grep -rn "import com.projecty.work.modules.<other>.internal" src/main/java/com/projecty/work/modules/<module>   → no results
grep -rln "@Service\|@Component\|@Repository" src/main/java/com/projecty/work/modules/*/api                 → no results
grep -rn "@Autowired\|RequiredArgsConstructor" src/main/java                                                     → no results
```

@ARCHITECTURE.md

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
