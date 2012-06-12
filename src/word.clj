(in-ns 'word)
(clojure.core/refer 'clojure.core)
(def token-regex #"\w+")
(def stop-words #{"a" "in" "that" "for" "was" "is" "it" "the" "of" "and" "to" "he"})

(defn to-lower-case [str]
  (.toLowerCase str))

(defn tokenize-str 
  ([input-string]
    (map to-lower-case (re-seq token-regex input-string)))
  ([input-string stop-word?]
    (filter (complement stop-word?) (tokenize-str input-string))))   

