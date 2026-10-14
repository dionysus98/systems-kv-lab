(ns systems-kv.integration-test
  "Explicitly invoked tooling test. Debugs only the compiled isolated fixture."
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [systems-kv.memory :as memory]))

(defonce !result (atom {:status :not-run}))

(defn check! [condition message]
  (when-not condition (throw (js/Error. message))))

(defn ^:async wait-for! [predicate]
  (let [deadline (+ (.now js/Date) 15000)]
    (loop []
      (cond (predicate) true
            (> (.now js/Date) deadline) (throw (js/Error. "Timed out waiting for debugger state"))
            :else (do (await (js/Promise. #(js/setTimeout % 50))) (recur))))))

(defn ^:async run! []
  (check! (nil? vscode/debug.activeDebugSession) "End your existing debug session before running the tooling test.")
  (let [folder (first vscode/workspace.workspaceFolders)
        root (-> folder .-uri .-fsPath)
        source (path/join root ".joyride/test/fixtures/memory.c")
        program (path/join root "build/joyride-memory-fixture")
        breakpoint (vscode/SourceBreakpoint. (vscode/Location. (vscode/Uri.file source) (vscode/Position. 5 0)))
        owned-session (atom nil)]
    (reset! !result {:status :running})
    (try
      (memory/install!)
      (memory/install!)
      (await (memory/capture! :before "number"))
      (check! (.includes (or (:error @memory/!state) "") "Start a C debug session") "No-debugger error missing")
      (vscode/debug.addBreakpoints #js [breakpoint])
      (check! (await (vscode/debug.startDebugging folder
                      #js {:name "Joyride tooling verification" :type "cppdbg" :request "launch"
                           :program program :cwd root :MIMode "gdb" :miDebuggerPath "/usr/bin/gdb"
                           :stopAtEntry false :externalConsole false})) "Debugger launch failed")
      (reset! owned-session vscode/debug.activeDebugSession)
      (let [session @owned-session id (.-id session)]
        (await (wait-for! #(= :paused (get-in @memory/!state [:sessions id :status]))))
        (let [before (await (memory/capture! :before "number"))
              thread (get-in @memory/!state [:sessions id :thread-id])
              epoch (get-in @memory/!state [:sessions id :epoch])]
          (check! (= "42" (:value before)) (str "Unexpected initial snapshot: " (:error @memory/!state)))
          (await (.customRequest session "next" #js {:threadId thread}))
          (await (wait-for! #(and (= :paused (get-in @memory/!state [:sessions id :status]))
                                 (> (get-in @memory/!state [:sessions id :epoch]) epoch))))
          (let [after (await (memory/capture! :after "number"))
                comparison (memory/compare-snapshots before after)
                payload (await (memory/read-snapshot! "payload"))]
            (check! (= "1000" (:value after)) "Value did not change to 1000")
            (check! (and (:value-changed? comparison) (not (:address-changed? comparison))
                         (not (:size-changed? comparison)) (seq (:changed-offsets comparison))) "Bad snapshot comparison")
            (check! (and (= 80 (:size payload)) (= 64 (count (:bytes payload)))) "Byte limit was not applied")
            (await (memory/capture! :after "missing_object"))
            (check! (and (:error @memory/!state) (not (:busy? @memory/!state))) "Invalid object did not produce a recoverable error")
            (check! (await (memory/reveal-source! :after)) "Source navigation failed")
            (await (.customRequest session "continue" #js {:threadId thread}))
            (await (memory/capture! :after "number"))
            (check! (.includes (or (:error @memory/!state) "") "Pause the debugger") "Running-state capture was not rejected")
            (await (wait-for! #(= :ended (get-in @memory/!state [:sessions id :status]))))
            (check! (.includes (memory/render-html @memory/!state) "ended") "Ended-session status missing")
            (let [sessions (:sessions @memory/!state)]
              (memory/dispose!)
              (check! (and (nil? (:panel @memory/!state)) (empty? (:disposables @memory/!state))) "Disposal left resources registered")
              (memory/install!)
              ;; Restore real recorded captures for the human-facing demonstration.
              (swap! memory/!state assoc :before before :after after :sessions sessions :error nil)
              (memory/render!))
            (reset! !result {:status :passed :before before :after after :comparison comparison
                            :checks [:reload :no-debug :real-bytes :step :bounded-read :invalid-object
                                     :source-navigation :running-rejected :ended-label :dispose]}))))
      (catch :default error
        (reset! !result {:status :failed :message (.-message error)})
        (when-let [session @owned-session]
          (when (= (.-id session) (some-> vscode/debug.activeDebugSession .-id))
            (await (vscode/debug.stopDebugging session)))))
      (finally (vscode/debug.removeBreakpoints #js [breakpoint])))
    (:status @!result)))
