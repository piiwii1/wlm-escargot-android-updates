from pathlib import Path
import hashlib

root=Path(__file__).resolve().parents[1]
android=root/'android'
sprite=android/'app/src/main/res/drawable-nodpi/vw_sprite_192.webp'
main=android/'app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java'
gradle=android/'app/build.gradle'
api=android/'app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java'

def once(text,old,new,label):
    n=text.count(old)
    if n!=1:
        raise SystemExit(f'{label}: expected one anchor, found {n}')
    return text.replace(old,new,1)

bad_sha='8f03e7f9e5f612b60a9d2574de8bd042d19a889e02bf0ae1dce0d4dc4e7ac945'
good_sha='9c7f63e246eaccb4f65b4372ca14c6a4b23ebb57ecde3d07b3df0421fd1561a9'
raw=bytearray(sprite.read_bytes())
sha=hashlib.sha256(raw).hexdigest()
if sha==good_sha:
    print('Volkswagen sprite already repaired')
elif sha==bad_sha:
    patches=[(897, 'ff'), (22562, '980000feff68a50000'), (22572, '0000000000'), (22578, '3b5aeae8739a717b5c9630439979afe235d006fbdc127461ee2628645f54a70d36d506f60670ee3ec7a8da00c865e75db42bbddd8a5fc5151e42805e392da1157fde17fbb6463642d78096fd04b07625dd120bb1af77c457cf7f356966a28e3322a317243ef851e209c996011aa6eb34f07bf59ad8e5b75e7ee051a71c57101bffed5ea8c2d66b2d9afee4024c935bdadbed33a8de2d1f04657dfa0a3f0bf4b68236fe34d2e64f3e7a52e7dd6c5ff0c5f0244879619e2f8b5d77b7520c711e087afe3bf813e6b53a49aac141e911abb81d72d9afb5300111492e91de82365683b52d'), (22805, '311f84abb658ca78100cd267d1e62b5ddbbd5e84a68e6305d277915714a574793a8911323b55be0004ad3c4f5e5f4d1778a01dbb65fe0bba38153a5e9f6e72b73bb7df22645c7ca701958433917c1dd3cfa0b89416b519738f852dea921c8a6fd033a2a42483c71ad4eb67d476afbba6d3e6cf7e910d0ffa672a3ae33d84c23a25d255e95c877bbf8e7faef1bd376384825d8311efd1a78e92de21c84f90d322e0f3bfd23f48b2d86ba0742e2ff7fe7e70e1b518204d97e33b01a594aa39eed3992df8cc44c5a14b4a5c2944a83c0c51b7b226b89fcdfa2f07c4115dfedddda435af95b7be7484270b36d3f33c9eac050c4762b4ea6d7b51f5e0a03e2242408167b4b271a5f049acf3b57f0c624b88e522f35fb25f875e0a0397bc6ab5bf4c7f85de612300f601909a79ec8bd7458091832b2be0303c99e7f63eea653485cbfbc979a3c2c66798b85b78265eaae1935ffcfc767cdfb95e3ab877c0b743471d2c3f81584b0c89e7aa7f5a8ea4299acdc6aed18c07a02afd6348f39bcb7a9784c33087eb6ee3c10cb277388a75daca3aa4776d37a4d17ae7a20195cba4911b7948230955041f4c43581a9dcf0ed5e62a3157a9f579a848386a9c02f52ccd14f6ed2ee602e2aa4539a320eb8f7dbf270c3ce507b958d13842d53a13d392daa1fd6a95c9076a2060dd6948f99cb0cdd8811e7419d5aacc00b1414fdc4353bef7691177857b862fe77aa357ab5d23cf5589591b436fcc280b833f61207699d904'), (23348, '3ce4772085d5b44bef738143d40454f29cbad80a11ab2c8e'), (23373, 'e0'), (23375, '3eb585eaf8f177b2ec7ae01196758099a2f078f1b1b0444e88adf878a6ee9fb791b41c260e322d294073d0b69ceedd2e2ce68916e81e46cc5f150a34579eb89bb0487b06ebf7645242d946ec9b59f62946221dd1ce4411199bfde77d7b0cb75f8c24cd56fb3a6e7e51087f5d79d39b73df9874e471cf42de08b2ee2a170bf31f0f869101cde67ca7dfb88a2ac4c3cceb761347686fa22fa7'), (23528, '0678d5647a29298db8571b95c1cb0811112b35825fd1688c2a3d5bac8811ff4ec252704f9e341ad5d2efe9582cffca3561f2194eb7caa997039606478eadd49934a8779b124e4e0f2b66fd55963cfbc1cf75c5a311f3e7296705d15b7cbdc414ea024af90aa7134f854676b04e105499b9d5d1f33f5e571db44fdf81'), (23653, '028bf97a709e73169c4411a32efcd942f5d055b6e8637be65fb735109cc26258db2317ac92eb198230998315e07008df5d3965e7ff735060f74bbc6f4c393b79419c4c3260e900ab8c654fb63ff88234336ea366c916411022e722a5e9c72e38bc2c830dc173d15b1df2f8ac8ab3b1e02e6b6cf656e99312dc5152a7993c6bbef8c33b8d1ab7a50f5fdaae8aa020da0b9416ddcd9fe7fc1bce2fe043aea10ae9d00e59b91b14f80391e40876592878891ca5bc6d123b3eb4c443768b9ea3c602b6f7f398ec9ce2ef0ee17c14055f3c1459fda225d22b9b7c6d7b784855d5c9477e488082ebabae7c1e8490647034ac2b19e821da2d5bfee9e7473ff8a2b4bedc606053720c2bfec02f8fa9176c5096bb0831b81c1782c1b6cdd501b69debd35cfb547f49f682b4e7332022437f9402a2750a8c6b90b7c064b37057909380fbd3f6cf9aa80d20138f4af8180aa249d411a4f2e606f0f5978883753d')]
    for offset,hexdata in patches:
        data=bytes.fromhex(hexdata)
        raw[offset:offset+len(data)]=data
    if hashlib.sha256(raw).hexdigest()!=good_sha:
        raise SystemExit('Volkswagen sprite repair checksum mismatch')
    sprite.write_bytes(raw)
    print('Volkswagen sprite repaired and checksum verified')
else:
    raise SystemExit(f'Unexpected Volkswagen sprite sha256: {sha}')

m=main.read_text()
m=once(m,'Mode Convoi 0.3.40','Mode Convoi 0.3.41','about version')
needle='        if(VolkswagenIconPack.isVolkswagen(icon)){\n            try{\n                Bitmap b=VolkswagenIconPack.bitmapFor(getResources(),icon);\n                if(b!=null){\n                    ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setPadding(dp(2),dp(2),dp(2),dp(2));iv.setImageBitmap(b);\n                    iv.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));iv.setClipToOutline(false);return iv;\n                }\n            }catch(Exception ignored){}\n        }\n'
replacement=needle+'        if(VolkswagenIconPack.isVolkswagen(icon))icon="🚗";\n'
m=once(m,needle,replacement,'Volkswagen text fallback')
main.write_text(m)

g=gradle.read_text()
g=once(g,'versionCode 43','versionCode 44','versionCode')
g=once(g,"versionName '0.3.40'","versionName '0.3.41'",'versionName')
gradle.write_text(g)

a=api.read_text()
a=once(a,'ModeConvoi-Android/0.3.40','ModeConvoi-Android/0.3.41','User-Agent')
api.write_text(a)

assert hashlib.sha256(sprite.read_bytes()).hexdigest()==good_sha
assert 'Mode Convoi 0.3.41' in main.read_text()
assert 'versionCode 44' in gradle.read_text()
print('Mode Convoi 0.3.41 Volkswagen rendering hotfix ready')
