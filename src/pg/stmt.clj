(ns pg.stmt)


(defprotocol IStatement

  (get-name [this])

  (param-count [this])

  (param-type [this idx])

  (field-count [this])

  (field-name [this idx])

  (field-type [this idx]))


;; TODO: implement toString
(deftype Statement

    [Statement
     ParameterDescription
     RowDescription

     encoding]

    IStatement

    (get-name [this]
      Statement)

    (param-count [this]
      (get ParameterDescription :param-count))

    (param-type [this idx]
      (get-in ParameterDescription [:param-types idx]))

    (field-count [this]
      (get RowDescription :field-count))

    (field-name [this idx]
      (get-in RowDescription [:fields idx :name]))

    (field-type [this idx]
      (get-in RowDescription [:fields idx :type-id])))


(defn make-statement
  [stmt-name ParameterDescription RowDescription]
  (new Statement stmt-name ParameterDescription RowDescription "utf-8"))
