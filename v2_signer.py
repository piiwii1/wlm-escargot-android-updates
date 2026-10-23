#!/usr/bin/env python3
import base64,hashlib,json,struct,sys
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import serialization,hashes
from cryptography.hazmat.primitives.asymmetric import padding
U32=lambda n:struct.pack('<I',n)
U64=lambda n:struct.pack('<Q',n)
LP=lambda b:U32(len(b))+b
def loc(data):
  pos=data.rfind(b'PK\x05\x06')
  if pos<0: raise ValueError('ZIP EOCD not found')
  cd=struct.unpack_from('<I',data,pos+16)[0]
  assert data[cd:cd+4]==b'PK\x01\x02'
  assert pos+22+struct.unpack_from('<H',data,pos+20)[0]==len(data)
  return cd,pos
def content_digest(src):
  cd,eo=loc(src)
  chunks=[]
  for section in (src[:cd],src[cd:eo],src[eo:]):
    for i in range(0,len(section),1048576):
      block=section[i:i+1048576]
      chunks.append(hashlib.sha256(b'\xa5'+U32(len(block))+block).digest())
  return hashlib.sha256(b'Z'+U32(len(chunks))+b''.join(chunks)).digest()
def signable(src,cert):
  d=content_digest(src)
  return LP(LP(U32(0x103)+LP(d)))+LP(LP(cert))+LP(b'')
def main():
  mode=sys.argv[1]; p=Path('release'); src=(p/'WidgEvent-1.5.5-UNSIGNED.apk').read_bytes()
  cert=base64.b64decode(Path('signing-public-certificate.b64').read_text().strip())
  message=signable(src,cert)
  if mode=='prepare':
    req={'sha256':hashlib.sha256(src).hexdigest(),'certificate_sha256':hashlib.sha256(cert).hexdigest(),'data_base64':base64.b64encode(message).decode()}
    Path('signing-request.json').write_text(json.dumps(req,indent=2)+'\n')
    print('Signature request ready: ',req['sha256'])
    return
  req=json.loads(Path('signing-request.json').read_text())
  assert req['sha256']==hashlib.sha256(src).hexdigest()
  assert base64.b64decode(req['data_base64'])==message
  response=json.loads(Path('signing-response.json').read_text())
  sig=base64.b64decode(response['signature_base64'])
  public=x509.load_der_x509_certificate(cert).public_key()
  public.verify(sig,message,padding.PKCS1v15(),hashes.SHA256())
  pub=public.public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
  signer=LP(message)+LP(LP(U32(0x103)+LP(sig)))+LP(pub)
  value=LP(LP(signer))
  pair=U64(len(value)+4)+U32(0x7109871a)+value
  size=len(pair)+24
  magic=b'APK Sig Block 42'
  block=U64(size)+pair+U64(size)+magic
  cd,eo=loc(src); ending=bytearray(src[eo:]);struct.pack_into('<I',ending,16,cd+len(block))
  result=src[:cd]+block+src[cd:eo]+ending
  target=p/'WidgEvent-1.5.5.apk';target.write_bytes(result)
  (p/'SHA256SUMS.txt').write_text(hashlib.sha256(result).hexdigest()+'  WidgEvent-1.5.5.apk\n')
  print('SIGNED APK SHA256:',hashlib.sha256(result).hexdigest())
if __name__=='__main__':main()
