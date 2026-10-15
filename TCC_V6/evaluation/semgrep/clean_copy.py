"""Copies the .java, .properties and .yml files of each package's src/main to <destination>, without comments and
with the lines in the same place. Semgrep reads this copy: the text rules never see a comment, and file:line
matches the original. Called by detect.py. (V5's copia-limpa.mjs and sem-comentarios.mjs, joined, same logic.)

The project of each package is the one of the shallowest pom.xml (outside target/ and .git/), the same rule as
the build and the suite: Semgrep reads exactly the code that was compiled and tested, even with the project in
a subfolder or a draft of the agent in another folder. In a multi-module project (the root pom only lists
<module>), each listed module's src/main counts. Without a pom.xml, the shallowest src/main.

Usage:  python clean_copy.py <packages-root> <empty-destination>
"""

import re
import sys
from collections.abc import Callable
from pathlib import Path

SKIPPED = {"target", ".git"}
COPIED = re.compile(r"\.(java|properties|ya?ml)$")


# ------------------------------------------------------------------ removing comments

def strip_java_comments(source: str) -> str:
    """Removes the comments of a .java without touching quoted text and without changing the line count."""
    kept: list[str] = []
    position, length = 0, len(source)
    while position < length:
        char = source[position]
        next_char = source[position + 1] if position + 1 < length else ""
        if source.startswith('"""', position):                     # text block: kept as it is
            closing = source.find('"""', position + 3)
            block_end = length if closing < 0 else closing + 3
            kept.append(source[position:block_end])
            position = block_end
        elif char in "\"'":                                         # string or char: kept as it is
            quote_end = position + 1
            while quote_end < length and source[quote_end] not in (char, "\n"):
                quote_end += 2 if source[quote_end] == "\\" else 1   # skip the escaped character
            kept.append(source[position:quote_end + 1])
            position = quote_end + 1
        elif char == "/" and next_char == "/":                      # line comment: dropped up to the line break
            while position < length and source[position] != "\n":
                position += 1
        elif char == "/" and next_char == "*":                      # block comment: only its line breaks stay
            closing = source.find("*/", position + 2)
            comment_end = length if closing < 0 else closing + 2
            kept.append("\n" * source.count("\n", position, comment_end))
            position = comment_end
        else:
            kept.append(char)
            position += 1
    return re.sub(r"[ \t]+$", "", "".join(kept), flags=re.MULTILINE)


def strip_line_comments(text: str) -> str:
    return re.sub(r"^[ \t]*[#!].*$", "", text, flags=re.MULTILINE)


def copy_without_comments(source: Path, target: Path) -> None:
    strip_comments = strip_java_comments if source.suffix == ".java" else strip_line_comments
    text = source.read_text(encoding="utf-8", errors="replace")
    target.write_text(strip_comments(text), encoding="utf-8", newline="\n")


# ------------------------------------------------------------------ picking the project

def depth_within(folder: Path, package: Path) -> int:
    return len(folder.relative_to(package).parts)


def shallowest_first(folders: list[Path], package: Path) -> list[Path]:
    """The shallowest first; a tie in depth is broken by the path (like the build's sort)."""
    return sorted(folders, key=lambda folder: (depth_within(folder, package), folder.relative_to(package).as_posix()))


def find_poms_and_mains(package: Path) -> tuple[list[Path], list[Path]]:
    """Every folder of the package that has a pom.xml, and every src/main folder."""
    folders_with_pom: list[Path] = []
    main_folders: list[Path] = []
    folders_to_visit = [package]
    while folders_to_visit:
        folder = folders_to_visit.pop()
        for entry in folder.iterdir():
            if entry.is_dir():
                if entry.name in SKIPPED:
                    continue
                if entry.name == "main" and folder.name == "src":
                    main_folders.append(entry)
                folders_to_visit.append(entry)
            elif entry.name == "pom.xml":
                folders_with_pom.append(folder)
    return folders_with_pom, main_folders


def project_main_folders(project: Path, already_seen: set[Path]) -> list[Path]:
    """The src/main of a pom's project, and of each <module> it lists (recursively)."""
    if project in already_seen:
        return []
    already_seen.add(project)
    pom_text = (project / "pom.xml").read_text(encoding="utf-8", errors="replace")
    pom_without_comments = re.sub(r"<!--[\s\S]*?-->", "", pom_text)
    main_folders = [project / "src" / "main"] if (project / "src" / "main").is_dir() else []
    for module in re.findall(r"<module>\s*([^<]+?)\s*</module>", pom_without_comments):
        if (project / module / "pom.xml").is_file():
            main_folders += project_main_folders(project / module, already_seen)
    return main_folders


def package_main_folders(package: Path, warn: Callable[[str], None] = lambda _message: None) -> list[Path]:
    """The src/main folders that count for one package, with a warning when the choice was not obvious."""
    folders_with_pom, main_folders = find_poms_and_mains(package)

    def shown(folder: Path) -> str:
        return folder.relative_to(package).as_posix() or "."

    if folders_with_pom:
        by_depth = shallowest_first(folders_with_pom, package)
        if len(by_depth) > 1 and depth_within(by_depth[1], package) == depth_within(by_depth[0], package):
            warn(f"two pom.xml at the same depth; {shown(by_depth[0])} counts")
        counted = project_main_folders(by_depth[0], set())
        if len(counted) > 1:
            warn(f"multi-module; {', '.join(shown(folder) for folder in counted)} count")
        return counted
    if main_folders:
        shallowest = shallowest_first(main_folders, package)[0]
        warn(f"no pom.xml; {shown(shallowest)} counts")
        return [shallowest]
    warn("no src/main (the classifier marks it no-code)")
    return []


# ------------------------------------------------------------------ the copy

def clean_copy(packages_root: Path, destination: Path) -> int:
    """Copies every package of the root; returns how many files were copied."""
    if destination.exists() and any(destination.iterdir()):
        sys.exit(f"{destination} is not empty")
    files_copied = 0
    for package in sorted(folder for folder in packages_root.iterdir() if folder.is_dir()):
        (destination / package.name).mkdir(parents=True, exist_ok=True)

        def warn(message: str, package_name: str = package.name) -> None:
            print(f"WARNING {package_name}: {message}", file=sys.stderr)

        for main_folder in package_main_folders(package, warn):
            for source_file in main_folder.rglob("*"):
                inside_a_skipped_folder = bool(SKIPPED & set(source_file.relative_to(main_folder).parts))
                if not source_file.is_file() or not COPIED.search(source_file.name) or inside_a_skipped_folder:
                    continue
                target_file = destination / package.name / source_file.relative_to(package)
                target_file.parent.mkdir(parents=True, exist_ok=True)
                copy_without_comments(source_file, target_file)
                files_copied += 1
    return files_copied


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: python clean_copy.py <packages-root> <empty-destination>")
    packages_root, destination = Path(sys.argv[1]), Path(sys.argv[2])
    files_copied = clean_copy(packages_root, destination)
    package_count = sum(1 for folder in packages_root.iterdir() if folder.is_dir())
    print(f"{package_count} packages, {files_copied} files without comments", file=sys.stderr)


if __name__ == "__main__":
    main()
