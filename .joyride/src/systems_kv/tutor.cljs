(ns systems-kv.tutor
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [clojure.string :as str]))

(def phases [:orient :predict :attempt :observe :explain :transfer :complete])
(def phase-labels {:orient "Orient" :predict "Predict" :attempt "Attempt"
                   :observe "Observe" :explain "Explain" :transfer "Transfer"
                   :complete "Complete"})
(defonce !state (atom {:phase :orient
                       :prediction ""
                       :events []
                       :panel nil
                       :disposables []}))

(declare render!)

(defn escape-html [value]
  (str/escape (str value) {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&#39;"}))

(defn workspace-root []
  (some-> vscode/workspace.workspaceFolders first .-uri .-fsPath))

(defn record! [kind text]
  (swap! !state update :events conj {:kind kind :text text})
  (render!))

(defn ^:async open-file! [relative-path line]
  (let [root (workspace-root)]
    (when-not root (throw (js/Error. "Open the project workspace first.")))
    (let [file (path/join root relative-path)
          doc (await (vscode/workspace.openTextDocument file))
          editor (await (vscode/window.showTextDocument doc #js {:viewColumn vscode/ViewColumn.One
                                                                  :preserveFocus false}))
          line-index (max 0 (min line (dec (.-lineCount doc))))
          selection (vscode/Selection. line-index 0 line-index 0)]
      (set! (.-selection editor) selection)
      (.revealRange editor selection vscode/TextEditorRevealType.InCenter)
      true)))

(defn run-build! []
  (let [terminal (vscode/window.createTerminal "Learning Machine · C build")]
    (.show terminal)
    (.sendText terminal "mkdir -p build && cc -std=c17 -Wall -Wextra -Wpedantic -g lessons/03-fixed-store/c/main.c -o build/store-c && ./build/store-c" true)
    (record! "BUILD" "Build command launched in the learner-visible terminal.")))

(def phase-names (clj->js (map name phases)))
(defn phase-index [phase] (.indexOf phase-names (name phase)))

(defn button [action label]
  (str "<button data-action='" action "'>" label "</button>"))

(defn render-html [{:keys [phase prediction events]}]
  (let [current (phase-index phase)]
    (str "<!doctype html><html><head><meta charset='UTF-8'>"
         "<meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-systems-tutor';\">"
         "<style>"
         ":root{color-scheme:dark}body{font-family:var(--vscode-font-family);color:var(--vscode-foreground);background:var(--vscode-editor-background);padding:24px;line-height:1.5;max-width:1100px;margin:auto}"
         "h1{font-size:26px;margin:0}.subtitle{color:var(--vscode-descriptionForeground)}.grid{display:grid;grid-template-columns:1.4fr .8fr;gap:18px;margin-top:18px}"
         ".card{border:1px solid var(--vscode-panel-border);border-radius:8px;padding:18px;background:var(--vscode-sideBar-background)}"
         ".focus{border-left:4px solid var(--vscode-textLink-foreground);background:var(--vscode-textBlockQuote-background)}"
         ".phases{display:flex;gap:6px;flex-wrap:wrap;margin:18px 0}.phase{padding:7px 10px;border-radius:5px;background:var(--vscode-button-secondaryBackground);color:var(--vscode-descriptionForeground)}"
         ".phase.done{color:var(--vscode-testing-iconPassed);border-bottom:2px solid var(--vscode-testing-iconPassed)}.phase.current{background:var(--vscode-button-background);color:var(--vscode-button-foreground);font-weight:bold}"
         "button{border:0;border-radius:4px;padding:8px 12px;margin:4px 4px 4px 0;background:var(--vscode-button-background);color:var(--vscode-button-foreground);cursor:pointer}button:hover{background:var(--vscode-button-hoverBackground)}"
         "textarea{width:100%;min-height:90px;box-sizing:border-box;background:var(--vscode-input-background);color:var(--vscode-input-foreground);border:1px solid var(--vscode-input-border);padding:10px;resize:vertical}"
         ".event{border-left:3px solid var(--vscode-textLink-foreground);padding:7px 10px;margin:8px 0;background:var(--vscode-editor-background)}.muted{color:var(--vscode-descriptionForeground)}"
         "@media(max-width:700px){.grid{grid-template-columns:1fr}}"
         "</style></head><body>"
         "<h1>⚡ Tutor Studio</h1><p class='subtitle'>C fixed-capacity store · an interactive lesson workspace</p>"
         "<div class='phases'>"
         (str/join "" (map-indexed (fn [index phase]
                                     (str "<span class='phase " (cond (< index current) "done" (= index current) "current" :else "") "'>"
                                          (if (< index current) "✓ " (if (= index current) "→ " "○ "))
                                          (phase-labels phase) "</span>")) phases))
         "</div>"
         "<div class='card focus'><h2>Current mission</h2>"
         "<p><strong>Goal:</strong> modify caller-owned storage through a <code>Store*</code>.</p>"
         "<p><strong>Question:</strong> after inserting key 10, which fields should change—and which should stay unchanged?</p>"
         (button "open-spec" "Read specification")
         (button "open-implementation" "Open your implementation")
         "</div>"
         "<div class='grid'><section class='card'><h2>Your move</h2>"
         (case phase
           :orient (str "<p>Get oriented, then choose Predict when you are ready.</p>" (button "next-predict" "I understand the goal → Predict"))
           :predict (str "<p>Make a prediction before running code. This is your model, not a test score.</p>"
                         "<details><summary>What is a prediction?</summary>"
                         "<p>Describe what you expect to observe after the next action, and why. You do not need exact bytes or syntax yet.</p>"
                         "<p class='muted'>Sentence starter: <em>I expect ___ to change because ___.</em></p></details>"
                         (button "predict-help" "I need a hint")
                         "<textarea id='prediction' placeholder='I predict that ...'>" (escape-html prediction) "</textarea><br>"
                         (button "record-prediction" "Record prediction → Attempt"))
           :attempt (str "<p>Write the implementation yourself. The tutor will not replace your attempt.</p>"
                         (button "open-implementation" "Edit main.c") (button "run-build" "Run compiler")
                         (button "next-observe" "I have an observation → Observe"))
           :observe (str "<p>Interpret what the compiler, tests, or debugger showed you.</p>"
                         (button "run-build" "Run again") (button "next-explain" "Explain what changed →"))
           :explain (str "<p>Explain the relevant memory behavior in your own words.</p>"
                         "<textarea id='explanation' placeholder='A Store* permits ...'></textarea><br>"
                         (button "record-explanation" "Record explanation → Transfer"))
           :transfer (str "<p>Apply the idea in a small variation before advancing.</p>"
                          (button "open-implementation" "Return to code") (button "finish" "Mark variation attempted"))
           :complete (str "<p><strong>Checkpoint complete.</strong> You demonstrated behavior, explanation, and transfer.</p>"
                          (button "open-spec" "Review the specification")))
         "</section><aside class='card'><h2>Evidence timeline</h2>"
         (if (seq events)
           (str/join "" (map #(str "<div class='event'><strong>" (escape-html (:kind %)) "</strong><br>"
                                      (escape-html (:text %)) "</div>") events))
           "<p class='muted'>Your predictions, observations, and explanations will appear here.</p>")
         "</aside></div>"
         "<p class='muted'>Every action is learner-visible. The tutor guides; you operate the editor, compiler, and debugger.</p>"
         "<script nonce='systems-tutor'>const api=acquireVsCodeApi();document.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>{const p=document.getElementById('prediction');const e=document.getElementById('explanation');api.postMessage({action:b.dataset.action,prediction:p?p.value:'',explanation:e?e.value:''})}));</script>"
         "</body></html>")))

(defn render! []
  (when-let [panel (:panel @!state)]
    (set! (.-html (.-webview panel)) (render-html @!state))))

(defn next-phase! [phase]
  (swap! !state assoc :phase phase)
  (record! "PHASE" (str "Moved to " (phase-labels phase) ".")))

(defn handle-message! [message]
  (let [action (.-action message)]
    (case action
      "open-spec" (-> (open-file! "lessons/03-fixed-store/README.md" 0)
                       (.catch #(vscode/window.showErrorMessage (.-message %))))
      "open-implementation" (-> (open-file! "lessons/03-fixed-store/c/main.c" 0)
                                 (.catch #(vscode/window.showErrorMessage (.-message %))))
      "next-predict" (next-phase! :predict)
      "predict-help" (record! "HINT" "A prediction is an expectation about the next observation, plus your reason. It can be uncertain.")
      "next-observe" (next-phase! :observe)
      "next-explain" (next-phase! :explain)
      "record-prediction" (let [text (.-prediction message)]
                             (if (str/blank? text)
                               (vscode/window.showWarningMessage "Write a prediction before continuing.")
                               (do (swap! !state assoc :prediction text)
                                   (record! "PREDICTION" text)
                                   (next-phase! :attempt))))
      "record-explanation" (let [text (.-explanation message)]
                              (if (str/blank? text)
                                (vscode/window.showWarningMessage "Write an explanation before continuing.")
                                (do (record! "EXPLANATION" text)
                                    (next-phase! :transfer))))
      "run-build" (run-build!)
      "finish" (do (record! "TRANSFER" "Learner marked the variation attempted.")
                    (next-phase! :complete))
      nil)))

(defn dispose! []
  (doseq [disposable (:disposables @!state)] (.dispose disposable))
  (when-let [panel (:panel @!state)] (.dispose panel))
  (reset! !state {:phase :orient :prediction "" :events [] :panel nil :disposables []})
  :disposed)

(defn install! []
  (dispose!)
  (let [panel (vscode/window.createWebviewPanel "systems-kv-tutor" "Tutor Studio"
                                                vscode/ViewColumn.Beside
                                                #js {:enableScripts true :retainContextWhenHidden true})
        listener (.onDidReceiveMessage (.-webview panel) handle-message!)
        close-listener (.onDidDispose panel dispose!)]
    (swap! !state assoc :panel panel :disposables [listener close-listener])
    (render!)
    :installed))
