from pathlib import Path

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


def data(part):
    for line in notes(part):
        yield map(int, map(lambda s: s[2:], line.split()))


def ans(part, eni):
    return max(
        eni(a, x, m) + eni(b, y, m) + eni(c, z, m) for a, b, c, x, y, z, m in data(part)
    )


def join_remainders(rems):
    n = 0
    for rem in rems[::-1]:
        p = 1
        while p <= rem:
            p *= 10
        n *= p
        n += rem
    return n


def eni(n, exp, mod, f=join_remainders, s=1):
    a = s
    rems = []
    for _ in range(exp):
        a = a * n % mod
        if a == 0:
            break
        rems.append(a)
    return f(rems)


print(ans(1, eni))


def eni2(n, exp, mod):
    a = 1
    rems = {1: 0}
    for i in range(1, exp + 1):
        a = a * n % mod
        if a in rems:
            exp1, t = rems[a], i - rems[a]
            break
        rems[a] = i
    exp2 = (exp - exp1) % t
    for a, i in rems.items():
        if i == (exp2 - 5) % t:
            break

    return eni(n, 5, mod, s=a)


print(ans(2, eni2))


def eni3(n, exp, mod):
    a = 1
    rems = {1: 0}
    for i in range(1, exp + 1):
        a = a * n % mod
        if a in rems:
            exp1, t = rems[a], i - rems[a]
            break
        rems[a] = i
    times, exp2 = divmod(exp - exp1, t)

    return (
        eni(n, exp1, mod, sum)
        + eni(n, t, mod, sum, s=a) * times
        + eni(n, exp2, mod, sum, s=a)
    )


print(ans(3, eni3))
