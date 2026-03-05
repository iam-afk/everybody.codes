from collections import deque
from pathlib import Path

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


def start_and_bones(part):
    bones = set()
    for r, row in enumerate(notes(part)):
        for c, cell in enumerate(row):
            if cell == "@":
                start = (r, c)
            if cell == "#":
                bones.add((r, c))
    return start, bones


dr, dc = -1, 0
(r, c), bones = start_and_bones(1)
g = set()
step = 0
while (r, c) not in bones:
    g.add((r, c))
    if (r + dr, c + dc) not in g:
        r, c = r + dr, c + dc
        step += 1
    dr, dc = dc, -dr
print(step)


g = set()
(r, c), bones = start_and_bones(2)
mnr, mnc, mxr, mxc = 0, 0, max(r for r, _ in bones), max(c for _, c in bones)


def surrounded(positions, current):
    q = deque(positions)
    v = set(positions)
    while q:
        r, c = q.popleft()
        if r < mnr or c < mnc or r > mxr or c > mxc:
            return False
        for dr, dc in [(0, 1), (1, 0), (0, -1), (-1, 0)]:
            nx = r + dr, c + dc
            if nx in g or nx == current or nx in bones or nx in v:
                continue
            q.append(nx)
            v.add(nx)
    return True


dr, dc = -1, 0
step = 0
while not surrounded(bones, (r, c)):
    g.add((r, c))
    nx = r + dr, c + dc
    if nx not in g and nx not in bones and not surrounded((nx,), (r, c)):
        r, c = nx
        mnr, mnc = min(mnr, r), min(mnc, c)
        mxr, mxc = max(mxr, r), max(mxc, c)
        step += 1
    dr, dc = dc, -dr
print(step)


g = set()
(r, c), bones = start_and_bones(3)
mnr, mnc, mxr, mxc = 0, 0, max(r for r, _ in bones), max(c for _, c in bones)

dr, dc = -1, 0
step = 0
times = 3
while not surrounded(bones, (r, c)):
    g.add((r, c))
    nx = r + dr, c + dc
    if nx not in g and nx not in bones and not surrounded((nx,), (r, c)):
        r, c = nx
        mnr, mnc = min(mnr, r), min(mnc, c)
        mxr, mxc = max(mxr, r), max(mxc, c)
        step += 1
        times -= 1
    else:
        times = 0
    if times == 0:
        dr, dc = dc, -dr
        times = 3

print(step)
