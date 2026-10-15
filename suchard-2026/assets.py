from pathlib import Path
from PIL import Image, ImageDraw
import subprocess, hashlib
d=Path("app/src/main/res/drawable-nodpi")
d.mkdir(parents=True,exist_ok=True)
assets={
"poster":"139bd4_1468059667b74942aee49e6d4c8859d6~mv2.jpg",
"hero_history":"139bd4_b7e355daf6774ec1a862ff00b69ff821~mv2.png",
"fresco":"0c8f50_13f5279b8f02429e88f4c3cc70c665bc~mv2.jpg",
"factory":"139bd4_46d17acc7bfb409080cd1aa00f80e729~mv2.jpg",
"milka":"0c8f50_83a6fab30d96466487d6e6fca1841042~mv2.jpg"
}
with open("OFFICIAL-ASSETS.txt","w") as out:
 for name,filename in assets.items():
  url="https://static.wixstatic.com/media/"+filename
  tmp="/tmp/suchard_asset_"+name
  status="OFFICIAL"
  try:
   subprocess.run(["curl","-fLsS","--retry","2","--max-time","45","-o",tmp,url],timeout=55,check=True)
   with Image.open(tmp) as im: image=im.convert("RGB")
   if min(image.size)<200: raise RuntimeError("image too small")
   image.thumbnail((1100,1600),Image.Resampling.LANCZOS)
  except Exception as e:
   status="FALLBACK"; print("Asset failed",name,str(e),flush=True)
   image=Image.new("RGB",(1060,710),(94,48,42))
   painter=ImageDraw.Draw(image)
   for z in range(6):
    painter.rounded_rectangle([80+z*40,80+z*32,980-z*40,600-z*32],radius=36,outline=(210,105,138),width=8)
  path=d/(name+".jpg")
  image.save(path,quality=83,optimize=True)
  out.write(f"{name} | {status} | {url} | sha256 {hashlib.sha256(path.read_bytes()).hexdigest()}\n")
  print("ASSET",name,status,path.stat().st_size,flush=True)
