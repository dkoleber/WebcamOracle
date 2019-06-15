from webcam_manager import *


text = 'test text'
m = WebcamManager()
n,c,t = m.encrypt(text)
d = m.decrypt(n,c,t)

print(text)
print(n,c,t)
print(d)


