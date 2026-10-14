(ns systems-memory
  (:require [joyride.core :as joyride]
            [systems-kv.memory :as memory] :reload))

(when (= (joyride/invoked-script) joyride/*file*)
  (memory/install!))
