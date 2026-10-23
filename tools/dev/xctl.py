"""Drive the Xvfb display: shot FILE | click left|right | key KEYSYM | hold KEYSYM SECONDS | gui X Y (click at GUI coords, scale 2, window at 213,120) | drag X1 Y1 X2 Y2"""
import sys, time
from Xlib import X, XK, display
from Xlib.ext import xtest
from PIL import ImageGrab

d = display.Display(":99")
cmd, *rest = sys.argv[1:]
if cmd == "shot":
    ImageGrab.grab(xdisplay=":99").save(rest[0])
elif cmd == "click":
    btn = {"left": 1, "right": 3}[rest[0]]
    xtest.fake_input(d, X.ButtonPress, btn); d.sync(); time.sleep(0.08)
    xtest.fake_input(d, X.ButtonRelease, btn); d.sync()
elif cmd == "gui":
    x, y = 213 + int(float(rest[0]) * 2), 120 + int(float(rest[1]) * 2)
    xtest.fake_input(d, X.MotionNotify, x=x, y=y); d.sync(); time.sleep(0.15)
    xtest.fake_input(d, X.ButtonPress, 1); d.sync(); time.sleep(0.08)
    xtest.fake_input(d, X.ButtonRelease, 1); d.sync()
elif cmd == "drag":
    x1, y1, x2, y2 = (int(float(v) * 2) for v in rest)
    xtest.fake_input(d, X.MotionNotify, x=213 + x1, y=120 + y1); d.sync(); time.sleep(0.1)
    xtest.fake_input(d, X.ButtonPress, 1); d.sync()
    for step in range(1, 11):
        xtest.fake_input(d, X.MotionNotify, x=213 + x1 + (x2 - x1) * step // 10, y=120 + y1 + (y2 - y1) * step // 10); d.sync(); time.sleep(0.03)
    xtest.fake_input(d, X.ButtonRelease, 1); d.sync()
elif cmd == "type":
    for ch in rest[0]:
        code = d.keysym_to_keycode(XK.string_to_keysym(ch))
        xtest.fake_input(d, X.KeyPress, code); d.sync(); time.sleep(0.04)
        xtest.fake_input(d, X.KeyRelease, code); d.sync(); time.sleep(0.04)
elif cmd in ("key", "hold"):
    code = d.keysym_to_keycode(XK.string_to_keysym(rest[0]))
    xtest.fake_input(d, X.KeyPress, code); d.sync()
    time.sleep(float(rest[1]) if cmd == "hold" else 0.08)
    xtest.fake_input(d, X.KeyRelease, code); d.sync()
d.close()
