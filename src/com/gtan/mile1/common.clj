(ns com.gtan.mile1.common
  (:require [clojure.java.io :as io])
  (:import (java.nio.file Paths Path Files LinkOption FileVisitor FileVisitResult)
           (java.io File)
           (java.nio.file.attribute FileAttribute PosixFilePermission)))

(defn ^Path build-path [^String root & subs]
  (Paths/get root (into-array String subs)))

(defn mkdir
  ([path] (Files/createDirectories path (into-array FileAttribute [])))
  ([path & attrs] (Files/createDirectories path (into-array FileAttribute attrs))))

(defn download-url-to [url ^Path target-path]
  (mkdir (.getParent target-path))
  (with-open [in (io/input-stream url)
              out (io/output-stream (.toFile target-path))]
    (io/copy in out)))

(defn is-dir? [path]
  (Files/isDirectory path (into-array LinkOption [])))

(defn exists? [path]
  (Files/exists path (into-array LinkOption [])))

(defn ^String path-simple-name [^Path path]
  (.. path getFileName toString))

(defn ln-replace [^Path dst ^Path src]
  (when (exists? dst) (Files/delete dst))
  (Files/createSymbolicLink dst src (into-array FileAttribute [])))

(defn set-executable [^Path path]
  (Files/setPosixFilePermissions path #{PosixFilePermission/OWNER_EXECUTE
                                        PosixFilePermission/OWNER_READ
                                        PosixFilePermission/OWNER_WRITE}))

(defn delete-tree [^Path path]
  (Files/walkFileTree path (reify FileVisitor
                             (visitFile [this file attr]
                               (do (Files/delete file)
                                   FileVisitResult/CONTINUE))
                             (preVisitDirectory [this dir attr]
                               FileVisitResult/CONTINUE)
                             (postVisitDirectory [this dir attr]
                               (do (Files/delete dir)
                                   FileVisitResult/CONTINUE))
                             (visitFileFailed [this file exc]
                               (throw exc))))
  )

