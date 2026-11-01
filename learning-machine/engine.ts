/**
 * Interface-independent learning state transitions.
 *
 * This module has no Pi, VS Code, Joyride, terminal, debugger, or model
 * dependencies. Adapters send commands in and render state/events out.
 */

export const PHASES = ["orient", "predict", "attempt", "observe", "explain", "transfer", "complete"] as const;
export type Phase = (typeof PHASES)[number];

export interface EvidenceCount {
	done: number;
	total: number;
}

export interface Observation {
	kind: string;
	status?: string;
	text?: string;
	[key: string]: unknown;
}

export interface LearningState {
	checkpoint: string;
	language: string;
	phase: Phase;
	goal: string;
	nextAction: string;
	prediction: string;
	explanation: string;
	observations: Observation[];
	evidence: {
		behavior: EvidenceCount;
		explanation: EvidenceCount;
		transfer: EvidenceCount;
	};
	helpLevel: number;
}

export type LearningCommand =
	| { type: "advance" }
	| { type: "submit_prediction"; text: string }
	| { type: "record_observation"; observation: Observation }
	| { type: "submit_explanation"; text: string }
	| { type: "record_transfer"; text: string }
	| { type: "request_hint" };

export interface LearningEvent {
	type: string;
	[key: string]: unknown;
}

export function initialState(): LearningState {
	return {
		checkpoint: "03-c-fixed-store",
		language: "C",
		phase: "orient",
		goal: "Modify caller-owned storage through a Store pointer.",
		nextAction: "Read the specification, then make a prediction.",
		prediction: "",
		explanation: "",
		observations: [],
		evidence: {
			behavior: { done: 0, total: 10 },
			explanation: { done: 0, total: 1 },
			transfer: { done: 0, total: 1 },
		},
		helpLevel: 0,
	};
}

function event(type: string, details: Record<string, unknown> = {}): LearningEvent {
	return { type, ...details };
}

function advance(state: LearningState, phase: Phase, nextAction: string): LearningEvent {
	const from = state.phase;
	state.phase = phase;
	state.nextAction = nextAction;
	return event("PhaseAdvanced", { from, to: phase, nextAction });
}

export function dispatch(state: LearningState, command: LearningCommand): LearningEvent[] {
	const events: LearningEvent[] = [];

	switch (command.type) {
		case "advance": {
			const transitions: Partial<Record<Phase, [Phase, string]>> = {
				orient: ["predict", "Record a prediction before running code."],
				attempt: ["observe", "Interpret the compiler, test, or debugger observation."],
				observe: ["explain", "Explain what changed and why."],
			};
			const transition = transitions[state.phase];
			if (transition) events.push(advance(state, ...transition));
			else events.push(event("TransitionRejected", { reason: `cannot_advance_from_${state.phase}` }));
			break;
		}
		case "submit_prediction": {
			const text = command.text.trim();
			if (!text) {
				events.push(event("CommandRejected", { reason: "prediction_required" }));
				break;
			}
			state.prediction = text;
			events.push(event("PredictionRecorded", { text }));
			if (state.phase === "predict") events.push(advance(state, "attempt", "Implement the bounded task, then submit the attempt."));
			break;
		}
		case "record_observation":
			if (!command.observation || !command.observation.kind) {
				events.push(event("CommandRejected", { reason: "observation_required" }));
			} else {
				state.observations.push(command.observation);
				events.push(event("ObservationRecorded", { observation: command.observation }));
				if (state.phase === "attempt") events.push(advance(state, "observe", "Interpret this observation before changing code."));
			}
			break;
		case "submit_explanation": {
			const text = command.text.trim();
			if (!text) {
				events.push(event("CommandRejected", { reason: "explanation_required" }));
				break;
			}
			state.explanation = text;
			state.evidence.explanation.done = 1;
			events.push(event("ExplanationRecorded", { text }));
			if (state.phase === "explain") events.push(advance(state, "transfer", "Apply the memory model in a small variation."));
			break;
		}
		case "record_transfer": {
			const text = command.text.trim();
			if (!text) {
				events.push(event("CommandRejected", { reason: "transfer_evidence_required" }));
				break;
			}
			state.evidence.transfer.done = 1;
			events.push(event("TransferRecorded", { text }));
			events.push(advance(state, "complete", "Choose the next feature with the tutor."));
			break;
		}
		case "request_hint":
			state.helpLevel = Math.min(4, state.helpLevel + 1);
			events.push(event("HintRequested", { level: state.helpLevel }));
			break;
	}

	return events;
}
