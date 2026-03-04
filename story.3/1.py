from collections import defaultdict
from functools import reduce
from pathlib import Path

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


def convert(color):
    return reduce(lambda a, c: a * 2 + c.isupper(), color, 0)


def colors(part):
    for line in notes(part):
        id, rest = line.split(":")
        yield int(id), *map(convert, rest.split())


ans = sum(id for id, r, g, b in colors(1) if r < g and b < g)
print(ans)

id, *_ = max(colors(2), key=lambda x: (x[4], -x[1] - x[2] - x[3]))
print(id)

m = defaultdict(list)
for id, r, g, b, s in colors(3):
    scale = "matte" if s <= 30 else "shiny" if s >= 33 else "ignored"
    if scale == "ignored":
        continue
    if r > g and r > b:
        m[("red", scale)].append(id)
    if g > r and g > b:
        m[("green", scale)].append(id)
    if b > r and b > g:
        m[("blue", scale)].append(id)
print(sum(max(m.values(), key=lambda x: len(x))))
