#!/usr/bin/env python3
"""Read-only source checks for this inventory; does not test or launch the app."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
INVENTORY = json.loads((HERE / "inventory.json").read_text())
checks = []


def check(name, passed, details):
    checks.append({"name": name, "passed": bool(passed), "details": details})


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).rstrip("\n")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def declaration_variants(path, name):
    # Limited source-level scanner, not a Kotlin parser or compiler.
    # Mask comments and string contents before balancing declaration braces.
    text = path.read_text()
    pattern = r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/'
    masked = re.sub(pattern, lambda m: " " * len(m[0]), text)
    declaration = re.search(
        r"\b(sealed\s+(?:interface|class)|enum\s+class)\s+" + re.escape(name) + r"\b",
        masked,
    )
    if not declaration:
        raise ValueError(f"Declaration not found: {name}")
    start = masked.index("{", declaration.end())
    depth = 1
    end = start + 1
    while depth and end < len(masked):
        depth += (masked[end] == "{") - (masked[end] == "}")
        end += 1
    if depth:
        raise ValueError(f"Unbalanced declaration: {name}")
    body = masked[start + 1 : end - 1]
    if declaration[1].startswith("sealed"):
        variants = re.findall(r"\bdata\s+(?:object|class)\s+(\w+)", body)
    else:
        variants = re.findall(r"(?:^|,)\s*([A-Z][A-Z_0-9]*)\s*(?=[,(;]|$)", body)
    return variants, text.count("\n", 0, declaration.start()) + 1


head = git("rev-parse", "HEAD")
baseline_is_ancestor = subprocess.run(
    ["git", "merge-base", "--is-ancestor", INVENTORY["code_head"], head], cwd=ROOT
).returncode == 0
check("code_baseline", baseline_is_ancestor, {"head": head, "audit_head": INVENTORY["code_head"]})
release_diff = git("diff", "--name-only", INVENTORY["release_code_commit"], "--", "app")
check("app_equals_v180_release_source", not release_diff, release_diff.splitlines())
build = (ROOT / "app/build.gradle.kts").read_text()
check(
    "version_1_8_0_code_10",
    bool(re.search(r'versionName\s*=\s*"1\.8\.0"', build))
    and bool(re.search(r"versionCode\s*=\s*10\b", build)),
    "app/build.gradle.kts",
)
canonical = ROOT / INVENTORY["canonical_design"]
check(
    "canonical_file_observation",
    canonical.exists() == INVENTORY["canonical_design_exists"],
    {"path": str(canonical), "exists": canonical.exists(),
     "meaning": "Expected missing file; passing is not design completeness."},
)

rows = INVENTORY["groups"]
group_ids = {row["id"] for row in rows}
exception_ids = {row["id"] for row in INVENTORY["exceptions"]}
material_ids = {row["id"] for row in INVENTORY["materials"]}
check("unique_group_ids", len(group_ids) == len(rows), {"groups": len(rows)})
check(
    "unique_material_and_exception_ids",
    len(material_ids) == len(INVENTORY["materials"])
    and len(exception_ids) == len(INVENTORY["exceptions"]),
    {"materials": len(material_ids), "exceptions": len(exception_ids)},
)

nav = (ROOT / "app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt").read_text()
route_constants = re.findall(r"const\s+val\s+(\w+_ROUTE)\s*=", nav)
route_calls = re.findall(r"\bcomposable\s*\(\s*(\w+_ROUTE)", nav)
row_routes = [row["route_constant"] for row in rows if row["route_constant"]]
route_map = INVENTORY["route_mapping"]
check(
    "navigation_routes",
    set(route_constants) == set(route_calls) == set(route_map) == set(row_routes)
    and len(row_routes) == len(set(row_routes))
    and all(route_map[row["route_constant"]] == row["id"] for row in rows if row["route_constant"]),
    {"declarations": route_constants, "composable_calls": route_calls},
)

reference_errors = []
source_count = 0
test_paths = set()
for row in rows:
    for ref in row["sources"]:
        source_count += 1
        path = ROOT / ref["path"]
        lines = path.read_text().splitlines() if path.is_file() else []
        if not (1 <= ref["line"] <= len(lines)) or ref["anchor"] not in lines[ref["line"] - 1]:
            reference_errors.append({"id": row["id"], "source": ref})
    for path in row["test_sources"]:
        test_paths.add(path)
        if not (ROOT / path).is_file():
            reference_errors.append({"id": row["id"], "missing_test": path})
    for material_id in row["material_ids"]:
        if material_id not in material_ids:
            reference_errors.append({"id": row["id"], "unknown_material": material_id})
for material in INVENTORY["materials"]:
    for path in material["paths"]:
        if not (ROOT / path).is_file():
            reference_errors.append({"material": material["id"], "missing_path": path})
        elif sha256(ROOT / path) != material["sha256"][path]:
            reference_errors.append({"material": material["id"], "changed_path": path})
check(
    "source_test_material_references",
    not reference_errors,
    {"source_anchors": source_count, "unique_test_files": len(test_paths), "errors": reference_errors},
)

state_results = []
for state in INVENTORY["state_model_coverage"]:
    actual, line = declaration_variants(ROOT / state["path"], state["name"])
    expected = list(state["coverage"])
    targets = [target for values in state["coverage"].values() for target in values]
    passed = set(actual) == set(expected) and set(targets) <= group_ids | exception_ids
    state_results.append(
        {"name": state["name"], "path": state["path"], "line": line,
         "actual_variants": actual, "mapped_variants": expected, "passed": passed}
    )
check("selected_state_models", all(s["passed"] for s in state_results), state_results)

document_errors = []
document_links = 0
for document in HERE.glob("*.md"):
    for target in re.findall(r"\]\((/[^)]+)\)", document.read_text()):
        document_links += 1
        path = Path(target.split("#", 1)[0])
        if path == HERE / "validation.json":
            continue  # The caller may be generating this result for the first time.
        if not path.exists():
            document_errors.append({"document": document.name, "missing_link": target})
screen_list = (HERE / "screens-and-states.md").read_text()
listed_ids = re.findall(r"^### ([A-Z]\d\d) ", screen_list, re.MULTILINE)
check(
    "human_readable_document_links_and_groups",
    not document_errors and set(listed_ids) == group_ids and len(listed_ids) == len(rows),
    {"local_links": document_links, "listed_group_count": len(listed_ids), "errors": document_errors},
)

manifest = json.loads((HERE / "tracked-before.json").read_text())
changes = [
    path for path, digest in manifest.items()
    if not (ROOT / path).is_file() or sha256(ROOT / path) != digest
]
tracked_now = set(filter(None, git("ls-files", "-z").split("\0")))
prefix = str(HERE.relative_to(ROOT)) + "/"
added_tracked = tracked_now - set(manifest)
tracked_scope_matches = set(manifest) <= tracked_now and all(
    path.startswith(prefix) for path in added_tracked
)
check(
    "preexisting_tracked_files_unchanged",
    not changes and tracked_scope_matches,
    {"tracked_files": len(manifest),
     "app_source_files": sum(p.startswith("app/src/") for p in manifest),
     "changed_or_missing": changes, "tracked_scope_matches": tracked_scope_matches,
     "inventory_files_now_staged_or_committed": len(added_tracked)},
)
pen_files = {path: digest for path, digest in manifest.items() if path.endswith(".pen")}
check(
    "all_existing_pencil_files_unchanged",
    all((ROOT / p).is_file() and sha256(ROOT / p) == digest for p, digest in pen_files.items()),
    pen_files,
)
diff_check = subprocess.run(["git", "diff", "--check"], cwd=ROOT, capture_output=True, text=True)
staged_check = subprocess.run(["git", "diff", "--cached", "--check"], cwd=ROOT, capture_output=True, text=True)
check(
    "git_diff_check",
    diff_check.returncode == 0 and staged_check.returncode == 0,
    {"working_tree": diff_check.stdout + diff_check.stderr,
     "staged": staged_check.stdout + staged_check.stderr},
)
status = git("status", "--porcelain", "--untracked-files=all").splitlines()
check(
    "changes_limited_to_inventory",
    all(line[3:].startswith(prefix) for line in status),
    status,
)

report = {
    "schema_version": 1,
    "result": "PASS" if all(item["passed"] for item in checks) else "FAIL",
    "scope": "Inventory consistency and source provenance only; not UI/runtime acceptance.",
    "ui_group_count": len(rows),
    "route_count": len(route_map),
    "selected_state_model_count": len(state_results),
    "checks": checks,
    "not_run": ["build", "unit_tests", "lint", "android_ui_tests", "app_launch", "full_Pencil_validation"],
}
print(json.dumps(report, ensure_ascii=False, indent=2))
sys.exit(0 if report["result"] == "PASS" else 1)
