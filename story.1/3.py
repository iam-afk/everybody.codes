from pathlib import Path

from sympy.ntheory.modular import crt

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


def snails(part):
    for line in notes(part):
        x, y = line.split()
        yield int(x[2:]), int(y[2:])


s = 0
for x, y in snails(1):
    d = x + y - 1
    x = (x - 1 + 100) % d + 1
    y = (y - 1 - 100) % d + 1
    s += x + 100 * y
print(s)

for part in (2, 3):
    disks = []
    rows = []
    for x, y in snails(part):
        disks.append(x + y - 1)
        rows.append(y - 1)
    days, _ = crt(disks, rows)
    print(days)
