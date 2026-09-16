import ctypes,socket,threading,platform,sys,time
DISCOVERY_PORT=45454;COMMAND_PORT=45455;NAME=platform.node() or 'PC Windows';VK={'MUTE':0xAD,'VOL_DOWN':0xAE,'VOL_UP':0xAF,'NEXT':0xB0,'PREV':0xB1,'PLAY_PAUSE':0xB3};KEYEVENTF_KEYUP=2
def press(v):ctypes.windll.user32.keybd_event(v,0,0,0);time.sleep(.02);ctypes.windll.user32.keybd_event(v,0,KEYEVENTF_KEYUP,0)
def discover():
 s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1);s.bind(('',DISCOVERY_PORT))
 while True:
  data,addr=s.recvfrom(512)
  if data==b'PIIWII_DISCOVER':s.sendto(('PIIWII_REMOTE|%s|PC'%NAME).encode(),addr)
def commands():
 s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM);s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1);s.bind(('',COMMAND_PORT))
 while True:
  data,_=s.recvfrom(512);m=data.decode('utf-8','ignore')
  if m.startswith('PIIWII_CMD|'):
   c=m.split('|',1)[1]
   if c in VK:press(VK[c])
def main():
 if sys.platform!='win32':return
 threading.Thread(target=discover,daemon=True).start();threading.Thread(target=commands,daemon=True).start();import tkinter as tk;r=tk.Tk();r.title('PiiWii Remote Agent 1.0.0');r.geometry('370x190');r.resizable(False,False);tk.Label(r,text='PiiWii Remote Agent',font=('Segoe UI',18,'bold')).pack(pady=(24,6));tk.Label(r,text='PC détectable sur le réseau local').pack();tk.Label(r,text=NAME,font=('Segoe UI',11,'bold')).pack(pady=8);tk.Label(r,text='Volume • Muet • Play/Pause • Suivant • Précédent').pack();r.mainloop()
if __name__=='__main__':main()
