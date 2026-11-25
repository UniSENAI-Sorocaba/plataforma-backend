"""Write JaCoCo global and critical coverage to GitHub Actions' job summary."""

import os
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
POM_NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def maven_properties():
    pom = ET.parse(ROOT / "pom.xml").getroot()
    artifact = pom.findtext("m:artifactId", default="backend", namespaces=POM_NS)
    props = pom.find("m:properties", POM_NS)
    values = {} if props is None else {
        child.tag.rsplit("}", 1)[-1]: (child.text or "").strip() for child in props
    }
    return artifact, values


def report(path):
    file = ROOT / path
    if not file.exists():
        return None
    root = ET.parse(file).getroot()
    counters = {item.get("type"): item for item in root.findall("counter")}
    return root, counters


def percent(counter):
    if counter is None:
        return None
    covered = int(counter.get("covered", "0"))
    missed = int(counter.get("missed", "0"))
    total = covered + missed
    return None if total == 0 else covered * 100.0 / total


def show(value):
    return "n/a" if value is None else f"{value:.2f}%"


def result(line, branch, line_min, branch_min):
    checks = []
    if line is not None:
        checks.append(line >= line_min * 100)
    if branch is not None:
        checks.append(branch >= branch_min * 100)
    if not checks:
        return "indisponível"
    return "PASS" if all(checks) else "ABAIXO"


artifact, props = maven_properties()
global_line_min = float(props["be020.coverage.global.line.minimum"])
global_branch_min = float(props["be020.coverage.global.branch.minimum"])
critical_line_min = float(props["be020.coverage.critical.line.minimum"])
critical_branch_min = float(props["be020.coverage.critical.branch.minimum"])

lines = [
    "## Cobertura do backend",
    "",
    "| Módulo Maven | Escopo | Linhas | Branches | Resultado |",
    "| --- | --- | ---: | ---: | --- |",
]

global_report = report("target/site/jacoco-backend/jacoco.xml")
if global_report is None:
    lines.append(f"| `{artifact}` | global | relatório indisponível | relatório indisponível | indisponível |")
    lines.extend(["", "Relatório global não encontrado; consulte os passos Maven e Surefire para a causa."])
else:
    _, counters = global_report
    line = percent(counters.get("LINE"))
    branch = percent(counters.get("BRANCH"))
    lines.append(
        f"| `{artifact}` | backend (mín. {global_line_min:.0%}/{global_branch_min:.0%}) "
        f"| {show(line)} | {show(branch)} | {result(line, branch, global_line_min, global_branch_min)} |"
    )

lines.extend([
    "",
    "### Pacotes críticos",
    "",
    f"Limites por pacote: LINE >= {critical_line_min:.0%}, BRANCH >= {critical_branch_min:.0%}.",
    "",
    "| Módulo | Pacote | Linhas | Branches | Resultado |",
    "| --- | --- | ---: | ---: | --- |",
])

critical_report = report("target/site/jacoco-critical/jacoco.xml")
if critical_report is None:
    lines.append(f"| `{artifact}` | relatório crítico indisponível | n/a | n/a | indisponível |")
else:
    critical_root, _ = critical_report
    packages = critical_root.findall("package")
    if not packages:
        lines.append(f"| `{artifact}` | nenhum pacote no relatório crítico | n/a | n/a | indisponível |")
    for package in packages:
        counters = {item.get("type"): item for item in package.findall("counter")}
        line = percent(counters.get("LINE"))
        branch = percent(counters.get("BRANCH"))
        lines.append(
            f"| `{artifact}` | `{package.get('name', 'desconhecido').replace('/', '.')}` "
            f"| {show(line)} | {show(branch)} | "
            f"{result(line, branch, critical_line_min, critical_branch_min)} |"
        )

markdown = "\n".join(lines) + "\n"
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as summary:
        summary.write(markdown)
else:
    print(markdown, end="")
