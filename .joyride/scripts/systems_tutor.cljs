(ns systems-tutor
  (:require [joyride.core :as joyride]
            [systems-kv.tutor :as tutor] :reload))

(when (= (joyride/invoked-script) joyride/*file*)
  (tutor/install!))
