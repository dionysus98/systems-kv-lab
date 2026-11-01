# Start your own learning journey

The repository may contain another learner's code and history. Create a clean
workspace instead of deleting or resetting those files. From the repository
root, with Python 3.9 or later installed:

```sh
python3 scripts/start-learning.py ../my-systems-kv-lab
cd ../my-systems-kv-lab
```

The destination must not exist and must be outside the source repository.
The command copies teaching rules, exercise specifications, reusable editor
tools, and starter baselines. It creates blank learner records, restores the
introductory source exercises, and starts at Lesson 1. It excludes previous
answers, progress, comparison evidence, binaries, caches, and Git history.
It requires no network access or Python packages.

Open the new directory in your coding agent and say:

> I'm starting my own learning journey. Read AGENTS.md and the tutor skill.
> My programming background is [background], my systems experience is
> [experience], and my goal is [goal]. Establish my first checkpoint and teach
> me one meaningful step at a time. I will write the implementation.

The tutor records your background in `learner-profile.md`. You can also edit
that file yourself. No inherited starter code counts as your demonstrated
understanding. If you already know the introductory material, ask to establish
a later entry point through a bounded task and explanation.

For the first lesson, the tutor checks for a C compiler. Rust and Zig can be
set up when their checkpoints begin. The starter assumes Linux; ask the tutor
to help with your environment if it differs. Toolchain versions can affect
commands and APIs, especially Zig, so the tutor checks them before building.

## Keeping your work

The exported directory has no Git repository or remote. If you want local
checkpoints, initialize one in that directory:

```sh
git init
git add .
git commit -m "Start my systems programming journey"
```

Configure Git identity if Git requests it. Add your own remote only when you
want to publish your work; the tutor must not use the original learner's remote.
Without Git, lessons continue normally and the tutor explains that commits are
unavailable. Publication remains subject to your authorization.

## How progress works

- `learner-profile.md`: your background, goals, and preferences.
- `learning-checkpoints.md`: the current stage, evidence, and next action.
- `session-doc.md`: detailed attempts, explanations, failures, and results.
- `lessons/`: shared exercise requirements and your implementation work.
- `starter/`: clean baselines used only when creating a new workspace.

The tutor maintains the progress records. Passing checks establishes behavior;
explanation and application establish understanding. Lessons revisit concepts
as the store grows, with computer science introduced to support real decisions.
Resetting the journal alone does not reset progress. To start again without
losing work, export another fresh directory.

## Visual progress

Run `python3 scripts/progress.py --serve` and open `http://127.0.0.1:8765`.
Refresh after the tutor updates your checkpoint record. The page distinguishes
pending work, tutor explanations, tutor-verified checks, and learner demonstrations;
it shows the next learner action and lets you filter checkpoints by language.
Use Ctrl+C in the terminal to stop it, or choose another port with `--port 8766`.

For an offline page without a running server, use `python3 scripts/progress.py`
and open `build/progress.html`. Rerun the command whenever you want a fresh
snapshot. Neither mode changes your evidence or calculates a mastery score.

## Scope of the starter

Introductory exercises and the C fixed-store specification are included today.
Later Rust, Zig, and persistence exercises are developed by the tutor at their
checkpoints. The roadmap describes learning goals, not a complete prewritten
course. Progression rules are instructions followed by the agent, not an
automatic grading system.
