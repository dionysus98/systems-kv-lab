#!/usr/bin/env python3
"""Show recorded learning progress as an offline page or a local live view."""

import argparse
from collections import Counter
from datetime import datetime
from html import escape
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
import re
import sys


STAGES = ("queued", "implementing", "reviewing", "verifying", "explaining", "applying", "complete")
STATUSES = ("pending", "explained by tutor", "verified by tutor", "demonstrated by learner")


def plain(value):
    """Keep labels readable without interpreting Markdown as executable HTML."""
    return re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", value).replace("`", "")


def parse_checkpoints(text):
    checkpoints = []
    for section in re.split(r"^## ", text, flags=re.MULTILINE)[1:]:
        title, _, body = section.partition("\n")
        fields = {}
        key = None
        for line in body.splitlines():
            match = re.match(r"^- ([\w ]+):\s*(.*)$", line)
            if match:
                key = match[1].lower()
                fields[key] = match[2].strip()
            elif key and line.startswith("  "):
                fields[key] += " " + line.strip()
            else:
                key = None
        if "stage" not in fields:
            if re.match(r"\d+-", title):
                raise ValueError(f"{title}: missing Stage field")
            continue  # Narrative sections, such as plan changes, are not checkpoints.
        stage = plain(fields["stage"]).lower()
        if stage not in STAGES:
            raise ValueError(f"{title}: unknown stage {stage!r}")
        criteria = []
        table = False
        for line in body.splitlines():
            if line.startswith("### "):
                table = line.strip().lower() == "### acceptance criteria and evidence"
            if not table or not line.startswith("|"):
                continue
            cells = [cell.strip().replace(r"\|", "|")
                     for cell in re.split(r"(?<!\\)\|", line.strip().strip("|"))]
            if cells[0].lower() == "criterion" or re.fullmatch(r"[-: ]+", cells[0]):
                continue
            if len(cells) != 3:
                raise ValueError(f"{title}: evidence rows must have three columns")
            status = plain(cells[1]).lower()
            if status not in STATUSES:
                raise ValueError(f"{title}: unknown evidence status {status!r}")
            criteria.append({"criterion": plain(cells[0]), "status": status,
                             "source": plain(cells[2])})
        checkpoints.append({"id": title.strip(), "stage": stage,
                            "fields": fields, "criteria": criteria})
    if not checkpoints:
        raise ValueError("No checkpoints found. Add a checkpoint with a Stage field.")
    return checkpoints


def current_checkpoint(checkpoints):
    return next((item for item in checkpoints if item["stage"] != "complete"), None)


def label(value):
    return escape(plain(value))


def evidence_summary(criteria):
    if not criteria:
        return '<p class="muted">Acceptance criteria have not been recorded yet.</p>'
    counts = Counter(row["status"] for row in criteria)
    segments = "".join(
        f'<span class="evidence-{index}" style="flex:{counts[status]}"></span>'
        for index, status in enumerate(STATUSES) if counts[status])
    legend = "".join(f'<span><i class="evidence-{index}" aria-hidden="true"></i>'
                     f'{counts[status]} {status}</span>'
                     for index, status in enumerate(STATUSES) if counts[status])
    return (f'<div class="evidence-bar" aria-hidden="true">{segments}</div>'
            f'<div class="legend" aria-label="Evidence counts">{legend}</div>')


def render_page(checkpoints, live=False):
    current = current_checkpoint(checkpoints)
    completed = sum(item["stage"] == "complete" for item in checkpoints)
    queued = sum(item["stage"] == "queued" for item in checkpoints)
    now = datetime.now().astimezone().strftime("%d %b %Y · %H:%M:%S %Z")
    cards = []
    for index, item in enumerate(checkpoints):
        fields, stage, criteria = item["fields"], item["stage"], item["criteria"]
        language = plain(fields.get("language", "Unspecified"))
        steps = []
        for step in STAGES:
            selected = 'class="selected" aria-current="step"' if step == stage else 'class=""'
            steps.append(f'<li {selected}>{step.title()}</li>')
        pipeline = "".join(steps)
        rows = "".join(
            f'<tr><td>{escape(row["criterion"])}</td><td><span class="status evidence-'
            f'{STATUSES.index(row["status"])}">{escape(row["status"])}</span></td>'
            f'<td>{escape(row["source"])}</td></tr>' for row in criteria)
        table = (f'<div class="table-wrap"><table><caption class="sr-only">Acceptance criteria '
                 f'for {label(item["id"])}</caption><thead><tr><th>Criterion</th>'
                 f'<th>Evidence</th><th>Source / remaining work</th></tr></thead><tbody>{rows}'
                 '</tbody></table></div>') if rows else ""
        next_action = fields.get("next learner action", fields.get("next action", "No next action recorded."))
        warning = ""
        if stage == "complete" and (not criteria or any(
                row["status"] in ("pending", "explained by tutor") for row in criteria)):
            warning = '<p class="warning">Review this record: marked complete, with missing or pending evidence.</p>'
        target = fields.get("learning target")
        target_html = f'<p class="target"><strong>Learning target</strong> {label(target)}</p>' if target else ""
        assistance = fields.get("assistance")
        if assistance:
            target_html += f'<p class="target"><strong>Assistance used</strong> {label(assistance)}</p>'
        cards.append(f'''<article class="checkpoint {"current" if item is current else ""}"
data-language="{escape(language, quote=True)}">
<div class="checkpoint-heading"><div><p class="eyebrow">{label(language)} · CHECKPOINT {index + 1:02d}</p>
<h2>{label(item["id"])}</h2></div><span class="stage">{stage.title()}</span></div>
<p>{label(fields.get("outcome", "No outcome recorded."))}</p>
<ol class="pipeline" aria-label="Checkpoint stages">{pipeline}</ol>
{target_html}{evidence_summary(criteria)}{warning}
<details {"open" if item is current else ""}><summary>Evidence &amp; next action</summary>
<p class="next"><strong>Next action</strong> {label(next_action)}</p>{table}</details></article>''')
    options = "".join(f'<option value="{escape(language, quote=True)}">{escape(language)}</option>'
                      for language in dict.fromkeys(plain(item["fields"].get("language", "Unspecified"))
                                                    for item in checkpoints))
    next_text = (current["fields"].get("next learner action", current["fields"].get("next action", "No next action recorded."))
                 if current else "All recorded checkpoints are complete. Plan the next feature with your tutor.")
    refresh = '<a class="refresh" href="/">Refresh progress</a>' if live else '<span>Snapshot · rerun the command to update</span>'
    return f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Learning progress · Systems KV Lab</title><style>
:root {{ color-scheme: light dark; --bg:#f5f5f0; --surface:#fff; --fg:#202b2a; --muted:#53635f;
--line:#d7dfd8; --accent:#176348; --soft:#e6f2e9; --pending:#d8dfdc; --taught:#d5b465;
--verified:#6d94bc; --demonstrated:#438564; }}
@media(prefers-color-scheme:dark) {{ :root {{ --bg:#131c1b; --surface:#1c2826; --fg:#e7eee9;
--muted:#b0c1b9; --line:#3c5049; --accent:#a0dbb5; --soft:#293d33; --pending:#51615a;
--taught:#bca15d; --verified:#749cc5; --demonstrated:#78b491; }} }}
* {{ box-sizing:border-box; }} body {{ margin:0; background:var(--bg); color:var(--fg);
font:16px/1.6 system-ui,sans-serif; }} main {{ max-width:1100px; margin:auto; padding:44px 24px; }}
h1,h2,p {{ margin:0; }} h1 {{ font-size:clamp(2rem,5vw,3.2rem); line-height:1.15; letter-spacing:-.04em; }}
h2 {{ font-size:1.3rem; overflow-wrap:anywhere; }} .eyebrow {{ color:var(--accent); font:700 .75rem/1.5 ui-monospace,monospace;
letter-spacing:.1em; margin-bottom:8px; }} .intro {{ margin:16px 0 28px; max-width:75ch; color:var(--muted); }}
.stats {{ display:flex; gap:32px; flex-wrap:wrap; border-block:1px solid var(--line); padding:20px 0; }}
.stat strong {{ font-size:1.8rem; display:block; line-height:1.25; }} .stat span,.muted {{ color:var(--muted); }}
.focus {{ margin:24px 0; padding:20px; background:var(--soft); border-left:4px solid var(--accent); }}
.focus p {{ margin-top:6px; }} .toolbar,.checkpoint-heading,footer {{ display:flex; align-items:center; justify-content:space-between;
gap:16px; flex-wrap:wrap; }} .toolbar {{ margin:30px 0 16px; }} select {{ font:inherit; color:var(--fg);
background:var(--surface); border:1px solid var(--line); border-radius:6px; padding:8px; max-width:100%; }}
.checkpoint {{ margin:18px 0; background:var(--surface); border:1px solid var(--line); border-radius:12px; padding:24px; }}
.checkpoint.current {{ border-top:4px solid var(--accent); }} .checkpoint p {{ margin:12px 0; overflow-wrap:anywhere; }}
.checkpoint .eyebrow {{ margin:0 0 5px; }} .stage {{ border:1px solid var(--line); padding:4px 12px; border-radius:24px;
font-size:.85rem; }} .pipeline {{ display:flex; flex-wrap:wrap; padding:0; list-style:none; gap:6px; margin:20px 0; }}
.pipeline li {{ padding:4px 9px; font-size:.78rem; color:var(--muted); border-bottom:2px solid var(--line); }}
.pipeline .selected {{ color:var(--accent); border-color:var(--accent); font-weight:750; }}
.target {{ color:var(--muted); }} .target strong,.next strong {{ display:block; color:var(--fg); }}
.evidence-bar {{ display:flex; height:9px; border-radius:5px; overflow:hidden; margin-top:20px; }}
.evidence-0 {{ background:var(--pending); }} .evidence-1 {{ background:var(--taught); }}
.evidence-2 {{ background:var(--verified); }} .evidence-3 {{ background:var(--demonstrated); }}
.legend {{ display:flex; gap:8px 20px; flex-wrap:wrap; font-size:.8rem; margin:8px 0 18px; }}
.legend i {{ display:inline-block; width:9px; height:9px; border-radius:2px; margin-right:5px; }}
details {{ border-top:1px solid var(--line); margin-top:20px; padding-top:14px; }} summary {{ cursor:pointer; font-weight:650; }}
.table-wrap {{ overflow-x:auto; }} table {{ width:100%; border-collapse:collapse; text-align:left; font-size:.85rem; }}
th,td {{ border-bottom:1px solid var(--line); padding:12px 10px; vertical-align:top; overflow-wrap:anywhere; }}
th:first-child,td:first-child {{ padding-left:0; min-width:160px; }} td:last-child {{ min-width:170px; }}
.status {{ display:inline-block; border-radius:4px; padding:2px 6px; color:#15221c; min-width:95px; }}
.warning {{ border-left:3px solid var(--taught); padding-left:12px; }} footer {{ color:var(--muted); font-size:.8rem; margin-top:30px; }}
a {{ color:var(--accent); }} .sr-only {{ position:absolute; width:1px; height:1px; overflow:hidden; clip-path:inset(50%); }}
[hidden] {{ display:none !important; }} @media(max-width:500px) {{ main {{ padding:28px 14px; }}
.checkpoint {{ padding:18px; }} .stats {{ gap:20px; }} .pipeline {{ gap:4px; }} }}
</style></head><body><main><header><p class="eyebrow">SYSTEMS KV LAB / LEARNING RECORD</p>
<h1>Build. Explain. Apply.</h1><p class="intro">Your recorded checkpoints and the evidence behind them.
This view covers the current record, not the full roadmap or earlier journal history. Completion is not a score for mastery.</p></header>
<section class="stats" aria-label="Recorded checkpoint totals"><div class="stat"><strong>{completed} / {len(checkpoints)}</strong>
<span>checkpoints complete</span></div><div class="stat"><strong>{len(checkpoints)-completed-queued}</strong>
<span>in progress</span></div><div class="stat"><strong>{queued}</strong><span>queued</span></div></section>
<section class="focus" aria-label="Next learner action"><strong>{"Your next step" if current else "Ready for the next feature"}</strong>
<p>{label(next_text)}</p></section><div class="toolbar"><strong>Learning path</strong>
<label>Language <select id="language"><option value="">All languages</option>{options}</select></label></div>
<div id="checkpoints">{"".join(cards)}</div><p id="filter-status" class="sr-only" aria-live="polite"></p>
<footer><span>{"Live view · refresh to read the latest record" if live else "Generated"} · {escape(now)}</span>{refresh}</footer>
</main><script>
document.getElementById('language').addEventListener('change', function () {{
  let visible = 0;
  document.querySelectorAll('.checkpoint').forEach(card => {{
    card.hidden = this.value !== '' && card.dataset.language !== this.value;
    if (!card.hidden) visible++;
  }});
  document.getElementById('filter-status').textContent = visible + ' checkpoints shown';
}});
</script></body></html>'''


def read_record(root):
    return parse_checkpoints((root / "learning-checkpoints.md").read_text(encoding="utf-8"))


def render_chat(checkpoints):
    """Produce a read-only Markdown report the tutor can show in its reply."""
    completed = sum(item["stage"] == "complete" for item in checkpoints)
    lines = [f"Recorded checkpoints: {completed} / {len(checkpoints)} complete.", "", "```mermaid", "flowchart TD"]
    for index, item in enumerate(checkpoints):
        # Keep record text out of Mermaid syntax; node IDs are generated locally.
        language = re.sub(r"[^\w .+-]", " ", plain(item["fields"].get("language", "Unspecified")))
        lines.append(f'    step{index}["{index + 1}. {language}: {item["stage"]}"]')
        if index:
            lines.append(f"    step{index - 1} --> step{index}")
    lines.extend(["```", "", "Evidence recorded:"])
    for item in checkpoints:
        counts = Counter(row["status"] for row in item["criteria"])
        evidence = ", ".join(f"{counts[status]} {status}" for status in STATUSES if counts[status])
        # HTML escaping prevents record text becoming markup in rendered Markdown.
        lines.append(f'- {label(item["id"])}: {evidence or "criteria not recorded yet"}.')
    current = current_checkpoint(checkpoints)
    if current:
        fields = current["fields"]
        action = fields.get("next learner action", fields.get("next action", "No next action recorded."))
        lines.extend(["", "Next action: " + label(action)])
    else:
        lines.extend(["", "All recorded checkpoints complete; plan the next feature with the tutor."])
    lines.extend(["", "Current checkpoint record only; not a mastery score or full learning history."])
    return "\n".join(lines)


def serve(root, port):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/":
                self.send_error(404)
                return
            try:
                page, code = render_page(read_record(root), live=True), 200
            except (OSError, ValueError) as error:
                page, code = '<h1>Cannot read progress</h1><p role="alert">' + escape(str(error)) + '</p>', 400
            payload = page.encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(payload)

        def log_message(self, *_args):
            pass

    with HTTPServer(("127.0.0.1", port), Handler) as server:
        print(f"Progress: http://127.0.0.1:{server.server_port} · Ctrl+C to stop", flush=True)
        server.serve_forever()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--serve", action="store_true", help="Serve only the progress view on localhost; refresh to update")
    mode.add_argument("--chat", action="store_true", help="Print a Markdown/Mermaid report for the tutor's reply; writes no files")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    try:
        root = args.root.resolve()
        if not 0 <= args.port <= 65535:
            raise ValueError("Port must be between 0 and 65535.")
        checkpoints = read_record(root)
        if args.chat:
            print(render_chat(checkpoints))
        elif args.serve:
            serve(root, args.port)
        else:
            output = root / "build/progress.html"
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(render_page(checkpoints), encoding="utf-8")
            current = current_checkpoint(checkpoints)
            print(f"{sum(item['stage'] == 'complete' for item in checkpoints)} / {len(checkpoints)} recorded checkpoints complete")
            if current:
                print(f"Next: {current['id']} ({current['stage']})")
            print(f"Open {output}")
    except KeyboardInterrupt:
        return 0
    except (OSError, ValueError) as error:
        print(f"Cannot show progress: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
