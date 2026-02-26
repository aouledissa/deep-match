#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path


MARKER_START = "<!-- deepmatch-version-selector:start -->"
MARKER_END = "<!-- deepmatch-version-selector:end -->"
VERSIONS = ["latest", "0.2.0", "0.1.0"]

SELECTOR_JS = """(function () {
  var select = document.getElementById("deepmatch-version-select");
  if (!select) return;

  var versions = [];
  try {
    versions = JSON.parse(select.getAttribute("data-versions") || "[]");
  } catch (_err) {
    versions = [];
  }
  if (!Array.isArray(versions) || versions.length === 0) return;

  var path = window.location.pathname;
  var query = window.location.search || "";
  var hash = window.location.hash || "";
  var segments = path.split("/").filter(Boolean);
  var versionIndex = -1;

  for (var i = 0; i < segments.length; i++) {
    if (versions.indexOf(segments[i]) >= 0) {
      versionIndex = i;
      break;
    }
  }

  var current = versionIndex >= 0 ? segments[versionIndex] : versions[0];
  if (versions.indexOf(current) >= 0) {
    select.value = current;
  }

  var prefix = versionIndex >= 0 ? segments.slice(0, versionIndex) : [];
  var suffix = versionIndex >= 0 ? segments.slice(versionIndex + 1) : segments;
  var hadTrailingSlash = path.endsWith("/");

  function buildPath(version, includeSuffix) {
    var targetSegments = prefix.concat([version]);
    if (includeSuffix) targetSegments = targetSegments.concat(suffix);
    var next = "/" + targetSegments.join("/");
    if (hadTrailingSlash || includeSuffix) next += "/";
    return next;
  }

  select.addEventListener("change", async function () {
    var version = select.value;
    var preferred = buildPath(version, true);
    var fallback = buildPath(version, false);
    var target = preferred;
    try {
      var response = await fetch(preferred, { method: "HEAD", cache: "no-store" });
      if (!response.ok) target = fallback;
    } catch (_err) {
      target = fallback;
    }
    window.location.href = target + query + hash;
  });
})();"""

SELECTOR_CSS = """.deepmatch-version-selector {
  position: fixed;
  top: 4.25rem;
  right: 1rem;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.55rem;
  border-radius: 0.5rem;
  background: rgba(16, 20, 24, 0.78);
  color: #ffffff;
  font-size: 0.72rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(4px);
}

.deepmatch-version-selector label {
  font-weight: 600;
  letter-spacing: 0.02em;
}

.deepmatch-version-selector select {
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  border-radius: 0.35rem;
  padding: 0.2rem 0.35rem;
  font-size: 0.72rem;
}

.deepmatch-version-selector select option {
  color: #111111;
}

@media (max-width: 760px) {
  .deepmatch-version-selector {
    top: 3.8rem;
    right: 0.6rem;
  }
}"""


def ensure_assets(site_dir: Path) -> None:
  js_path = site_dir / "assets" / "javascripts" / "deepmatch-version-selector.js"
  css_path = site_dir / "assets" / "stylesheets" / "deepmatch-version-selector.css"
  js_path.parent.mkdir(parents=True, exist_ok=True)
  css_path.parent.mkdir(parents=True, exist_ok=True)
  js_path.write_text(SELECTOR_JS + "\n", encoding="utf-8")
  css_path.write_text(SELECTOR_CSS + "\n", encoding="utf-8")


def relative_asset_path(site_dir: Path, html_path: Path, asset: str) -> str:
  rel_parts = html_path.relative_to(site_dir).parts
  depth = max(len(rel_parts) - 1, 0)
  prefix = "../" * depth
  return f"{prefix}assets/{asset}"


def build_snippet(style_href: str, script_src: str) -> str:
  options = "\n".join(
    f'    <option value="{version}">{version}</option>' for version in VERSIONS
  )
  versions_json = "[" + ", ".join(f'"{version}"' for version in VERSIONS) + "]"
  return f"""<!-- deepmatch-version-selector:start -->
<link rel="stylesheet" href="{style_href}">
<div class="deepmatch-version-selector">
  <label for="deepmatch-version-select">Version</label>
  <select id="deepmatch-version-select" aria-label="Select documentation version" data-versions='{versions_json}'>
{options}
  </select>
</div>
<script src="{script_src}"></script>
<!-- deepmatch-version-selector:end -->
"""


def inject_into_html(path: Path) -> bool:
  content = path.read_text(encoding="utf-8")
  site_dir = Path("site")
  style_href = relative_asset_path(site_dir, path, "stylesheets/deepmatch-version-selector.css")
  script_src = relative_asset_path(site_dir, path, "javascripts/deepmatch-version-selector.js")
  snippet = build_snippet(style_href=style_href, script_src=script_src)

  if MARKER_START in content and MARKER_END in content:
    start = content.index(MARKER_START)
    end = content.index(MARKER_END) + len(MARKER_END)
    updated = content[:start] + snippet + content[end:]
    if updated == content:
      return False
    path.write_text(updated, encoding="utf-8")
    return True

  if "</body>" in content:
    updated = content.replace("</body>", snippet + "\n</body>", 1)
  else:
    updated = content + "\n" + snippet + "\n"

  path.write_text(updated, encoding="utf-8")
  return True


def main() -> None:
  site_dir = Path("site")
  if not site_dir.exists():
    raise SystemExit("Missing site directory. Run docs build before injection.")

  ensure_assets(site_dir)

  changed = 0
  for html in site_dir.rglob("*.html"):
    if inject_into_html(html):
      changed += 1
  print(f"Injected version selector into {changed} HTML files.")


if __name__ == "__main__":
  main()
