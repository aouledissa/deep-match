(function () {
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
})();
