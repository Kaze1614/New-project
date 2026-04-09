import argparse
import json
from pathlib import Path

from docx import Document
from docx.document import Document as DocumentObject
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph


def iter_blocks(parent):
    if isinstance(parent, DocumentObject):
        parent_elm = parent.element.body
    else:
        parent_elm = parent._element

    for child in parent_elm.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, parent)
        elif isinstance(child, CT_Tbl):
            yield Table(child, parent)


def normalize_text(text: str) -> str:
    return " ".join(text.split())


def heading_level(style_name: str):
    if not style_name:
        return None

    lowered = style_name.lower()
    if lowered.startswith("heading "):
        suffix = lowered.replace("heading ", "", 1)
        if suffix.isdigit():
            return int(suffix)
        return None

    if style_name.startswith("\u6807\u9898"):
        digits = "".join(ch for ch in style_name if ch.isdigit())
        return int(digits) if digits else None

    return None


def paragraph_payload(block: Paragraph):
    text = normalize_text(block.text)
    style_name = block.style.name if block.style else ""
    level = heading_level(style_name)
    kind = "heading" if level is not None else "paragraph"
    return {
        "type": kind,
        "style": style_name,
        "level": level,
        "text": text,
    }


def table_payload(block: Table):
    rows = []
    max_columns = 0

    for row_index, row in enumerate(block.rows, start=1):
        cells = []
        for column_index, cell in enumerate(row.cells, start=1):
            cells.append(
                {
                    "row": row_index,
                    "column": column_index,
                    "text": normalize_text(cell.text),
                }
            )
        max_columns = max(max_columns, len(cells))
        rows.append(cells)

    return {
        "type": "table",
        "row_count": len(rows),
        "column_count": max_columns,
        "rows": rows,
    }


def extract_structure(docx_path: Path):
    document = Document(docx_path)
    blocks = []
    headings = []
    paragraphs = []
    tables = []

    for block_index, block in enumerate(iter_blocks(document), start=1):
        if isinstance(block, Paragraph):
            payload = paragraph_payload(block)
            if not payload["text"]:
                continue
            payload["index"] = block_index
            blocks.append(payload)
            if payload["type"] == "heading":
                headings.append(payload)
            else:
                paragraphs.append(payload)
        elif isinstance(block, Table):
            payload = table_payload(block)
            payload["index"] = block_index
            blocks.append(payload)
            tables.append(payload)

    return {
        "source": str(docx_path.resolve()),
        "summary": {
            "block_count": len(blocks),
            "heading_count": len(headings),
            "paragraph_count": len(paragraphs),
            "table_count": len(tables),
        },
        "headings": headings,
        "paragraphs": paragraphs,
        "tables": tables,
        "blocks": blocks,
    }


def main():
    parser = argparse.ArgumentParser(
        description="Extract headings, paragraphs, and table structure from a DOCX file."
    )
    parser.add_argument("input", help="Path to the .docx file")
    parser.add_argument(
        "-o",
        "--output",
        help="Optional output JSON path. Defaults to <input>.structure.json",
    )
    args = parser.parse_args()

    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")

    if input_path.suffix.lower() != ".docx":
        raise SystemExit("Only .docx files are supported.")

    output_path = (
        Path(args.output).expanduser().resolve()
        if args.output
        else input_path.with_suffix(".structure.json")
    )

    data = extract_structure(input_path)
    output_path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(output_path)


if __name__ == "__main__":
    main()
