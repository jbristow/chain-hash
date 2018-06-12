(ns chain-hash.file
  (:import (java.nio ByteBuffer)
           (java.nio.channels SeekableByteChannel)
           (java.nio.file Files LinkOption OpenOption Paths StandardOpenOption))
  (:gen-class))

(defn path
  "Hides the ugliness of the java interop for Paths/get a little."
  [filename]
  (Paths/get filename (into-array [""])))

(defn exists? [filename]
  (Files/exists (path filename) (into-array LinkOption [])))

(defn file-size
  "Returns the total size of a file in bytes."
  [filename]
  (if (exists? filename)
    (Files/size (path filename))
    (throw (ex-info "Cannot get the size of a nonexistent file" {:filename filename}))))

(defn byte-channel
  "Simple clojure wrapper for creating a readonly RandomAccessFile object from
   a filename."
  [filename]
  (if (and (exists? filename)
           (Files/isReadable (path filename)))
    (Files/newByteChannel (path filename) (into-array OpenOption [StandardOpenOption/READ]))
    (throw (ex-info "File not readable for random access." {:filename filename}))))

(defn read-byte-array
  "Read a byte array of a given size starting at a given position.
   Operates on an already open RandomAccessFile object."
  [^SeekableByteChannel channel position size]
  (let [bbuff (ByteBuffer/allocate size)]
    (.position channel position)
    (.read channel bbuff)
    (.array bbuff)))

(defn output-stream-appender
  "Get an OutputStream set for append/create."
  [filename]
  (Files/newOutputStream (path filename)
                         (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])))

(defn append-to [filename data]
  (with-open [wrtr (Files/newOutputStream (path filename) (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND]))]
    (.write wrtr data)))
