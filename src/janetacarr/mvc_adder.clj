(ns janetacarr.mvc-adder
  (:require [seesaw.core :as s]
            [clojure.string :as string])
  (:gen-class))

;; Higher-order functions as our "strategy delegates".
(defn change-display
  "Takes an atom `model` and a string `input`, and updates
  `model` by appending `input` to the currently displayed
  text in our calculator."
  [model input]
  (swap! model update :display #(apply str (concat % input))))

(defn solve
  [model]
  (swap! model update :display #(apply + (mapv (fn [s]
                                                 (Integer/parseInt s))
                                               (string/split % #"\+")))))

;; A very simple view hierarchy using a few seesaw components.
(defn create-view-children
  [display state change-fn solve-fn]
  (let [display-label (s/label :text display)
        plus-button (s/button :text "+"
                              :listen [:action (fn [e]
                                                 (change-fn state "+"))])
        equal-button (s/button :text "="
                               :listen [:action (fn [e]
                                                  (solve-fn state))])
        keypad-buttons (mapv (fn [n]
                               (s/button :text (str n)
                                         :listen [:action (fn [e]
                                                            (change-display state (str n)))]))
                             (range 9))
        keypad (s/grid-panel :rows 3
                             :columns 3
                             :items keypad-buttons)
        panel (s/grid-panel :rows 1
                            :columns 2
                            :items [plus-button equal-button])]
    (s/border-panel
     :items [[panel :south] [display-label :north] [keypad :center]])))

(defn create-view
  [children]
  (s/frame :title "Positive Integer Adder"
           :visible? true
           :content children
           :on-close :exit
           :width 400
           :height 300))

;; We can add many "observers" to change other view state/composition
;; or even change the strategy used by the buttons to add the numbers now
;; that we've decoupled all three.
(defn update-view
  [view state]
  (fn [_ _ _ new-state]
    (let [display (:display new-state)]
      (s/config! view :content (create-view-children display
                                                     state
                                                     change-display
                                                     solve)))))

(defn state-logger
  [_ _ _ new-state]
  (println "State changed to: " new-state))

(defn -main
  [& args]
  (let [;; Our model, diligently keeping track of our state
        state (atom {:display ""})

        ;; The root of our view hierachy
        view (create-view (create-view-children 0 state change-display solve))]

    ;; Observe
    (add-watch state :update-view (update-view view state))
    (add-watch state :logger state-logger)

    ;; Start the app
    (s/native!)
    (s/invoke-now (fn [] (s/show! (s/pack! view))))))
