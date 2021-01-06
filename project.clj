(defproject react-cljs "0.1.0"
  :dependencies [[org.clojure/clojure "1.10.2-alpha2"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/core.match "1.0.0"]
                 [appliedscience/js-interop "0.2.5"]
                 [binaryage/devtools "1.0.2"]
                 [binaryage/oops "0.7.0"]
                 [com.wsscode/edn-json "1.1.0"]
                 [datascript "1.0.3"]
                 [district0x/graphql-query "1.0.6"]
                 [funcool/cuerdas "2020.03.26-3"]
                 [funcool/httpurr "2.0.0"]
                 [funcool/okulary "2020.04.14-0"]
                 [funcool/promesa "6.0.0"]
                 [funcool/rumext "2020.11.27-0"]
                 [juji/editscript "0.5.4"]
                 [medley "1.3.0"]
                 [net.sekao/odoyle-rules "0.6.0"]]

  :source-paths ["src"]
  :resource-paths ["dev-resources" "resources"]
  :target-path "target/%s")
