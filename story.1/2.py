from dataclasses import dataclass
from pathlib import Path

QUEST = Path(__file__).relative_to(Path.cwd()).with_suffix("")


def notes(part):
    return map(str.strip, open(f"{QUEST}.{part}.in").readlines())


@dataclass
class Node:
    id: str
    value: int
    left = None
    right = None


@dataclass
class Add:
    id: str
    left: Node
    right: Node


@dataclass
class Swap:
    id: str


def commands(part):
    for line in notes(part):
        command, *rest = line.split()
        match command:
            case "ADD":
                id, left, right = rest
                id = id[len("id=") :]
                left_value, left_id = left[len("left=[") : -1].split(",")
                right_value, right_id = right[len("right=[") : -1].split(",")
                yield Add(
                    id, Node(left_id, int(left_value)), Node(right_id, int(right_value))
                )
            case "SWAP":
                (id,) = rest
                yield Swap(id)


class Tree:
    def __init__(self):
        self.root = None

    def insert(self, node):
        def inner(parent, node):
            if parent is None:
                return node
            if node.value < parent.value:
                parent.left = inner(parent.left, node)
            else:
                parent.right = inner(parent.right, node)
            return parent

        self.root = inner(self.root, node)

    def message(self):
        levels = []

        def inner(node, level):
            if node is None:
                return
            if level >= len(levels):
                levels.append([])
            levels[level].append(node.id)
            inner(node.left, level + 1)
            inner(node.right, level + 1)

        inner(self.root, 0)
        return "".join(max(levels, key=lambda x: len(x)))


left_tree = Tree()
right_tree = Tree()
for command in commands(1):
    match command:
        case Add(id, left, right):
            left_tree.insert(left)
            right_tree.insert(right)

print(left_tree.message() + right_tree.message())


left_tree, right_tree = Tree(), Tree()
nodes = {}
for command in commands(2):
    match command:
        case Add(id, left, right):
            left_tree.insert(left)
            right_tree.insert(right)
            nodes[id] = (left, right)
        case Swap(id):
            left, right = nodes[id]
            left.id, right.id = right.id, left.id
            left.value, right.value = right.value, left.value

print(left_tree.message() + right_tree.message())


left_tree, right_tree = Tree(), Tree()
nodes = {}
for command in commands(3):
    match command:
        case Add(id, left, right):
            left_tree.insert(left)
            right_tree.insert(right)
            nodes[id] = (left, right)
        case Swap(id):
            left, right = nodes[id]
            left.id, right.id = right.id, left.id
            left.value, right.value = right.value, left.value
            left.left, right.left = right.left, left.left
            left.right, right.right = right.right, left.right

print(left_tree.message() + right_tree.message())
