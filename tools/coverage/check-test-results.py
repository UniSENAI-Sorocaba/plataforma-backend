"""Fail the gate when unit, integration, or contract tests are missing/skipped."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "target/surefire-reports"
CONTRACT_LOG = ROOT / "target/contracts/verification.log"
failed = False

reports = sorted(REPORTS.glob("TEST-*.xml")) if REPORTS.exists() else []
if not reports:
    print("FAIL: nenhum relatório Surefire encontrado; testes Java não foram comprovados.")
    failed = True
else:
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    for path in reports:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for key in totals:
                totals[key] += int(suite.get(key, "0"))
    print(
        "Surefire: {tests} testes, {failures} falhas, {errors} erros, "
        "{skipped} ignorados.".format(**totals)
    )
    if totals["tests"] == 0 or totals["skipped"] > 0 or totals["failures"] > 0 or totals["errors"] > 0:
        print("FAIL: suíte Java ausente, com teste ignorado ou com falha/erro.")
        failed = True

if not CONTRACT_LOG.exists():
    print("FAIL: log de contratos ausente; testes de contrato não foram comprovados.")
    failed = True
else:
    text = CONTRACT_LOG.read_text(encoding="utf-8", errors="replace")
    tests = re.search(r"(?:ℹ\s*)?tests\s+(\d+)", text)
    skipped = re.search(r"(?:ℹ\s*)?skipped\s+(\d+)", text)
    failures = re.search(r"(?:ℹ\s*)?fail\s+(\d+)", text)
    if not tests:
        print("FAIL: log sem resumo TAP; testes de contrato não foram comprovados.")
        failed = True
    else:
        contract_tests = int(tests.group(1))
        contract_skipped = int(skipped.group(1)) if skipped else 0
        contract_failures = int(failures.group(1)) if failures else 0
        print(
            f"Contratos (node:test): {contract_tests} testes, "
            f"{contract_failures} falhas, {contract_skipped} ignorados."
        )
        if contract_tests == 0 or contract_skipped > 0 or contract_failures > 0:
            print("FAIL: suíte de contrato ausente, com falha ou com testes ignorados.")
            failed = True

sys.exit(1 if failed else 0)
