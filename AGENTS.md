# Agents: start here

1. `docs/architecture.md` and `docs/decisions.md`: the parts and the reasons.
2. `docs/logic-map.md`: how a Meet caption becomes a transcript file: every step, every branch, the module behind it.
3. `docs/plans/`: work in progress; `2026-11-05-logic-map-fixes.md` lists the known logic gaps.
4. Open bugs, plans and ideas: [PLAN.md](PLAN.md).

Rules:
- Change a step or a branch → update `docs/logic-map.md` in the same commit.
- `scripts/check-logic-map.sh` fails when a `Module:` line in the map points to a module that is gone. Run it before committing.
