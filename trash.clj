

(defn ->csv [chunk]
  (with-out-str
    (doseq [[a b c] chunk]
      (println a \, b \, c))))

(defn ->input-stream ^InputStream [^String text]
  (-> text
      (.getBytes "UTF-8")
      clojure.java.io/input-stream))

(-> chunk
    ->csv
    ->input-stream)



{:tag :ParameterStatus, :param "application_name", :value ""}
{:tag :ParameterStatus, :param "client_encoding", :value "UTF8"}
{:tag :ParameterStatus, :param "DateStyle", :value "ISO, MDY"}
{:tag :ParameterStatus, :param "default_transaction_read_only", :value "off"}
{:tag :ParameterStatus, :param "in_hot_standby", :value "off"}
{:tag :ParameterStatus, :param "integer_datetimes", :value "on"}
{:tag :ParameterStatus, :param "IntervalStyle", :value "postgres"}
{:tag :ParameterStatus, :param "is_superuser", :value "on"}
{:tag :ParameterStatus, :param "server_encoding", :value "UTF8"}
{:tag :ParameterStatus, :param "server_version", :value "14.6"}
{:tag :ParameterStatus, :param "session_authorization", :value "ivan"}
{:tag :ParameterStatus, :param "standard_conforming_strings", :value "on"}
{:tag :ParameterStatus, :param "TimeZone", :value "Europe/Moscow"}
