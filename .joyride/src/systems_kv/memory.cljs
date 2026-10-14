(ns systems-kv.memory
  (:require ["vscode" :as vscode]
            [clojure.string :as str]))

(def max-bytes 64)
(defonce !state (atom {:generation 0 :disposables [] :sessions {} :expression "number"}))

(defn valid-expression? [s]
  (boolean (and (string? s) (<= (count s) 256)
                (re-matches #"[A-Za-z_][A-Za-z0-9_]*(?:(?:\.|->)[A-Za-z_][A-Za-z0-9_]*|\[[0-9]+\])*" s))))

(defn escape-html [s]
  (str/escape (str s) {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&#39;"}))

(defn compare-snapshots [before after]
  (let [compatible? (and before after
                         (= (select-keys before [:session-id :expression :scope])
                            (select-keys after [:session-id :expression :scope])))]
    (if-not compatible?
      {:comparable? false :changed-offsets []}
      {:comparable? true
       :value-changed? (not= (:value before) (:value after))
       :size-changed? (not= (:size before) (:size after))
       :address-changed? (not= (:address before) (:address after))
       :changed-offsets (vec (filter #(not= (nth (:bytes before) %) (nth (:bytes after) %))
                                     (range (min (count (:bytes before)) (count (:bytes after))))))})))

(defn hex-byte [n] (.padStart (.toString n 16) 2 "0"))

(defn byte-html [bytes changed]
  (str/join "" (map-indexed
                (fn [i b] (str "<span class='byte " (when (contains? changed i) "changed")
                                "' title='Offset " i "'>" (hex-byte b) "</span>")) bytes)))

(defn snapshot-html [label snapshot changed]
  (str "<section><h2>" label "</h2>"
       (if-not snapshot
         "<p class='muted'>No capture yet.</p>"
         (str "<dl><dt>Expression / type</dt><dd><code>" (escape-html (:expression snapshot))
              "</code> / " (escape-html (:type snapshot)) "</dd>"
              "<dt>Value</dt><dd>" (escape-html (:value snapshot)) "</dd>"
              "<dt>Address</dt><dd><code>" (escape-html (:address snapshot)) "</code></dd>"
              "<dt>Object size</dt><dd>" (:size snapshot) " bytes</dd></dl>"
              "<p class='muted'>" (escape-html (:file snapshot)) ":" (:line snapshot)
              " · top frame " (escape-html (:frame-name snapshot))
              " · stop " (:epoch snapshot) "</p>"
              "<div class='bytes'>" (byte-html (:bytes snapshot) changed) "</div>"
              (when (> (:size snapshot) (count (:bytes snapshot)))
                (str "<p class='notice'>Truncated: first " (count (:bytes snapshot)) " of " (:size snapshot) " bytes.</p>"))))
       "</section>"))

(defn render-html [{:keys [before after expression error busy? sessions]}]
  (let [{:keys [comparable? changed-offsets value-changed? size-changed? address-changed?]}
        (compare-snapshots before after)
        changed (set changed-offsets)
        statuses (distinct (keep #(when % (get-in sessions [(:session-id %) :status] :unknown)) [before after]))]
    (str "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
         "<meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-systems-memory';\">"
         "<meta name='viewport' content='width=device-width, initial-scale=1'>"
         "<style>body{font-family:var(--vscode-font-family);color:var(--vscode-foreground);background:var(--vscode-editor-background);padding:20px;line-height:1.5;max-width:1050px;margin:auto}"
         "h1{font-size:22px}h2{font-size:17px}input{background:var(--vscode-input-background);color:var(--vscode-input-foreground);border:1px solid var(--vscode-input-border);padding:8px;width:240px}"
         "button{background:var(--vscode-button-background);color:var(--vscode-button-foreground);border:0;padding:8px 12px;margin:4px;cursor:pointer}button:disabled{opacity:.5;cursor:default}"
         ".controls{display:flex;flex-wrap:wrap;align-items:center;gap:6px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(270px,1fr));gap:18px}"
         "section{border:1px solid var(--vscode-panel-border);border-radius:5px;padding:16px;overflow-wrap:anywhere}dt{color:var(--vscode-descriptionForeground);margin-top:8px}dd{margin:0;font-family:var(--vscode-editor-font-family)}"
         ".muted{color:var(--vscode-descriptionForeground);font-size:12px}.notice{border-left:3px solid var(--vscode-editorWarning-foreground);padding-left:10px}.error{color:var(--vscode-errorForeground)}"
         ".bytes{display:flex;gap:5px;flex-wrap:wrap;margin:14px 0}.byte{font-family:var(--vscode-editor-font-family);padding:5px 8px;border:1px solid var(--vscode-panel-border)}"
         ".changed{outline:2px solid var(--vscode-editorWarning-foreground);font-weight:bold}</style></head><body>"
         "<h1>Memory snapshots <small>· C / GDB</small></h1>"
         "<p>Pause your program. Capture Before, step in the debugger, then capture After.</p>"
         "<div class='controls'><label for='expression'>Object</label><input id='expression' value='" (escape-html expression) "' spellcheck='false'>"
         (str/join "" (for [[action title] [["before" "Capture Before"] ["after" "Capture After"] ["clear" "Clear"] ["close" "Close"]]]
                        (str "<button data-action='" action "'" (when busy? " disabled") ">" title "</button>"))) "</div>"
         "<p class='muted'>Identifiers, fields, and constant indices only. Maximum 64 bytes. Captures use the stopped thread's top frame.</p>"
         (when busy? "<p>Reading debugger state…</p>")
         (when error (str "<p class='error' role='alert'>" (escape-html error) "</p>"))
         (when (seq statuses)
           (str "<p class='notice'>Recorded observations. Session state: "
                (escape-html (str/join ", " (map name statuses)))
                ". Captures describe their recorded stop, not the current live state.</p>"))
         (when (and before after)
           (if comparable?
             (str "<p>Value " (if value-changed? "changed" "unchanged")
                  " · size " (if size-changed? "changed" "unchanged")
                  " · address " (if address-changed? "changed" "unchanged")
                  " · changed byte offsets: " (if (seq changed-offsets) (str/join ", " changed-offsets) "none in the captured overlap") "</p>"
                  (when address-changed? "<p class='notice'>Different addresses: this does not establish the same storage object.</p>"))
             "<p class='notice'>Different expressions, sessions, or scopes: byte comparison disabled.</p>"))
         "<div class='grid'>" (snapshot-html "Before" before changed) (snapshot-html "After" after changed) "</div>"
         "<div class='controls'><button data-action='source-before'>Before source</button><button data-action='source-after'>After source</button></div>"
         "<p class='muted'>Bytes are measured in increasing address order. No assumed byte order or conceptual memory is substituted. Snapshots do not prove ownership or lifetime safety.</p>"
         "<script nonce='systems-memory'>const api=acquireVsCodeApi();document.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>api.postMessage({action:b.dataset.action,expression:document.getElementById('expression').value})));</script>"
         "</body></html>")))

(defn render! []
  (when-let [panel (:panel @!state)]
    (set! (.-html (.-webview panel)) (render-html @!state))))

(defn dispose! []
  (let [{:keys [panel disposables generation]} @!state]
    (reset! !state {:generation (inc generation) :disposables [] :sessions {} :expression "number"})
    (doseq [d disposables] (.dispose d))
    (when panel (.dispose panel))
    :disposed))

(defn session-event! [id status thread-id]
  (swap! !state update-in [:sessions id]
         (fn [s] (cond-> (assoc s :status status :epoch (inc (or (:epoch s) 0)))
                   thread-id (assoc :thread-id thread-id))))
  (render!))

(defn ^:async request! [session command arguments]
  (let [timer (atom nil)]
    (try
      (await (js/Promise.race
              #js [(.customRequest session command (clj->js arguments))
                   (js/Promise. (fn [_ reject]
                                  (reset! timer (js/setTimeout #(reject (js/Error. (str command " timed out"))) 5000))))]))
      (finally (when @timer (js/clearTimeout @timer))))))

(defn parse-integer [s]
  (let [m (re-find #"^(?:0x[0-9a-fA-F]+|[0-9]+)(?:\s|$)" (str/trim s))]
    (when-not m (throw (js/Error. (str "Unexpected debugger integer: " s))))
    (let [n (js/Number (str/trim m))]
      (when-not (js/Number.isSafeInteger n) (throw (js/Error. "Debugger integer exceeds supported range")))
      n)))

(defn ^:async evaluate! [session frame-id expression]
  (let [response (await (request! session "evaluate" {:frameId frame-id :expression expression :context "watch"}))]
    (when (re-find #"^(-var-create|<optimized out>|<unavailable>)" (or (.-result response) ""))
      (throw (js/Error. (str "Cannot inspect " expression ": " (.-result response)))))
    response))

(defn ^:async top-frame! [session]
  (let [id (.-id session)
        status (get-in @!state [:sessions id :status])]
    (when (contains? #{:running :ended} status)
      (throw (js/Error. "Pause the debugger before capturing.")))
    (let [threads (await (request! session "threads" {}))
          thread-id (or (get-in @!state [:sessions id :thread-id]) (some-> threads .-threads first .-id))
          stack (await (request! session "stackTrace" {:threadId thread-id :startFrame 0 :levels 1}))
          frame (first (.-stackFrames stack))]
      (when-not frame (throw (js/Error. "No paused stack frame is available.")))
      (when-not (re-find #"\.(c|h)$" (or (some-> frame .-source .-path) ""))
        (throw (js/Error. "v1 requires a C source frame (.c or .h).")))
      (when-not status (session-event! id :paused thread-id))
      frame)))

(defn ^:async read-snapshot! [expression]
  (when-not (valid-expression? expression)
    (throw (js/Error. "Use a simple object expression: number, store.count, ptr->field, or entries[0].")))
  (let [session vscode/debug.activeDebugSession]
    (when-not session (throw (js/Error. "Start a C debug session and pause before capturing.")))
    (when-not (= "cppdbg" (.-type session))
      (throw (js/Error. "v1 supports the C/C++ GDB adapter (cppdbg).")))
    (when-not (get-in @!state [:sessions (.-id session) :tracked?])
      (throw (js/Error. "Open the snapshot tool before starting this debug session; relaunch the debugger to enable reliable stop tracking.")))
    (let [frame (await (top-frame! session))
          id (.-id session)
          epoch (get-in @!state [:sessions id :epoch])
          frame-id (.-id frame)
          value (await (evaluate! session frame-id expression))
          size (parse-integer (.-result (await (evaluate! session frame-id (str "sizeof(" expression ")")))))
          pointer (.-result (await (evaluate! session frame-id (str "(void *)&(" expression ")"))))
          address (re-find #"0x[0-9a-fA-F]+" pointer)]
      (when-not address (throw (js/Error. (str "Cannot read object address: " pointer))))
      (let [bytes (loop [i 0 result []]
                    (if (= i (min size max-bytes)) result
                      (let [b (parse-integer (.-result (await (evaluate! session frame-id
                                              (str "(unsigned int)((unsigned char *)&(" expression "))[" i "]")))))]
                        (when-not (<= 0 b 255) (throw (js/Error. "Invalid byte returned by debugger.")))
                        (recur (inc i) (conj result b)))))]
        (when-not (and (= id (some-> vscode/debug.activeDebugSession .-id))
                       (= epoch (get-in @!state [:sessions id :epoch]))
                       (= :paused (get-in @!state [:sessions id :status])))
          (throw (js/Error. "Debugger moved during capture; pause and retry.")))
        {:session-id id :expression expression :value (.-result value) :type (or (.-type value) "unreported")
         :size size :address address :bytes bytes :epoch epoch
         :file (-> frame .-source .-path) :line (.-line frame) :frame-name (.-name frame)
         :scope [(-> frame .-source .-path) (.-name frame)]}))))

(defn ^:async capture! [slot expression]
  (when-not (contains? #{:before :after} slot) (throw (js/Error. "Unknown capture slot")))
  (when (:busy? @!state) (throw (js/Error. "A capture is already in progress.")))
  (let [generation (:generation @!state)]
    (swap! !state assoc :busy? true :error nil :expression expression)
    (render!)
    (try
      (let [snapshot (await (read-snapshot! expression))]
        (when (= generation (:generation @!state)) (swap! !state assoc slot snapshot))
        snapshot)
      (catch js/Error e
        (when (= generation (:generation @!state)) (swap! !state assoc :error (.-message e)))
        nil)
      (finally
        (when (= generation (:generation @!state)) (swap! !state assoc :busy? false) (render!))))))

(defn ^:async reveal-source! [slot]
  (when-let [{:keys [file line]} (get @!state slot)]
    (let [uri (vscode/Uri.file file)
          folder (vscode/workspace.getWorkspaceFolder uri)]
      (when-not folder (throw (js/Error. "Source is outside the current workspace.")))
      (let [doc (await (vscode/workspace.openTextDocument uri))
            editor (await (vscode/window.showTextDocument doc #js {:viewColumn vscode/ViewColumn.One :preserveFocus false}))
            line-index (max 0 (min (dec line) (dec (.-lineCount doc))))
            selection (vscode/Selection. line-index 0 line-index 0)]
        (set! (.-selection editor) selection)
        (.revealRange editor selection vscode/TextEditorRevealType.InCenter)
        true))))

(defn clear! []
  (swap! !state dissoc :before :after :error)
  (render!))

(defn handle-message! [message]
  (let [action (.-action message) expression (.-expression message)]
    (case action
      "before" (capture! :before expression)
      "after" (capture! :after expression)
      "clear" (clear!)
      "close" (dispose!)
      "source-before" (-> (reveal-source! :before) (.catch #(vscode/window.showErrorMessage (.-message %))))
      "source-after" (-> (reveal-source! :after) (.catch #(vscode/window.showErrorMessage (.-message %))))
      nil)))

(defn install! []
  (dispose!)
  (let [generation (:generation @!state)
        tracked-event! (fn [id status thread-id]
                         (when (= generation (:generation @!state))
                           (session-event! id status thread-id)))
        panel (vscode/window.createWebviewPanel "systems-kv-memory" "Memory snapshots"
                                               #js {:viewColumn vscode/ViewColumn.Beside :preserveFocus true}
                                               #js {:enableScripts true :retainContextWhenHidden true})
        tracker (vscode/debug.registerDebugAdapterTrackerFactory
                 "cppdbg"
                 #js {:createDebugAdapterTracker
                      (fn [session]
                        (when (= generation (:generation @!state))
                          (swap! !state assoc-in [:sessions (.-id session)] {:tracked? true :epoch 0 :status :starting}))
                        #js {:onWillReceiveMessage
                             (fn [message]
                               (when (contains? #{"continue" "next" "stepIn" "stepOut" "restart" "disconnect" "terminate"} (.-command message))
                                 (tracked-event! (.-id session) :running nil)))
                             :onDidSendMessage
                             (fn [message]
                               (when (= "event" (.-type message))
                                 (case (.-event message)
                                   "stopped" (tracked-event! (.-id session) :paused (some-> message .-body .-threadId))
                                   "continued" (tracked-event! (.-id session) :running nil)
                                   "terminated" (tracked-event! (.-id session) :ended nil)
                                   nil)))})})
        terminated (vscode/debug.onDidTerminateDebugSession #(tracked-event! (.-id %) :ended nil))
        listener (.onDidReceiveMessage (.-webview panel) handle-message!)
        close-listener (.onDidDispose panel #(when (= panel (:panel @!state)) (dispose!)))
        clear-command (vscode/commands.registerCommand "systems-kv.memory.clear" clear!)
        close-command (vscode/commands.registerCommand "systems-kv.memory.close" dispose!)]
    (swap! !state assoc :panel panel :disposables [tracker terminated listener close-listener clear-command close-command])
    (render!)
    :installed))
