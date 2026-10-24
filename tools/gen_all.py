"""Regenerates every generated asset in the right order. Run: python tools/gen_all.py"""
import subprocess
import sys
from pathlib import Path

tools = Path(__file__).resolve().parent
for script in ("gen_content.py", "gen_textures.py", "gen_dimension.py", "item3d.py", "mobs.py", "gen_menu.py", "gen_story.py", "lang_uk.py"):
    subprocess.run([sys.executable, str(tools / script)], check=True, cwd=tools)
