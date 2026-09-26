"""Embed ODS mappings into the offline page. Usage: python3 tools/sanim-xml/build.py SOURCE.ods"""
import collections
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
NS = {'table': 'urn:oasis:names:tc:opendocument:xmlns:table:1.0',
      'text': 'urn:oasis:names:tc:opendocument:xmlns:text:1.0'}
with zipfile.ZipFile(sys.argv[1]) as source:
    document = ET.fromstring(source.read('content.xml'))
mappings = collections.defaultdict(dict)
for row in document.findall('.//table:table-row', NS):
    cells = []
    for cell in row.findall('table:table-cell', NS):
        value = '\n'.join(''.join(p.itertext()) for p in cell.findall('text:p', NS)).strip()
        repeat = int(cell.get('{%s}number-columns-repeated' % NS['table'], '1'))
        cells.extend([value] * min(repeat, max(0, 4 - len(cells))))
        if len(cells) >= 4:
            break
    if len(cells) == 4 and cells[0].startswith('LTR_'):
        kind, section, title, tag = cells
        if not tag or not title:
            raise ValueError(f'Incomplete mapping: {cells}')
        field = {'title': title, 'section': section, 'defined': title != 'تعریف نشده'}
        if tag in mappings[kind]:
            existing = mappings[kind][tag]
            existing.setdefault('alternatives', [dict(existing)]).append(field)
        else:
            mappings[kind][tag] = field
data = json.dumps(mappings, ensure_ascii=False, separators=(',', ':')).replace('<', '\\u003c')
template = (ROOT / 'tools/sanim-xml/page.html').read_text()
output = ROOT / 'src/main/frontend/public/sanim-xml-translator.html'
output.write_text(template.replace('__MAPPINGS__', data))
print(f'{output}: {len(mappings)} types, {sum(map(len, mappings.values()))} fields')
