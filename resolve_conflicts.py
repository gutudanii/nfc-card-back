import os

def resolve_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
        
    new_lines = []
    in_conflict = False
    keep_this_block = False
    modified = False
    
    for line in lines:
        if line.startswith('<<<<<<<'):
            in_conflict = True
            keep_this_block = False
            modified = True
            continue
        elif line.startswith('======='):
            keep_this_block = True
            continue
        elif line.startswith('>>>>>>>'):
            in_conflict = False
            continue
            
        if not in_conflict or keep_this_block:
            new_lines.append(line)
            
    if modified:
        with open(filepath, 'w') as f:
            f.writelines(new_lines)
        print(f"Resolved: {filepath}")

for root, _, files in os.walk('.'):
    for f in files:
        if f.endswith('.java'):
            resolve_file(os.path.join(root, f))
