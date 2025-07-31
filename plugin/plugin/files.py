import os


def list_files(start_path, indent=0):
    items = sorted(os.listdir(start_path))
    for item in items:
        path = os.path.join(start_path, item)
        print("  " * indent + "|-- " + item)
        if os.path.isdir(path):
            list_files(path, indent + 1)


if __name__ == "__main__":
    root = "."
    print(root)
    list_files(root)
