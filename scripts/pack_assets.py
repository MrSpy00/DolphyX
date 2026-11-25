#!/usr/bin/env python3
"""Pack DolphyX IRDB, OUI database, and signed sample plugins into app/src/main/assets."""

from __future__ import annotations

import hashlib
import json
import sqlite3
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
DOLPHYX_ROOT = SCRIPT_DIR.parent
DOLPHY_ROOT = DOLPHYX_ROOT.parent
RAW_ASSETS = DOLPHY_ROOT / "unpacked" / "raw" / "assets"
ASSETS_OUT = DOLPHYX_ROOT / "app" / "src" / "main" / "assets"


def parse_ir_file(path: Path) -> list[dict[str, object]]:
    """Parse a Flipper-format .ir file into a list of signal dicts."""
    signals: list[dict[str, object]] = []
    current: dict[str, str] = {}

    def flush_block() -> None:
        nonlocal current
        if "name" in current and "type" in current:
            freq_raw = current.get("frequency")
            duty_raw = current.get("duty_cycle")
            try:
                freq = int(freq_raw) if freq_raw is not None else None
            except ValueError:
                freq = None
            try:
                duty = float(duty_raw) if duty_raw is not None else None
            except ValueError:
                duty = None
            signals.append(
                {
                    "name": current["name"],
                    "sig_type": current["type"],
                    "protocol": current.get("protocol"),
                    "address": current.get("address"),
                    "command": current.get("command"),
                    "frequency": freq,
                    "duty_cycle": duty,
                    "raw_data": current.get("data"),
                }
            )
        current = {}

    text = path.read_text(encoding="utf-8", errors="replace")
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            flush_block()
            continue
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        key = key.strip()
        value = value.strip()
        if key in ("Filetype", "Version"):
            continue
        if key == "name" and "name" in current:
            flush_block()
        current[key] = value

    flush_block()
    return signals


def build_irdb(out_db: Path) -> tuple[int, int, int]:
    """Pack tv_best_remotes and Flipper-IRDB-main into dolphyx_irdb.db."""
    if out_db.exists():
        out_db.unlink()
    out_db.parent.mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(out_db)
    conn.execute("PRAGMA page_size=4096;")
    conn.execute("PRAGMA journal_mode=OFF;")
    conn.execute("PRAGMA synchronous=OFF;")
    conn.executescript(
        """
        CREATE TABLE remotes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category TEXT NOT NULL,
            brand TEXT NOT NULL,
            model TEXT NOT NULL,
            source TEXT NOT NULL,
            signal_count INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE signals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            remote_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            sig_type TEXT NOT NULL,
            protocol TEXT,
            address TEXT,
            command TEXT,
            frequency INTEGER,
            duty_cycle REAL,
            raw_data TEXT,
            FOREIGN KEY(remote_id) REFERENCES remotes(id)
        );
        """
    )

    remote_count = 0
    signal_count = 0
    raw_bytes = 0

    def insert_remote(category: str, brand: str, model: str, source: str, ir_path: Path) -> None:
        nonlocal remote_count, signal_count, raw_bytes
        raw_bytes += ir_path.stat().st_size
        sigs = parse_ir_file(ir_path)
        if not sigs:
            return
        cur = conn.execute(
            "INSERT INTO remotes (category, brand, model, source, signal_count) VALUES (?, ?, ?, ?, ?)",
            (category, brand, model, source, len(sigs)),
        )
        remote_id = cur.lastrowid
        conn.executemany(
            """
            INSERT INTO signals (remote_id, name, sig_type, protocol, address, command, frequency, duty_cycle, raw_data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            [
                (
                    remote_id,
                    s["name"],
                    s["sig_type"],
                    s["protocol"],
                    s["address"],
                    s["command"],
                    s["frequency"],
                    s["duty_cycle"],
                    s["raw_data"],
                )
                for s in sigs
            ],
        )
        remote_count += 1
        signal_count += len(sigs)

    tv_best_dir = RAW_ASSETS / "tv_best_remotes"
    if tv_best_dir.is_dir():
        for ir_file in sorted(tv_best_dir.rglob("*.ir")):
            rel = ir_file.relative_to(tv_best_dir)
            brand = rel.parts[0] if len(rel.parts) >= 2 else "Generic"
            model = ir_file.stem
            insert_remote("TVs", brand, model, "tv_best", ir_file)

    flipper_dir = RAW_ASSETS / "Flipper-IRDB-main"
    if flipper_dir.is_dir():
        for ir_file in sorted(flipper_dir.rglob("*.ir")):
            rel = ir_file.relative_to(flipper_dir)
            category = rel.parts[0] if len(rel.parts) >= 2 else "Other"
            brand = rel.parts[1] if len(rel.parts) >= 3 else "Generic"
            model = ir_file.stem
            insert_remote(category, brand, model, "flipper_irdb", ir_file)

    conn.executescript(
        """
        CREATE INDEX idx_remotes_cat_brand ON remotes(category, brand);
        CREATE INDEX idx_remotes_brand_model ON remotes(brand, model);
        CREATE INDEX idx_signals_remote_id ON signals(remote_id);
        """
    )
    conn.commit()
    conn.execute("PRAGMA page_size=4096;")
    conn.execute("VACUUM;")
    conn.close()
    return remote_count, signal_count, raw_bytes


def build_oui(out_db: Path) -> tuple[int, int]:
    """Pack ble/nmap-mac-prefixes.txt into dolphyx_oui.db."""
    if out_db.exists():
        out_db.unlink()
    out_db.parent.mkdir(parents=True, exist_ok=True)

    src = RAW_ASSETS / "ble" / "nmap-mac-prefixes.txt"
    raw_bytes = src.stat().st_size

    conn = sqlite3.connect(out_db)
    conn.execute("PRAGMA page_size=4096;")
    conn.execute("PRAGMA journal_mode=OFF;")
    conn.execute("PRAGMA synchronous=OFF;")
    conn.execute("CREATE TABLE oui (prefix TEXT PRIMARY KEY, vendor TEXT NOT NULL);")

    rows: list[tuple[str, str]] = []
    for line in src.read_text(encoding="utf-8", errors="replace").splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        parts = s.split(None, 1)
        if len(parts) != 2:
            continue
        prefix, vendor = parts[0].upper(), parts[1].strip()
        if len(prefix) == 6:
            rows.append((prefix, vendor))

    conn.executemany("INSERT OR REPLACE INTO oui (prefix, vendor) VALUES (?, ?)", rows)
    row_count = conn.execute("SELECT COUNT(*) FROM oui").fetchone()[0]
    conn.commit()
    conn.execute("VACUUM;")
    conn.close()
    return row_count, raw_bytes


def canonical_sha256(payload: dict[str, object]) -> str:
    """Compute SHA-256 hex of compact, sorted-key UTF-8 JSON without contentSha256."""
    clean = {k: v for k, v in payload.items() if k != "contentSha256"}
    canonical = json.dumps(clean, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def write_sample_plugins(out_dir: Path) -> list[tuple[str, str]]:
    """Write sandboxed, SHA-256-verified sample .dolphyxplugin manifests."""
    out_dir.mkdir(parents=True, exist_ok=True)
    plugins: list[tuple[str, dict[str, object]]] = [
        (
            "hardware_health_check.dolphyxplugin",
            {
                "id": "com.aegissoft.dolphyx.plugin.hardware_health",
                "name": "Hardware Health Check",
                "version": "1.0.0",
                "author": "aegisSoft",
                "description": "Read-only diagnostic summary of device BLE, NFC, IR, and Wi-Fi hardware readiness.",
                "capabilities": ["hardware.status", "irdb.stats"],
                "ui": [
                    {
                        "type": "header",
                        "title": "Hardware Readiness",
                        "value": "Android 16 / Honor MagicOS",
                    },
                    {
                        "type": "badge",
                        "title": "Sandbox Mode",
                        "value": "READ_ONLY_VERIFIED",
                    },
                    {
                        "type": "metric",
                        "title": "Subsystems",
                        "value": "BLE + NFC/HCE + ConsumerIR + Wi-Fi",
                    },
                    {
                        "type": "text",
                        "title": "Policy",
                        "value": "Zero shell, zero reflection, zero dynamic DEX execution.",
                    },
                    {
                        "type": "button",
                        "title": "Refresh Hardware Snapshot",
                        "value": "refresh_hardware",
                        "action": "hardware.refresh",
                    },
                ],
            },
        ),
        (
            "ble_oui_summary.dolphyxplugin",
            {
                "id": "com.aegissoft.dolphyx.plugin.ble_oui_summary",
                "name": "BLE & OUI Summary",
                "version": "1.0.0",
                "author": "aegisSoft",
                "description": "Displays live BLE scan/bonded device counts and local SQLite OUI vendor database stats.",
                "capabilities": ["ble.summary", "oui.stats"],
                "ui": [
                    {
                        "type": "header",
                        "title": "BLE & OUI Telemetry",
                        "value": "Local SQLite OUI Index",
                    },
                    {
                        "type": "badge",
                        "title": "Transport",
                        "value": "TRANSPORT_LE (API >= 23)",
                    },
                    {
                        "type": "metric",
                        "title": "OUI Lookup",
                        "value": "Indexed 6-Hex Prefix",
                    },
                    {
                        "type": "text",
                        "title": "Usage",
                        "value": "Resolves bonded and scanned BLE MAC prefixes offline.",
                    },
                    {
                        "type": "button",
                        "title": "Sync BLE Summary",
                        "value": "sync_ble",
                        "action": "ble.summary",
                    },
                ],
            },
        ),
    ]

    results: list[tuple[str, str]] = []
    for filename, payload in plugins:
        digest = canonical_sha256(payload)
        full_doc = dict(payload)
        full_doc["contentSha256"] = digest
        dest = out_dir / filename
        dest.write_text(json.dumps(full_doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        results.append((filename, digest))
    return results


def main() -> None:
    irdb_path = ASSETS_OUT / "dolphyx_irdb.db"
    oui_path = ASSETS_OUT / "dolphyx_oui.db"
    plugins_dir = ASSETS_OUT / "sample_plugins"

    remotes, signals, ir_raw_bytes = build_irdb(irdb_path)
    oui_rows, oui_raw_bytes = build_oui(oui_path)
    plugin_results = write_sample_plugins(plugins_dir)

    irdb_size = irdb_path.stat().st_size
    oui_size = oui_path.stat().st_size
    total_raw = ir_raw_bytes + oui_raw_bytes
    total_packed = irdb_size + oui_size

    print(f"IRDB: remotes={remotes}, signals={signals}, raw={ir_raw_bytes} B ({ir_raw_bytes/1024/1024:.2f} MiB), packed={irdb_size} B ({irdb_size/1024/1024:.2f} MiB)")
    print(f"OUI : rows={oui_rows}, raw={oui_raw_bytes} B ({oui_raw_bytes/1024/1024:.2f} MiB), packed={oui_size} B ({oui_size/1024/1024:.2f} MiB)")
    print(f"TOTAL: raw={total_raw} B ({total_raw/1024/1024:.2f} MiB / {total_raw/1e6:.2f} MB) -> packed={total_packed} B ({total_packed/1024/1024:.2f} MiB / {total_packed/1e6:.2f} MB)")
    for fn, digest in plugin_results:
        print(f"PLUGIN: {fn} -> sha256={digest}")


if __name__ == "__main__":
    main()
