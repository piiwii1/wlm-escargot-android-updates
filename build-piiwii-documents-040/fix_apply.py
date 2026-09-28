from pathlib import Path
import re
p=Path('build-piiwii-documents-040/apply.py')
s=p.read_text()
pattern=r"'''\.replace\(.*?\)\)"
good="'''.replace(chr(92)+'n', chr(10)))"
s2,count=re.subn(pattern,good,s,flags=re.S)
if count < 2:
    raise SystemExit(f'Expected at least 2 malformed replacements, fixed {count}')
p.write_text(s2)
print(f'Fixed {count} malformed newline replacement(s)')
