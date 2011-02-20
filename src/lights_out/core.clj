(ns lights-out.core
  (:use [clojure.contrib.combinatorics :only [cartesian-product]]
        [clojure.contrib.generic.functor :only [fmap]])
  (:import [java.awt Color]
           [javax.swing JPanel JFrame]
           [java.awt.event MouseListener MouseAdapter MouseEvent])
  (:gen-class))

(def light-size 50)

(def matrix [[-1 0] [0 -1] [0 0] [1 0] [0 1]])

(defn create-board [x y]
  (-> (cartesian-product (range x) (range y))
      (zipmap (repeat false))
      (with-meta {:x x :y y})))

(defn random-configuration [board]
  (-> (fn [_] (rand-nth [true false]))
      (fmap board)
      (with-meta (meta board))))

(def board (atom (random-configuration (create-board 5 5))))

(defn toggle-position [board position]
  (update-in board [position] not))

(defn within-bounds? [board position]
  (let [{:keys [x y]} (meta board)]
    (and (>= (dec x) (first position) 0)
         (>= (dec y) (second position) 0))))

(defn toggle [board position]
  (->> (identity matrix)
       (map (partial map + position))
       (filter (partial within-bounds? board))
       (reduce toggle-position board)))

(defn draw-board [graphics board]
  (doseq [[[x y] state] board]
    (let [x (* x light-size) y (* y light-size)]
      (doto graphics
        (.setColor Color/BLACK) ; border
        (.fillRect x y light-size light-size)
        (.setColor (if state Color/RED Color/WHITE)) ; light
        (.fillRect x y (- light-size 2) (- light-size 2))))))

(defn panel []
  (proxy [JPanel] []
    (paintComponent [g]
      (proxy-super paintComponent g)
      (draw-board g @board))))

(defn frame []
  (let [{:keys [x y]} (meta @board)]
    (doto (JFrame. "Lights Out")
      (.setSize (+ 16 (* x light-size)) (+ 38 (* y light-size)))
      (.setVisible true)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE))))

(defn -main [& args]
  (let [frame (frame)
        panel (panel)]
    (.addMouseListener panel
      (proxy [MouseAdapter] []
        (mouseClicked [e]
          (let [x (-> e (.getX) (/ light-size) int)
                y (-> e (.getY) (/ light-size) int)
                position (list x y)]
            (swap! board toggle position)
            (.repaint panel)))))
    (.setFocusable panel true)
    (.add frame panel)))
