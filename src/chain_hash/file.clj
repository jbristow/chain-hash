(ns chain-hash.file
  (:import (java.io File RandomAccessFile)
           (java.nio.file Files Paths StandardOpenOption)))

(defn path
  "Hides the ugliness of the java interop for Paths/get a little."
  [filename]
  (Paths/get filename (into-array [""])))

(defn file-size
  "Returns the total size of a file in bytes."
  [filename]
  (Files/size (path filename)))

(defn random-access-file
  "Simple clojure wrapper for creating a readonly RandomAccessFile object from
   a filename."
  [filename]
  (if (Files/isReadable (path filename))
    (RandomAccessFile. (File. filename) "r")
    (throw (ex-info "File not readable for random access." {:filename filename}))))

(defn read-byte-array
  "Read a byte array of a given size starting at a given position.
   Operates on an already open RandomAccessFile object."
  [^RandomAccessFile raf position size]
  (let [bs (byte-array size)]
    (.seek raf position)
    (.read raf bs)
    bs))

(defn append-to [filename data]
  (if (Files/exists (path filename) (make-array []))
    (throw (ex-info "Output file already exists." {:filename filename}))
    (with-open [wrtr (Files/newOutputStream filename StandardOpenOption/APPEND)]
      (.write wrtr data))))
