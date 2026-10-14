(ns systems-kv.memory-test
  (:require [cljs.test :refer [deftest is run-tests]]
            [systems-kv.memory :as memory]))

(deftest expressions-are-object-paths
  (doseq [s ["number" "store.count" "entries[0].value" "ptr->field" "a[3][2]"]]
    (is (memory/valid-expression? s)))
  (doseq [s [nil "" "f()" "x++" "x=2" "a[i]" "a[-1]" "*(ptr)" "x;run()" "x\n"]]
    (is (not (memory/valid-expression? s)))))

(def baseline {:session-id "one" :expression "number" :scope ["fixture.c" "main"]
               :value "42" :address "0x1000" :size 4 :bytes [42 0 0 0]})

(deftest comparisons-use-measured-overlap
  (is (= [0 1] (:changed-offsets (memory/compare-snapshots baseline (assoc baseline :value "1000" :bytes [232 3 0 0])))))
  (is (false? (:value-changed? (memory/compare-snapshots baseline baseline))))
  (is (:address-changed? (memory/compare-snapshots baseline (assoc baseline :address "0x2000"))))
  (is (= [] (:changed-offsets (memory/compare-snapshots baseline (assoc baseline :bytes [42]))))))

(deftest incompatible-captures-are-not-compared
  (doseq [other [nil (assoc baseline :session-id "two") (assoc baseline :expression "other")
                 (assoc baseline :scope ["fixture.c" "different"])]]
    (is (false? (:comparable? (memory/compare-snapshots baseline other))))))

(deftest rendering-escapes-debugger-content
  (is (= "&lt;script&gt;&amp;&quot;&#39;" (memory/escape-html "<script>&\"'")))
  (let [html (memory/render-html {:expression "<bad>" :error "<script>bad</script>" :sessions {}})]
    (is (.includes html "&lt;bad&gt;"))
    (is (.includes html "&lt;script&gt;bad&lt;/script&gt;"))
    (is (not (.includes html "<script>bad</script>")))))

(deftest debugger-integer-parsing
  (is (= 4 (memory/parse-integer "4")))
  (is (= 42 (memory/parse-integer "42 '*'")))
  (is (= 255 (memory/parse-integer "0xff")))
  (is (thrown? js/Error (memory/parse-integer "unavailable"))))

(defn run! [] (run-tests 'systems-kv.memory-test))
