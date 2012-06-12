(ns analytics-clj.views.welcome
  (:require [analytics-clj.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to analytics-clj"]))
