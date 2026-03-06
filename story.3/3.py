from dataclasses import dataclass
from pathlib import Path

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


def elements(part):
    for line in notes(part):
        id, plug, left, right, data = line.split(", ")
        yield (
            int(id[len("id=") :]),
            plug[len("plug=") :].split(),
            left[len("leftSocket=") :].split(),
            right[len("rightSocket=") :].split(),
            data[len("data=") :],
        )


@dataclass
class Node:
    id: int
    plug: list[str]
    left_plug: list[str]
    right_plug: list[str]
    left: Node | None = None
    right: Node | None = None
    data: str | None = None

    def swap(self, other):
        self.id, self.plug, other.id, other.plug = (
            other.id,
            other.plug,
            self.id,
            self.plug,
        )
        self.left_plug, self.right_plug, other.left_plug, other.right_plug = (
            other.left_plug,
            other.right_plug,
            self.left_plug,
            self.right_plug,
        )
        self.left, self.right, other.left, other.right = (
            other.left,
            other.right,
            self.left,
            self.right,
        )
        self.data, other.data = other.data, self.data


def checksum(part, insert):
    root = None
    for id, plug, left, right, _ in elements(part):
        node = Node(id, plug, left, right)
        if root is None:
            root = node
        else:
            while not insert(root, node):
                pass

    def calculate_checksum(node, index):
        if node is None:
            return 0, 0
        left, left_count = calculate_checksum(node.left, index)
        right, right_count = calculate_checksum(node.right, index + left_count + 1)
        return left + node.id * (
            index + left_count
        ) + right, left_count + right_count + 1

    result, _ = calculate_checksum(root, 1)
    return result


def insert1(current, node):
    if current.left is None:
        if node.plug == current.left_plug:
            current.left = node
            return True
    else:
        if insert1(current.left, node):
            return True
    if current.right is None:
        if node.plug == current.right_plug:
            current.right = node
            return True
    else:
        if insert1(current.right, node):
            return True
    return False


print(checksum(1, insert1))


def insert2(current, node):
    if current.left is None:
        if node.plug[0] == current.left_plug[0] or node.plug[1] == current.left_plug[1]:
            current.left = node
            return True
    else:
        if insert2(current.left, node):
            return True
    if current.right is None:
        if (
            node.plug[0] == current.right_plug[0]
            or node.plug[1] == current.right_plug[1]
        ):
            current.right = node
            return True
    else:
        if insert2(current.right, node):
            return True
    return False


print(checksum(2, insert2))


def insert3(current, node):
    if current.left is None:
        if node.plug[0] == current.left_plug[0] or node.plug[1] == current.left_plug[1]:
            current.left = node
            return True
    else:
        if node.plug == current.left_plug and current.left.plug != current.left_plug:
            node.swap(current.left)
        elif insert3(current.left, node):
            return True
    if current.right is None:
        if (
            node.plug[0] == current.right_plug[0]
            or node.plug[1] == current.right_plug[1]
        ):
            current.right = node
            return True
    else:
        if node.plug == current.right_plug and current.right.plug != current.right_plug:
            node.swap(current.right)
        elif insert3(current.right, node):
            return True
    return False


print(checksum(3, insert3))
