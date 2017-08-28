(ns io.github.bjconlan.ctm.bin-packing
  "This namespace implements greedy bin-packing algorithm(s) (actually in
  this case only one, the best-fit decreasing algorithm which I thought was
  best suited for seeding the 'local search' algorithm.")

(defn best-fit-decreasing
  "This is a very simple implementation of the 'best fit' bin packing algorithm
   using the 'offline' set with 'decreasing' (sort order optimization)

   It takes a collection of item weights/values as integers and a collection of
   bin capacities as integers, returning a collection of vectors (index
   representative of the provided capacities input) for each bin and the
   item weight/value indexes for the provided weights.

   NOTE If any items are unable to fit in the bins using the best fit algorithm
        they will be omitted from the the resulting collection of item index
        vectors"
  [weights capacities]
  {:pre [(coll? weights)
         (every? integer? weights)
         (coll? capacities)
         (every? integer? capacities)]}
  (->> (reduce (fn [bins item]
                 (if-let [bin (or (first (filter #(= (:weight item) (:capacity %)) bins)) ;best fit
                                  (first (filter #(< (:weight item) (:capacity %)) bins)))] ;first fit
                   (assoc bins (:index bin) (assoc bin :capacity (- (:capacity bin) (:weight item))
                                                       :items (conj (:items bin) (:index item))))
                   bins))
               (into [] (map-indexed (fn [index capacity] {:index index :capacity capacity :items []}) capacities))
               (->> (map-indexed (fn [index weight] {:index index :weight weight}) weights)
                    (sort-by :weight >)))
       (map :items)))
