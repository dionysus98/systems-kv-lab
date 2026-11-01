import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { ExtensionAPI, Theme } from "@earendil-works/pi-coding-agent";
import { matchesKey, truncateToWidth } from "@earendil-works/pi-tui";

type Phase = "orient" | "predict" | "attempt" | "diagnose" | "explain" | "transfer" | "complete" | "blocked";
type Status = "success" | "warning" | "error" | "prompt";

interface EvidenceCount {
	done: number;
	total: number;
}

interface Observation {
	time: string;
	kind: string;
	status: Status;
	text: string;
}

interface Fixture {
	id: string;
	checkpoint: string;
	language: string;
	phase: Phase;
	helpLevel: number;
	goal: string;
	challenge: string;
	nextAction: string;
	evidence: {
		behavior: EvidenceCount;
		explanation: EvidenceCount;
		transfer: EvidenceCount;
	};
	observations: Observation[];
}

interface FixtureFile {
	fixtures: Fixture[];
}

const phases: Phase[] = ["orient", "predict", "attempt", "diagnose", "explain", "transfer", "complete"];

function loadFixtures(): Fixture[] {
	const path = join(process.cwd(), "design", "learning-machine", "fixtures.json");
	const parsed = JSON.parse(readFileSync(path, "utf8")) as FixtureFile;
	if (!parsed.fixtures.length) throw new Error("No learning-machine fixtures found");
	return parsed.fixtures;
}

function bar(value: EvidenceCount, width: number): string {
	const ratio = value.total === 0 ? 0 : value.done / value.total;
	const filled = Math.round(Math.max(0, Math.min(1, ratio)) * width);
	return "█".repeat(filled) + "░".repeat(Math.max(0, width - filled));
}

function phaseIcon(current: Phase, phase: Phase): string {
	const currentIndex = phases.indexOf(current);
	const phaseIndex = phases.indexOf(phase);
	if (phaseIndex < currentIndex) return "✓";
	if (phaseIndex === currentIndex) return "→";
	return "○";
}

function statusIcon(status: Status): string {
	if (status === "success") return "✓";
	if (status === "warning") return "⚠";
	if (status === "error") return "✗";
	return "◆";
}

function colorObservation(theme: Theme, observation: Observation): string {
	const prefix = `${observation.time}  ${observation.kind.padEnd(8)} ${statusIcon(observation.status)} `;
	const color = observation.status === "success"
		? "success"
		: observation.status === "warning"
			? "warning"
			: observation.status === "error"
				? "error"
				: "accent";
	return theme.fg(color, prefix) + theme.fg("muted", observation.text);
}

class Cockpit {
	private cachedWidth?: number;
	private cachedLines?: string[];
	private fixtureIndex = 0;

	constructor(
		private readonly fixtures: Fixture[],
		private readonly theme: Theme,
		private readonly close: () => void,
		private readonly tui: { requestRender: () => void },
	) {}

	private get fixture(): Fixture {
		return this.fixtures[this.fixtureIndex];
	}

	private invalidate(): void {
		this.cachedWidth = undefined;
		this.cachedLines = undefined;
	}

	private panel(title: string, lines: string[], width: number): string[] {
		const inner = Math.max(8, width - 4);
		const top = this.theme.fg("border", `┌─ ${title} ${"─".repeat(Math.max(0, inner - title.length - 3))}┐`);
		const body = lines.map((line) => this.theme.fg("border", "│ ") + truncateToWidth(line, inner) + this.theme.fg("border", " │"));
		const bottom = this.theme.fg("border", `└${"─".repeat(width - 2)}┘`);
		return [truncateToWidth(top, width), ...body, truncateToWidth(bottom, width)];
	}

	private renderHeader(width: number): string {
		const f = this.fixture;
		const title = this.theme.fg("accent", " LEARNING MACHINE ");
		const status = this.theme.fg("warning", `● HELP ${f.helpLevel}/4`);
		const left = `${title}${this.theme.fg("muted", ` ${f.language} FIXED STORE · ${f.checkpoint} · `)}${this.theme.fg("accent", f.phase.toUpperCase())}`;
		return truncateToWidth(left + " ".repeat(Math.max(1, width - left.length - status.length)) + status, width);
	}

	render(width: number): string[] {
		if (this.cachedLines && this.cachedWidth === width) return this.cachedLines;
		const f = this.fixture;
		const lines: string[] = ["", this.renderHeader(width), this.theme.fg("border", "─".repeat(width))];
		const narrow = width < 90;
		if (narrow) {
			lines.push(...this.panel("ACTIVE CHALLENGE", [
				this.theme.fg("accent", f.goal),
				"",
				f.challenge,
				"",
				this.theme.fg("warning", `Next: ${f.nextAction}`),
			], width));
			lines.push(...this.panel("EVIDENCE", this.evidenceLines(f), width));
			lines.push(...this.panel("OBSERVATIONS", this.observationLines(f), width));
		} else {
			const gap = 1;
			const leftWidth = Math.max(24, Math.floor((width - gap * 2) * 0.22));
			const rightWidth = Math.max(28, Math.floor((width - gap * 2) * 0.25));
			const centerWidth = Math.max(32, width - leftWidth - rightWidth - gap * 2);
			const left = this.panel("CURRICULUM", this.curriculumLines(f), leftWidth);
			const center = this.panel("ACTIVE CHALLENGE", [
				this.theme.fg("accent", f.goal),
				"",
				f.challenge,
				"",
				this.theme.fg("warning", `Next: ${f.nextAction}`),
			], centerWidth);
			const right = this.panel("EVIDENCE", this.evidenceLines(f), rightWidth);
			const rows = Math.max(left.length, center.length, right.length);
			for (let i = 0; i < rows; i++) {
				lines.push([
					truncateToWidth(left[i] ?? " ".repeat(leftWidth), leftWidth),
					" ",
					truncateToWidth(center[i] ?? " ".repeat(centerWidth), centerWidth),
					" ",
					truncateToWidth(right[i] ?? " ".repeat(rightWidth), rightWidth),
				].join(""));
			}
		}
		lines.push(this.theme.fg("border", "─".repeat(width)));
		lines.push(...this.panel("OBSERVATIONS", this.observationLines(f), width));
		lines.push(this.theme.fg("muted", " [1] orient  [2] diagnose  [3] complete   [h] more help   [esc] close"));
		lines.push("");
		this.cachedWidth = width;
		this.cachedLines = lines.map((line) => truncateToWidth(line, width));
		return this.cachedLines;
	}

	private curriculumLines(f: Fixture): string[] {
		return phases.map((phase) => {
			const icon = phaseIcon(f.phase, phase);
			const label = phase === f.phase ? this.theme.fg("accent", phase) : this.theme.fg("muted", phase);
			return `${icon} ${label}`;
		});
	}

	private evidenceLines(f: Fixture): string[] {
		const rows = [
			["BEHAVIOR", f.evidence.behavior],
			["EXPLAIN", f.evidence.explanation],
			["TRANSFER", f.evidence.transfer],
		] as const;
		return [
			...rows.flatMap(([label, value]) => [
				this.theme.fg("muted", `${label.padEnd(9)} ${value.done}/${value.total}`),
				this.theme.fg(value.done === value.total ? "success" : "accent", `  ${bar(value, 12)}`),
			]),
			"",
			this.theme.fg("warning", `Help level: ${f.helpLevel}/4`),
		];
	}

	private observationLines(f: Fixture): string[] {
		return f.observations.length
			? f.observations.map((observation) => colorObservation(this.theme, observation))
			: [this.theme.fg("muted", "No observations yet. Make a prediction first.")];
	}

	handleInput(data: string): void {
		if (matchesKey(data, "escape") || matchesKey(data, "ctrl+c")) {
			this.close();
			return;
		}
		const index = data === "1" ? 0 : data === "2" ? 1 : data === "3" ? 2 : -1;
		if (index >= 0 && index < this.fixtures.length) {
			this.fixtureIndex = index;
			this.invalidate();
			this.tui.requestRender();
			return;
		}
		if (data.toLowerCase() === "h") {
			this.fixture.helpLevel = Math.min(4, this.fixture.helpLevel + 1);
			this.invalidate();
			this.tui.requestRender();
		}
	}
}

export default function learningMachine(pi: ExtensionAPI) {
	pi.registerCommand("learn", {
		description: "Open the Learning Machine cockpit",
		handler: async (_args, ctx) => {
			if (ctx.mode !== "tui") {
				ctx.ui.notify("/learn requires interactive TUI mode", "error");
				return;
			}
			try {
				const fixtures = loadFixtures();
				await ctx.ui.custom<void>((tui, theme, _keybindings, done) => new Cockpit(fixtures, theme, done, tui));
			} catch (error) {
				ctx.ui.notify(`Learning Machine failed: ${error instanceof Error ? error.message : String(error)}`, "error");
			}
		},
	});
}
