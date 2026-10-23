"""Tiny RCON client: python3 rcon.py "cmd1" "cmd2" ... prints each response."""
import socket, struct, sys, time

def packet(rid, kind, body):
    data = struct.pack("<ii", rid, kind) + body.encode() + b"\x00\x00"
    return struct.pack("<i", len(data)) + data

def recv(sock):
    n = struct.unpack("<i", sock.recv(4))[0]
    data = b""
    while len(data) < n:
        data += sock.recv(n - len(data))
    rid, kind = struct.unpack("<ii", data[:8])
    return rid, data[8:-2].decode(errors="replace")

s = socket.create_connection(("127.0.0.1", 25575), timeout=30)
s.sendall(packet(1, 3, "skibiditest"))
rid, _ = recv(s)
assert rid == 1, "rcon login failed"
for i, cmd in enumerate(sys.argv[1:], start=2):
    if cmd.startswith("sleep "):
        time.sleep(float(cmd.split()[1])); continue
    s.sendall(packet(i, 2, cmd))
    _, out = recv(s)
    print(f"> {cmd}\n  {out}")
