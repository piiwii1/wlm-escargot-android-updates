from pathlib import Path
p=Path('mode-convoi-build/ux_039_patch.py')
lines=p.read_text().splitlines()
start=next(i for i,l in enumerate(lines) if l.startswith("needle='''        EditText convoyName=profileInput"))
end=next(i for i in range(start,len(lines)) if lines[i].startswith('assert needle in s;s=s.replace(needle,rep,1)'))
replacement=[
"idx=s.index('        content.addView(profile);', s.index('    private void renderWelcome()'))",
"s=s[:idx]+'        Button vehicleLook=ghostButton(\"🚗   APPARENCE DU VÉHICULE\"); vehicleLook.setOnClickListener(v->vehicleAppearanceDialog()); profile.addView(vehicleLook);\\n'+s[idx:]"
]
lines=lines[:start]+replacement+lines[end+1:]
p.write_text('\n'.join(lines)+'\n')
print('0.3.9 patch prepared')