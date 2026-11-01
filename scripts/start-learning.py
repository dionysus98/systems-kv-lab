#!/usr/bin/env python3
"""Export reusable teaching material and clean exercises without learner history."""

import argparse
from pathlib import Path
import shutil
import sys


def export_workspace(source: Path, destination: Path) -> int:
    source = source.resolve()
    destination = destination.resolve()
    if destination == source or source in destination.parents:
        raise ValueError("Choose a destination outside the source repository.")
    if destination.exists():
        raise ValueError("Destination already exists; no files were changed.")
    if not destination.parent.is_dir():
        raise ValueError("Destination parent directory must already exist.")

    # Explicit inclusion keeps personal records, answers, caches, and remotes out.
    files = {Path(name): source / name for name in ("README.md", "AGENTS.md", ".gitignore")}
    for directory in (".agents/skills", "docs", "scripts", "tests"):
        for path in (source / directory).rglob("*"):
            if path.is_file() and path.suffix in {".md", ".yaml", ".py"}:
                files[path.relative_to(source)] = path
    for path in (source / "lessons").glob("*/README.md"):
        files[path.relative_to(source)] = path
    for path in (source / ".joyride").rglob("*"):
        if path.is_file() and path.suffix in {".cljs", ".c"}:
            files[path.relative_to(source)] = path

    # Overlay maintained starter inputs, never the current learner's source.
    baseline = source / "starter"
    for path in baseline.rglob("*"):
        if path.is_file():
            relative = path.relative_to(baseline)
            files[relative] = path
            files[Path("starter") / relative] = path
    required = {"learner-profile.md", "learning-checkpoints.md", "session-doc.md",
                "notes/language-comparison.md", "lessons/01-build-and-run/predictions.md",
                "lessons/01-build-and-run/c/main.c",
                "lessons/01-build-and-run/rust/main.rs",
                "lessons/01-build-and-run/zig/main.zig",
                "lessons/02-integers-and-memory/c/main.c",
                "lessons/02-integers-and-memory/rust/main.rs",
                "lessons/03-fixed-store/c/main.c"}
    for name in required:
        if not (baseline / name).is_file():
            raise ValueError(f"Missing starter baseline: {name}")
    for relative, path in files.items():
        if path.is_symlink() or not path.resolve().is_relative_to(source):
            raise ValueError(f"Refusing linked source file: {relative}")
        if not path.is_file():
            raise ValueError(f"Missing source file: {relative}")

    # Exclusive creation also protects a destination created after the preflight.
    destination.mkdir()
    for relative, path in sorted(files.items()):
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)
    return len(files)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path, help="New directory outside this repository")
    args = parser.parse_args()
    try:
        count = export_workspace(Path(__file__).resolve().parents[1], args.destination)
    except (OSError, ValueError) as error:
        print(f"Cannot create starter: {error}", file=sys.stderr)
        return 1
    print(f"Created {args.destination.resolve()} with {count} files.")
    print("Open that directory and follow docs/new-learner.md. No Git remote was copied.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
