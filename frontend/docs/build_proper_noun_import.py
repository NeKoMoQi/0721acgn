# -*- coding: utf-8 -*-
"""
0721ACGN · 专有名词导入表（对齐 Downloads「专有名词导入模板」）

上传校验（与平台文档一致的理解）：
1. 下载模版并按列填写；
2. 专有名词至多 80 字、释义至多 500 字；本平台对超出部分通常会忽略；
3. 文件内专有名词互不重复；且不能与词库云端已有专有名词字面重复；
4. 单库至多 1000 条专有名词术语。

数据源：0721acgn/docs/圣地巡礼-知识库 + 0721acgn/docs/专业词库
输出：0721ACGN-proper-noun-import-时间戳.xlsx（每次新建文件）
"""
from __future__ import annotations

import re
from collections import OrderedDict
from datetime import datetime
from pathlib import Path

from openpyxl import Workbook, load_workbook

DOCS = Path(__file__).resolve().parent
KB_PATH = DOCS / "圣地巡礼-知识库-0721ACGN.md"
GLO_PATH = DOCS / "专业词库-0721ACGN.md"
OUTPUT_PREFIX = "0721ACGN-proper-noun-import"

NAME_MAX = 80
DESC_MAX = 500
MAX_ROWS = 1000

TEMPLATE_CANDIDATES = [
    Path(r"c:\Users\MoQi\Downloads\专有名词导入模板 .xlsx"),
    Path(r"c:\Users\MoQi\Downloads\专有名词导入模板.xlsx"),
]


def get_section(md: str, heading_prefix: str) -> str:
    pat = "^" + re.escape(heading_prefix) + r"[^\n]*\n(.*?)(?=^## |\Z)"
    m = re.search(pat, md, re.MULTILINE | re.DOTALL)
    return m.group(1) if m else ""


def extract_tables(section_body: str) -> list[list[list[str]]]:
    blocks = re.split(r"\n\s*\n", section_body)
    tables: list[list[list[str]]] = []
    for block in blocks:
        rows: list[list[str]] = []
        for line in block.splitlines():
            line_st = line.strip()
            if not line_st:
                continue
            if not line_st.startswith("|"):
                break
            if re.match(r"^\|\s*-+", line_st):
                continue
            rows.append([c.strip() for c in line_st.strip("|").split("|")])
        if len(rows) >= 2:
            tables.append(rows)
    return tables


def norm_dup_key(term: str) -> str:
    t = "".join(term.split())
    t = re.sub(r"（版块）|\(版块\)", "", t)
    return t


def clip(s: str, n: int) -> tuple[str, bool]:
    s = (s or "").strip()
    if len(s) <= n:
        return s, False
    return s[:n], True


def put_unique(
    bag: OrderedDict[str, tuple[str, str]],
    term: str,
    definition: str,
) -> None:
    term_i, defin_i = term.strip(), definition.strip().replace("\r", "").replace("\n", " ")
    defin_i = re.sub(r"\s+", " ", defin_i).strip()
    if not term_i:
        return
    k = norm_dup_key(term_i)
    if k in bag:
        return
    bag[k] = (term_i, defin_i if defin_i else "（暂无释义，请站内补充）")


def ingest_kb_section_one(md: str, bag: OrderedDict[str, tuple[str, str]]) -> None:
    sec = get_section(md, "## 一、")
    for table in extract_tables(sec):
        for row in table:
            if len(row) < 2:
                continue
            if row[0] == "说法":
                continue
            put_unique(bag, row[0], row[1])


def ingest_kb_map_section_two(md: str, bag: OrderedDict[str, tuple[str, str]]) -> None:
    sec = get_section(md, "## 二、")
    for line in sec.splitlines():
        line = line.strip()
        m = re.match(r"^\d+\.\s*\*\*(.+?)\*\*[：:]\s*(.+)$", line)
        if m:
            put_unique(bag, m.group(1).strip(), m.group(2).strip())


def ingest_kb_numbered_bold_section(
    md: str, heading: str, bag: OrderedDict[str, tuple[str, str]]
) -> None:
    sec = get_section(md, heading)
    for line in sec.splitlines():
        line = line.strip()
        m = re.match(r"^\d+\.\s*\*\*(.+?)\*\*[：:]\s*(.+)$", line)
        if m:
            put_unique(bag, m.group(1).strip(), m.group(2).strip())


def ingest_glossary_section_one(md: str, bag: OrderedDict[str, tuple[str, str]]) -> None:
    sec = get_section(md, "## 一、")
    for table in extract_tables(sec):
        for row in table:
            if len(row) < 3:
                continue
            if row[0] == "标准词":
                continue
            std, aliases, note = row[0], row[1], row[2]
            defin = f"站内标准用语：{std}。口语/别名可参考：{aliases}。备注：{note}".strip()
            put_unique(bag, std, defin)


def ingest_glossary_simple_table(
    md: str, heading: str, skip_heads: set[str], bag: OrderedDict[str, tuple[str, str]]
) -> None:
    sec = get_section(md, heading)
    for table in extract_tables(sec):
        for row in table:
            if len(row) < 2:
                continue
            if row[0] in skip_heads:
                continue
            put_unique(bag, row[0], row[1])


def ingest_disambig_section_eight(md: str, bag: OrderedDict[str, tuple[str, str]]) -> None:
    sec = get_section(md, "## 八、")
    for table in extract_tables(sec):
        for row in table:
            if len(row) < 3:
                continue
            if row[0] == "说法 A":
                continue
            term = f"{row[0]}（不宜与「{row[1]}」混为一谈）"
            put_unique(bag, term, row[2])


def collect_raw_entries() -> list[tuple[str, str]]:
    bag: OrderedDict[str, tuple[str, str]] = OrderedDict()
    kb = KB_PATH.read_text(encoding="utf-8")
    gl = GLO_PATH.read_text(encoding="utf-8")

    ingest_kb_section_one(kb, bag)
    ingest_kb_map_section_two(kb, bag)
    ingest_kb_numbered_bold_section(kb, "## 四、", bag)
    ingest_kb_numbered_bold_section(kb, "## 五、", bag)

    ingest_glossary_section_one(gl, bag)
    ingest_glossary_simple_table(gl, "## 二、", {"标准能力 / 控件"}, bag)
    ingest_glossary_simple_table(gl, "## 三、", {"标准概念"}, bag)
    ingest_glossary_simple_table(gl, "## 四、", {"用户说法"}, bag)
    ingest_glossary_simple_table(gl, "## 五、", {"用户说法"}, bag)
    ingest_disambig_section_eight(gl, bag)

    return list(bag.values())


def load_template_workbook():
    for p in TEMPLATE_CANDIDATES:
        if p.is_file():
            wb = load_workbook(p)
            if wb.active["A1"].value is None:
                wb.active["A1"] = "专有名词"
            if wb.active["B1"].value is None:
                wb.active["B1"] = "释义"
            return wb, p
    wb = Workbook()
    ws = wb.active
    ws.title = "Sheet1"
    ws["A1"] = "专有名词"
    ws["B1"] = "释义"
    ws["B2"] = (
        f"专有名词≤{NAME_MAX}字；释义≤{DESC_MAX}字。"
        "单文件勿重复条目；云端已存在同名术语则本条导入失败。单库≤1000条。"
    )
    return wb, None


def main() -> Path:
    raw_rows = collect_raw_entries()

    wb, tpl_path = load_template_workbook()
    ws = wb.active

    while ws.max_row > 1:
        ws.delete_rows(2)

    clipped_names = clipped_desc = 0
    out_keys: set[str] = set()
    written = 0

    for name_raw, def_raw in raw_rows:
        if written >= MAX_ROWS:
            break

        nm, tn = clip(name_raw, NAME_MAX)
        df, td = clip(def_raw, DESC_MAX)

        dk = norm_dup_key(nm)
        if dk in out_keys:
            continue
        out_keys.add(dk)

        clipped_names += int(tn)
        clipped_desc += int(td)

        ws.append([nm, df])
        written += 1

    dropped_after_cap = max(0, len(raw_rows) - MAX_ROWS)

    out_path = DOCS / f"{OUTPUT_PREFIX}-{datetime.now():%Y%m%d-%H%M%S}.xlsx"
    wb.save(out_path)

    print(("模版: " + str(tpl_path)) if tpl_path else "模版: （内置表头）")
    print(f"写入新文件: {out_path}")
    print(
        f"写出 {written} 条（单项上限 {MAX_ROWS}）；"
        f"名称裁剪 {clipped_names} 次；释义裁剪 {clipped_desc} 次；"
        f"源数据超过 {MAX_ROWS} 条时已裁掉尾部 {dropped_after_cap} 条。"
    )
    print("若上传失败条目重复：请先到控制台删掉云端同名专有名词或改本条名称。")

    return out_path


if __name__ == "__main__":
    main()
