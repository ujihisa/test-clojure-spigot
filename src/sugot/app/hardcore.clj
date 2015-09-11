(ns sugot.app.hardcore
  (:require [sugot.lib :as l]
            [sugot.block :as b]
            [sugot.world])
  (:import [org.bukkit Bukkit Server WorldCreator Material Location Sound]
           [org.bukkit.block Biome]
           [org.bukkit.entity ArmorStand Monster Blaze Egg SmallFireball Player LivingEntity]
           [org.bukkit.event.block Action]
           [org.bukkit.event.entity CreatureSpawnEvent$SpawnReason]
           [org.bukkit.event.entity EntityDamageEvent$DamageCause]
           [org.bukkit.inventory ItemStack]
           [org.bukkit.enchantments Enchantment]))

(defn PlayerDropItemEvent [event]
  (when (= "Magic Compass"
           (some-> event
                   .getItemDrop .getItemStack .getItemMeta .getDisplayName))
    (.setCancelled event true)
    (l/send-message (.getPlayer event) "[HARDCORE] You should keep it for going back home!")))

(defn- hardcore-world-exist? []
  (.isDirectory (clojure.java.io/as-file "hardcore")))

(defn in-hardcore? [loc]
  (= "hardcore" (.getName (.getWorld loc))))

(defn ProjectileHitEvent [event]
  nil)

(defn BlockPlaceEvent [event]
  nil)

; key: ^String playername, value: ^Long timestamp msec
(def enter-time-all (atom {}))

(defn EntityDamageEvent [event]
  (try
    (let [entity (.getEntity event)
          cause (.getCause event)]
      (when (in-hardcore? (.getLocation entity))
        (cond
          (instance? ArmorStand entity)
          (.setCancelled event true)

          (instance? Monster entity)
          (condp = cause
            EntityDamageEvent$DamageCause/FIRE_TICK
            (.setCancelled event true)
            nil))))
    (catch Exception e (.printStackTrace e))))

(defn EntityDeathEvent [event]
  )

(defn- hardcore-players []
  (seq (filter #(in-hardcore? (.getLocation %))
               (Bukkit/getOnlinePlayers))))

(defn CreatureSpawnEvent [event]
  (let [entity (.getEntity event)
        reason (.getSpawnReason event)
        l (.getLocation event)]
    ; You can't use `case` for Java enum
    (when (and (hardcore-world-exist?)
               (in-hardcore? l)
               (instance? Monster entity)
               (= CreatureSpawnEvent$SpawnReason/NATURAL reason))
      (l/later 0
        (dotimes [_ 2]
          (let [loc (doto (.clone l)
                      (.add (rand-nth [-0.5 0.5]) 0.5 (rand-nth [-0.5 0.5])))
                klass (if (= 0 (rand-int 3))
                        Blaze
                        (class entity))
                monster
                (sugot.world/spawn loc klass)]
            (when-let [players (hardcore-players)]
              (.setTarget monster (rand-nth players)))))))))

(defn- launch-projectile [source projectile velocity]
  (.launchProjectile source projectile velocity))

(defn ProjectileLaunchEvent [event]
  (try
    (let [projectile (.getEntity event)
          shooter (.getShooter projectile)]
      (when (and
              (instance? Blaze shooter)
              (instance? SmallFireball projectile))
        (when-let [target (.getTarget shooter)]
          (.setCancelled event true)
          (let [velocity
                (.normalize (.getDirection
                              (.subtract (.getLocation shooter)
                                         (.getLocation target))))
                #_ egg #_(launch-projectile shooter org.bukkit.entity.Arrow velocity)]
            (.setVelocity shooter velocity)
            #_ (.setBounce egg true)))))
    (catch Exception e (.printStackTrace e))))

#_ (def interesting-seeds
  [#_7352190906321318631 ; http://epicminecraftseeds.com/stronghold-in-ravine-1-8x/
   #_ 3083175 ; http://epicminecraftseeds.com/spawn-beside-jungle-temple/
   516687594611420526 ; http://epicminecraftseeds.com/minecraft-village-seed-great-loot/
   5574457897082764526 ; http://epicminecraftseeds.com/sweet-savanna-m-above-the-clouds-minecraft-1-8-seed/
   1603402340 ; underwater
   ])

(defn random-xz [radius]
  {:pre [(< 0 radius)]}
  (let [x (- (rand-int (* 2 radius)) radius)
        z (Math/round (* (Math/sqrt (- (* radius radius) (* x x)))
                         (rand-nth [1 -1])))]
    [x z]))

(defn- rand-treasure []
  (case (rand-int 34)
    0 (ItemStack. Material/DIRT (inc (rand-int 32)))
    1 (ItemStack. Material/DIRT (inc (rand-int 32)))
    2 (ItemStack. Material/SAND (inc (rand-int 64)))
    3 (ItemStack. Material/STICK (inc (rand-int 64)))
    4 (ItemStack. Material/RAW_FISH (inc (rand-int 10)) (short 0) (byte (rand-int 4)))
    5 (ItemStack. Material/COOKED_FISH (inc (rand-int 10)) (short 0) (byte (rand-int 2)))
    6 (ItemStack. Material/EMERALD (inc (rand-int 4)))
    7 (ItemStack. Material/ANVIL (inc (rand-int 4)))
    8 (ItemStack. Material/ARROW (inc (rand-int 64)))
    9 (ItemStack. Material/BOAT (inc (rand-int 10)))
    10 (ItemStack. Material/BRICK (inc (rand-int 64)))
    11 (ItemStack. Material/COAL (inc (rand-int 64)))
    12 (ItemStack. Material/INK_SACK (inc (rand-int 8)) (short 0) (byte 4)) ; LAPIS
    13 (ItemStack. Material/DIAMOND (inc (rand-int 2)))
    14 (ItemStack. Material/DIAMOND_HOE 1)
    15 (ItemStack. Material/ENDER_CHEST 1)
    16 (ItemStack. Material/ENDER_PEARL 1)
    17 (ItemStack. Material/EXP_BOTTLE 1)
    18 (ItemStack. Material/FLINT_AND_STEEL 1)
    19 (ItemStack. Material/GOLD_INGOT (inc (rand-int 16)))
    20 (ItemStack. Material/BONE (inc (rand-int 16)))
    21 (ItemStack. Material/LOG (inc (rand-int 64)))
    22 (ItemStack. Material/SADDLE 1)
    23 (ItemStack. Material/SLIME_BALL (inc (rand-int 5)))
    24 (ItemStack. Material/STRING (inc (rand-int 5)))
    25 (ItemStack. Material/APPLE (inc (rand-int 5)))
    26 (ItemStack. Material/INK_SACK (inc (rand-int 4)) (short 0) (byte (rand-int 16)))
    27 (ItemStack. Material/INK_SACK (inc (rand-int 4)) (short 0) (byte (rand-int 16)))
    28 (ItemStack. Material/INK_SACK (inc (rand-int 4)) (short 0) (byte (rand-int 16)))
    29 (ItemStack. Material/INK_SACK (inc (rand-int 4)) (short 0) (byte (rand-int 16)))
    30 (ItemStack. Material/IRON_INGOT 1)
    31 (ItemStack. Material/IRON_BARDING 1)
    32 (ItemStack. Material/GOLD_BARDING 1)
    33 (ItemStack. Material/DIAMOND_BARDING 1)
    nil))

(defn- rand-treasures [min-n max-n]
  (for [_ (range (+ min-n (rand-int (inc (- max-n min-n)))))]
    (rand-treasure)))

(defn create-treasure-chest [block]
  (b/set-block block Material/CHEST 0)
  (let [chest (.getBlock (.getLocation block))]
    (doseq [item-stack (rand-treasures 2 8)
            :when item-stack]
      (b/add-chest-inventory chest (into-array [item-stack])))))

(defn- create-main-logic [hardcore-world]
  (.setTime hardcore-world 21000)
    (let [init-y (.getHighestBlockYAt hardcore-world 0 0)]
      (l/later 0
        (doseq [x (range -1 2)
                z (range -1 2)]
          (b/set-block! (.getBlockAt hardcore-world x (dec init-y) z)
                        Material/OBSIDIAN 0)
          (b/set-block! (.getBlockAt hardcore-world x init-y z)
                        Material/TORCH 0))))
    (let [[goal-distance chest-distance]
          (let [init-biome (.getBiome hardcore-world 0 0)]
            (get {#_Biome/OCEAN #_[100 10]
                  Biome/DESERT [350 15]
                  Biome/TAIGA [270 15]
                  Biome/EXTREME_HILLS [90 2]}
                 init-biome
                 [220 6]))

          [goal-x goal-z] (random-xz (int (* goal-distance (rand-nth [0.7 0.8 0.9 1.0 1.1 1.2 1.3 2.0]))))

          goal-y (.getHighestBlockYAt hardcore-world goal-x goal-z)]
      (.setSpawnLocation hardcore-world goal-x (inc goal-y) goal-z)
      (l/later 0
        (b/set-block! (.getBlockAt hardcore-world goal-x (dec goal-y) goal-z)
                      Material/BEDROCK 0)
        (let [x (+ goal-x (rand-nth (remove zero? (range (- chest-distance) (inc chest-distance)))))
              z (+ goal-z (rand-nth (remove zero? (range (- chest-distance) (inc chest-distance)))))
              y (+ (.getHighestBlockYAt hardcore-world x z) (rand-nth [-3 -2 -1 -1 -1 0 10]))]
          (b/set-block! (.getBlockAt hardcore-world x (dec y) z) Material/WOOD 0)
          (b/set-block! (.getBlockAt hardcore-world x y z) Material/AIR 0)
          (create-treasure-chest (.getBlockAt hardcore-world x y z)))
        (-> (sugot.world/spawn (Location. hardcore-world
                                          (+ 0.5 goal-x)
                                          goal-y
                                          (+ 0.5 goal-z))
                               ArmorStand)
          (.setHelmet (ItemStack. Material/OBSIDIAN 1))))))

(declare garbage-collection)

(defn create [num-retry]
  {:pre [(not (hardcore-world-exist?))]}
  (let [world-creator (-> (WorldCreator. "hardcore")
                        (.copy (Bukkit/getWorld "world"))
                        (.seed (rand-int Integer/MAX_VALUE)
                               #_ (first interesting-seeds)))
        hardcore-world (.createWorld world-creator)]
    (if (and
          (< 0 num-retry)
          (contains? #{Biome/OCEAN Biome/DEEP_OCEAN} (.getBiome hardcore-world 0 0)))
      (do
        (l/broadcast "[HARDCORE] (It's an ocean. Retrying...)")
        (garbage-collection)
        (create (dec num-retry)))
      (create-main-logic hardcore-world))))

(defn hardcore-world []
  (Bukkit/getWorld "hardcore"))

(defn enter-hardcore [living-entity]
  {:pre [(hardcore-world)]}
  (let [init-loc
        (Location.
          (hardcore-world) 0.5 (inc (.getHighestBlockYAt (hardcore-world) 0 0)) 0.5)]
    (.teleport living-entity init-loc)
    (when (instance? Player living-entity)
      (l/broadcast-and-post-lingr
        (format "[HARDCORE] %s entered to hardcore world. (seed: \"%d\", biome: %s)"
                (.getName living-entity)
                (.getSeed (hardcore-world))
                (.name (.getBiome (hardcore-world) 0 0))))
      (swap! enter-time-all assoc (.getName living-entity) (System/currentTimeMillis)))))

(defn enter-satisfy? [player]
  (when-let [item-stack (.getItemInHand player)]
    (when (and (= "world" (.getName (.getWorld (.getLocation player))))
               (= Material/PAPER (.getType item-stack))
               (= 1 (.getAmount item-stack)))
      (when-let [armour-stand (some #(when (instance? ArmorStand %) %)
                                    (.getNearbyEntities player 0 0 0))]
        (= Material/PUMPKIN (.getType (.getHelmet armour-stand)))))))

(defn leave-satisfy? [player]
  (when-let [item-stack (.getItemInHand player)]
    (when (and (in-hardcore? (.getLocation player))
               (= Material/COMPASS (.getType item-stack))
               (= 1 (.getAmount item-stack)))
      (when-let [armour-stand (some #(when (instance? ArmorStand %) %)
                                    (.getNearbyEntities player 0 0 0))]
        (= Material/OBSIDIAN (.getType (.getHelmet armour-stand)))))))

(defn dir-delete-recursively
  "From https://gist.github.com/edw/5128978"
  [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (clojure.java.io/delete-file f))]
    (func func (clojure.java.io/file fname))))

(defn garbage-collection []
  (if (and
        (hardcore-world-exist?)
        (empty? (hardcore-players)))
    (if-let [hardcore-world (hardcore-world)]
      (let [folder (.getWorldFolder hardcore-world)]
        (if (Bukkit/unloadWorld "hardcore" false)
          (do
            (try
              (dir-delete-recursively (.getAbsolutePath folder))
              (catch Exception e (.printStackTrace e)))
            :unloadWorld-succeeded)
          :unloadWorld-failed))
      :precondition-failed--weird)
    :precondition-failed--normal))

(defn- format-from-msec [msec]
  (let [sec-total (/ msec 1000)
        min-total (/ sec-total 60)
        hour-total (/ min-total 60)
        seconds (int (rem sec-total 60))
        minutes (int (rem min-total 60))
        hours (int hour-total)]
    (cond
      (and (< 0 hours) (< 0 minutes))
      (format "%d hours %d minutes %d seconds" hours minutes seconds)

      (< 0 minutes)
      (format "%d minutes %d seconds" minutes seconds)

      :else
      (format "%d seconds" seconds))))

(defn leave-hardcore [player]
  {:pre [(in-hardcore? (.getLocation player))]}
  (let [to (some identity [(.getBedSpawnLocation player)
                           (.getSpawnLocation (Bukkit/getWorld "world"))])]
    (l/broadcast-and-post-lingr (format "[HARDCORE] %s left from the hardcore world."
                                        (.getName player)))
    (.teleport player to)
    (try
      (when-let [enter-time (get @enter-time-all (.getName player))]
        (l/broadcast-and-post-lingr
          (format "[HARDCORE] Record: %s"
                  (format-from-msec
                    (- (System/currentTimeMillis) enter-time)))))
      (catch Exception e (.printStackTrace e)))))

(defn PlayerInteractEvent [event]
  (try
    (when-let [player (.getPlayer event)]
    (let [action (.getAction event)]
      (when (contains? #{Action/RIGHT_CLICK_AIR Action/RIGHT_CLICK_BLOCK} action)
        (cond
          (enter-satisfy? player)
          (do
            ; effect
            (let [loc (.getLocation player)]
              (sugot.world/strike-lightning-effect loc)
              (sugot.world/play-sound loc Sound/AMBIENCE_CAVE 1.0 1.0)
              (sugot.world/play-sound loc Sound/AMBIENCE_CAVE 1.0 1.0))
            ; main
            (garbage-collection)
            (let [compass (doto (ItemStack. Material/COMPASS 1)
                            (.addUnsafeEnchantment Enchantment/DURABILITY 1)
                            (l/set-display-name "Magic Compass"))]
              (.setItemInHand player compass)
              (when-not (hardcore-world-exist?)
                (l/broadcast "[HARDCORE] (Creating world...)")
                (create 3))
              ; TODO living entity
              (enter-hardcore player)))

          (leave-satisfy? player)
          (do
            (.setItemInHand player (ItemStack. Material/PAPER 1))
            (leave-hardcore player))))))
    (catch Exception e (.printStackTrace e))))
