from pathlib import Path
p=Path('build-piiwii-documents-040/apply.py')
s=p.read_text()
bad=".replace('\\\\n','" + "\n" + "'))"
good=".replace(chr(92)+'n', chr(10)))"
count=s.count(bad)
if count==0:
    raise SystemExit('Expected malformed newline replacement not found')
s=s.replace(bad,good)
p.write_text(s)
print(f'Fixed {count} malformed newline replacement(s)')
