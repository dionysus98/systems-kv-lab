import assert from "node:assert/strict";
import { dispatch, initialState } from "../learning-machine/engine.ts";

const state = initialState();
assert.equal(state.phase, "orient");
dispatch(state, { type: "advance" });
assert.equal(state.phase, "predict");

let events = dispatch(state, { type: "advance" });
assert.equal(events[0].type, "TransitionRejected");

events = dispatch(state, { type: "submit_prediction", text: "count becomes one" });
assert.equal(state.phase, "attempt");
assert.deepEqual(events.map((item) => item.type), ["PredictionRecorded", "PhaseAdvanced"]);

dispatch(state, { type: "record_observation", observation: { kind: "build", status: "success" } });
assert.equal(state.phase, "observe");
dispatch(state, { type: "advance" });
dispatch(state, { type: "submit_explanation", text: "Store* permits caller-visible mutation." });
assert.equal(state.phase, "transfer");
assert.equal(state.evidence.explanation.done, 1);

dispatch(state, { type: "record_transfer", text: "I applied the invariant to deletion." });
assert.equal(state.phase, "complete");

console.log("learning engine: 5 assertions passed");
