import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { ExtensionAPI, ExtensionCommandContext, Theme } from "@earendil-works/pi-coding-agent";
import { matchesKey, truncateToWidth, visibleWidth } from "@earendil-works/pi-tui";
import { dispatch, initialState, type LearningCommand, type LearningState } from "../../learning-machine/engine.ts";

type Phase = "orient" | "predict" | "attempt" | "observe" | "explain" | "transfer" | "complete" | "blocked";
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

const phases: Phase[] = ["orient", "predict", "attempt", "observe", "explain", "transfer", "complete"];
let engineState: LearningState = initialState();

function syncEngine(fixture: Fixture): void {
	const phase = fixture.phase === "blocked" ? "orient" : fixture.phase;
	engineState = {
		...engineState,
		checkpoint: fixture.checkpoint,
		language: fixture.language,
		phase: phase as LearningState["phase"],
		goal: fixture.goal,
		nextAction: fixture.nextAction,
		helpLevel: fixture.helpLevel,
	};
}

function applyCommand(command: LearningCommand): void {
	dispatch(engineState, command);
}

function loadFixtures(): Fixture[] {
	const path = join(process.cwd(), "design", "learning-machine", "fixtures.json");
	const parsed = JSON.parse(readFileSync(path, "utf8")) as FixtureFile;
	if (!parsed.fixtures.length) throw new Error("No learning-machine fixtures found");
	return parsed.fixtures;
}

function field(body: string, name: string): string {
	const match = body.match(new RegExp(`^- ${name}:\\s*(.*(?:\\n  .*)*)`, "m"));
	return match?.[1]?.replace(/\n  /g, " ").trim() ?? "";
}

function phaseForStage(stage: string): Phase {
	if (stage === "implementing") return "attempt";
	if (stage === "reviewing" || stage === "verifying") return "diagnose";
	if (stage === "explaining") return "explain";
	if (stage === "applying") return "transfer";
	return "orient";
}

function countEvidence(body: string, status: string): EvidenceCount {
	const table = body.split("### Acceptance criteria and evidence")[1] ?? "";
	const rows = table.split("\n").filter((line) => /\|\s*(pending|explained by tutor|verified by tutor|demonstrated by learner)\s*\|/i.test(line));
	const relevant = rows.filter((line) => line.toLowerCase().includes(`| ${status.toLowerCase()} |`));
	return { done: relevant.length, total: rows.length };
}

function loadCurrentCheckpoint(): Fixture {
	const path = join(process.cwd(), "learning-checkpoints.md");
	const source = readFileSync(path, "utf8");
	const sections = source.split(/^## /m).slice(1);
	const section = sections.find((candidate) => !/^.*^- Stage: `complete`/ms.test(candidate));
	if (!section) throw new Error("No incomplete checkpoint found");
	const [title, ...bodyParts] = section.split("\n");
	const body = bodyParts.join("\n");
	const stage = field(body, "Stage").replaceAll("`", "") || "queued";
	const language = field(body, "Language") || "Unknown";
	const target = field(body, "Learning target") || "Continue the current learning checkpoint.";
	const nextAction = field(body, "Next learner action") || "Choose the next bounded learner action.";
	const behavior = countEvidence(body, "verified by tutor");
	const demonstrated = countEvidence(body, "demonstrated by learner");
	const total = Math.max(1, behavior.total);
	return {
		id: title.trim(),
		checkpoint: title.trim(),
		language,
		phase: phaseForStage(stage),
		helpLevel: 1,
		goal: target,
		challenge: field(body, "Outcome") || "Work on the current checkpoint without skipping its acceptance criteria.",
		nextAction,
		evidence: {
			behavior: { done: Math.min(total, behavior.done), total },
			explanation: { done: stage === "explaining" || stage === "applying" ? 1 : 0, total: 1 },
			transfer: { done: demonstrated.done > 0 ? 1 : 0, total: 1 },
		},
		observations: [{ time: "now", kind: "STATE", status: "prompt", text: "Checkpoint loaded" }],
	};
}

function wrapText(text: string, width: number): string[] {
	const words = text.split(/\s+/).filter(Boolean);
	const lines: string[] = [];
	let current = "";
	for (const word of words) {
		if (!current) {
			current = word;
		} else if (current.length + 1 + word.length <= width) {
			current += ` ${word}`;
		} else {
			lines.push(current);
			current = word;
		}
	}
	if (current) lines.push(current);
	return lines.length ? lines : [""];
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
		private readonly onCommand: (command: LearningCommand) => void,
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
		const titleText = ` ${title} `;
		const top = this.theme.fg("border", `┌─${titleText}${"─".repeat(Math.max(0, width - visibleWidth(titleText) - 3))}┐`);
		const padLine = (line: string) => {
			const content = truncateToWidth(line, inner, "…", true);
			return this.theme.fg("border", "│ ") + content + " ".repeat(Math.max(0, inner - visibleWidth(content))) + this.theme.fg("border", " │");
		};
		const body = lines.map(padLine);
		const bottom = this.theme.fg("border", `└${"─".repeat(Math.max(0, width - 2))}┘`);
		return [truncateToWidth(top, width), ...body, bottom];
	}

	private renderHeader(width: number): string {
		const f = this.fixture;
		const left = this.theme.fg("accent", "LEARNING MACHINE") + this.theme.fg("muted", `  ${f.language} · ${f.checkpoint} · `) + this.theme.fg("accent", f.phase.toUpperCase());
		const status = this.theme.fg("warning", `HELP ${f.helpLevel}/4`);
		return truncateToWidth(left + " ".repeat(Math.max(1, width - visibleWidth(left) - visibleWidth(status))) + status, width);
	}

	render(width: number): string[] {
		if (this.cachedLines && this.cachedWidth === width) return this.cachedLines;
		const f = this.fixture;
		const lines: string[] = ["", this.renderHeader(width), this.theme.fg("border", "─".repeat(width))];
		const narrow = width < 90;
		if (narrow) {
			lines.push(...this.panel("CHALLENGE", this.challengeLines(f, width), width));
			lines.push(...this.panel("EVIDENCE", this.evidenceLines(f), width));
		} else {
			const gap = 1;
			const leftWidth = Math.max(24, Math.floor((width - gap * 2) * 0.22));
			const rightWidth = Math.max(28, Math.floor((width - gap * 2) * 0.25));
			const centerWidth = Math.max(32, width - leftWidth - rightWidth - gap * 2);
			const left = this.panel("PHASES", this.curriculumLines(f), leftWidth);
			const center = this.panel("CHALLENGE", this.challengeLines(f, centerWidth), centerWidth);
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
		lines.push(this.theme.fg("muted", " 1/2/3 demo states   h hint   esc close"));
		lines.push("");
		this.cachedWidth = width;
		this.cachedLines = lines.map((line) => truncateToWidth(line, width));
		return this.cachedLines;
	}

	private challengeLines(f: Fixture, width: number): string[] {
		const inner = Math.max(10, width - 4);
		return [
			...wrapText(f.goal, inner).map((line) => this.theme.fg("accent", line)),
			"",
			...wrapText(f.challenge, inner),
			"",
			...wrapText(`Next: ${f.nextAction}`, inner).map((line) => this.theme.fg("warning", line)),
		];
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
			["CHECKS", f.evidence.behavior],
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
			this.onCommand({ type: "request_hint" });
			this.fixture.helpLevel = Math.min(4, this.fixture.helpLevel + 1);
			this.invalidate();
			this.tui.requestRender();
		}
	}
}

export default function learningMachine(pi: ExtensionAPI) {
	pi.on("before_agent_start", (event) => {
		try {
			const checkpoint = loadCurrentCheckpoint();
			event.systemPromptOptions.sections.learning_machine = [
				"Learning Machine mode is active for this project.",
				`Current checkpoint: ${checkpoint.checkpoint} (${checkpoint.language})`,
				`Phase: ${checkpoint.phase}`,
				`Learning target: ${checkpoint.goal}`,
				`Next learner action: ${checkpoint.nextAction}`,
				"Preserve learner agency: do not implement the substantive exercise unless explicitly asked for a worked solution.",
				"Prefer a goal, prediction, bounded attempt, observation, explanation, and small transfer variation.",
				"Distinguish passing behavior from learner explanation and independent transfer.",
			].join("\\n");
		} catch {
			delete event.systemPromptOptions.sections.learning_machine;
		}
	});

	const openCockpit = async (ctx: ExtensionCommandContext, fixtures: Fixture[]) => {
		if (ctx.mode !== "tui") {
			ctx.ui.notify("This Learning Machine command requires interactive TUI mode", "error");
			return;
		}
		try {
			syncEngine(fixtures[0]);
			await ctx.ui.custom<void>((tui, theme, _keybindings, done) => new Cockpit(fixtures, theme, done, tui, applyCommand));
		} catch (error) {
			ctx.ui.notify(`Learning Machine failed: ${error instanceof Error ? error.message : String(error)}`, "error");
		}
	};

	const openCurrent = async (ctx: ExtensionCommandContext) => {
		try {
			return await openCockpit(ctx, [loadCurrentCheckpoint()]);
		} catch (error) {
			ctx.ui.notify(`Learning Machine failed: ${error instanceof Error ? error.message : String(error)}`, "error");
		}
	};

	pi.registerCommand("learn", {
		description: "Open the Learning Machine cockpit for the current checkpoint",
		handler: async (_args, ctx) => openCurrent(ctx),
	});

	pi.registerCommand("learn-demo", {
		description: "Open the Learning Machine visual fixture demo",
		handler: async (_args, ctx) => openCockpit(ctx, loadFixtures()),
	});

	pi.registerCommand("progress", {
		description: "Show current learning progress",
		handler: async (_args, ctx) => openCurrent(ctx),
	});

	pi.registerCommand("hint", {
		description: "Show a small hint for the current checkpoint",
		handler: async (_args, ctx) => {
		try {
			const checkpoint = loadCurrentCheckpoint();
			applyCommand({ type: "request_hint" });
			ctx.ui.notify(`Hint level ${engineState.helpLevel}: focus on this next action — ${checkpoint.nextAction}`, "info");
		} catch (error) {
			ctx.ui.notify(`Learning Machine failed: ${error instanceof Error ? error.message : String(error)}`, "error");
		}
	},
	});

	const recordResponse = async (kind: "prediction" | "explanation" | "reflection", ctx: ExtensionCommandContext) => {
		if (!ctx.hasUI) {
			ctx.ui.notify(`/${kind} requires interactive input`, "error");
			return;
		}
		const prompt = kind === "prediction"
			? "Before running anything, what do you predict will happen?"
			: kind === "explanation"
				? "Explain the relevant memory model in your own words."
				: "What did you learn, and what should we try next?";
		const response = await ctx.ui.input(prompt, "");
		if (!response?.trim()) {
			ctx.ui.notify("No response recorded.", "info");
			return;
		}
		const text = response.trim();
		applyCommand(kind === "prediction"
			? { type: "submit_prediction", text }
			: kind === "explanation"
				? { type: "submit_explanation", text }
				: { type: "record_transfer", text });
		pi.appendEntry("learning-machine-response", {
			checkpoint: loadCurrentCheckpoint().checkpoint,
			kind,
			text,
			createdAt: new Date().toISOString(),
		});
		ctx.ui.notify(`${kind[0].toUpperCase()}${kind.slice(1)} recorded.`, "success");
	};

	pi.registerCommand("predict", {
		description: "Record a prediction before an experiment",
		handler: async (_args, ctx) => recordResponse("prediction", ctx),
	});

	pi.registerCommand("explain", {
		description: "Record an explanation of the current concept",
		handler: async (_args, ctx) => recordResponse("explanation", ctx),
	});

	pi.registerCommand("reflect", {
		description: "Record a lesson reflection",
		handler: async (_args, ctx) => recordResponse("reflection", ctx),
	});
}
