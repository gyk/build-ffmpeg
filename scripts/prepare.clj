(ns prepare
  (:require [babashka.fs :as fs]
            [babashka.process :as p :refer [process sh]]
            [clojure.string :as str]))

; FIXME
(def ESSENTIAL_FFMPEG_PATH "C:\\Users\\Dell\\bin\\ffmpeg-5.0-essentials_build\\bin")
(def FFMPEG_SRC_PATH "C:\\Users\\Dell\\Documents\\Code\\CPrj\\FFmpeg")

(defn sh*
  [args opt]
  (let [cmd       (str/join " " args)
        shell-cmd (str/join " " (list "C:\\cygwin64\\bin\\bash.exe" "-c" (format "\"%s\"" cmd)))]
    (sh shell-cmd (merge {:err :inherit} opt))))

(defn list-essential-decoders
  []
  (let [decoder-list       (-> (process ["./ffmpeg.exe" "-decoders"]
                                        {:dir ESSENTIAL_FFMPEG_PATH
                                         :err nil})
                               :out
                               clojure.java.io/reader
                               line-seq)

        essential-decoders (->> decoder-list
                                ; Decoder list starts after a line of " -----"
                                (drop-while (complement #(re-matches #"^\s*-+.*" %)))
                                rest
                                (keep #(re-find #"(?<=.{6}\s+)[^\s]+" %))
                                (apply sorted-set))]
    essential-decoders))

(defn list-default-decoders
  []
  (let [decoder-str (:out (sh* ["./configure" "--list-decoders"] {:dir FFMPEG_SRC_PATH}))
        decode-list (str/split decoder-str #"\s")]
    (apply sorted-set decode-list)))

(defn configure
  []
  (sh* ["./configure"
        "--toolchain=msvc"
        "--prefix=/opt/ffmpeg"
        "--disable-everything"
        "--enable-protocol=file"
        "--enable-bsfs"
        ] {:dir FFMPEG_SRC_PATH}))

(defn build
  []
  (let [default-decoders       (list-default-decoders)
        essential-decoders     (list-essential-decoders)
        non-essential-decoders (clojure.set/difference default-decoders essential-decoders)]
    (println (count default-decoders) (count essential-decoders))
    (doseq [x non-essential-decoders]
      (println x)))
  )

(defn run
  []
  (build)
  #_(extract-decoders))
