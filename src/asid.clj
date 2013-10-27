(ns asid
  (:use compojure.core
        ring.middleware.file-info
        ring.util.response
        [asid.error.thread :only [fail-> dofailure]]
        [asid.error.definition :only [validate!]])

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clojure.string :as cs])

  (:require [asid.log :as log] 
            [asid.graph :as ag]
            [asid.nodes :as an]
            [asid.identity :as aid]
            [asid.trust-pool :as tp]
            [asid.wallet :as w]
            [asid.calling-card :as cc]
            [asid.introduction :as i]
            [asid.connection-request :as conn]
            [asid.wallet.links :as awl]
            [asid.wallet.repository :as wr]
            [asid.content-negotiation :as acn]
            [asid.json-doc-exchange :as jde]
            [asid.static :as as]
            [asid.current-request :as req]
            [asid.file-resource :as afr]
            [asid.response :as ar]
            [asid.trust-pool-repository :as tpr]
            [asid.calling-card-repository :as ccr]
            [asid.connection-request-repository :as cr]))

(def repo (an/initialize!))

(defroutes main-routes
  (POST "/identity" [_ :as {body :body}]
        (let [id-seed (slurp body)]
          (fail-> {"id seed" id-seed}
                  (validate! :not-empty "id seed")
                  (get "id seed")
                  w/new-wallet
                  (wr/save repo)
                  ar/created)))

  (GET ["/:id", :id aid/grammar] [id :as {accepted :accepts}]
       (acn/by-content accepted
                       "text/html"
                       (afr/file-resource "wallet/index.html")

                       "application/vnd.org.asidentity.wallet+json"
                       (fail-> (wr/get-wallet id repo)
                               ar/resource)

                       "application/vnd.org.asidentity.introduction+json"
                       (fail-> (wr/get-wallet id repo)
                               (i/intro-to-wallet)
                               ar/resource)))

  (POST ["/:id/bag", :id aid/grammar] [id key value]
        (fail-> {"id" id, "key" key, "value" value}
                (validate! :not-empty "key")
                (validate! :not-empty "value")
                (get "id")
                (wr/get-wallet repo)
                (w/add-data key value)
                (wr/save repo)
                ar/resource))

  (POST ["/:id/trustpool", :id aid/grammar] [id :as {pool-doc :json-doc}]
        (dofailure [data (validate! pool-doc :not-empty :name)
                    name (name :data)
                    challenge-keys (:challenge data)
                    pool (tpr/save (tp/new-trust-pool name challenge-keys) repo)
                    wallet (wr/get-wallet id repo)
                    pool (ag/trustpool pool wallet)]
                   (ar/created pool)))

  (GET "/:walletid/trustpool/:poolid" [walletid poolid]
       (fail-> (wr/get-wallet walletid repo)
               (tpr/pool-from-wallet poolid)
               ar/resource))

  (POST "/:walletid/trustpool/:poolid" [walletid poolid :as {calling-card :json-doc}]
        (dofailure [wallet (wr/get-wallet walletid repo)
                    pool (tpr/pool-from-wallet wallet poolid)]
                   (fail-> (cc/new-calling-card (:uri calling-card)
                                                (:identity calling-card))
                           (cc/submit wallet pool)
                           ccr/save
                           (cc/attach pool)
                           ar/created)))

  (POST ["/:id/letterplate", :id aid/grammar] [id :as {conn-req :json-doc}]
        (dofailure [wallet (wr/get-wallet id repo)] 
                   (fail-> (conn/new-connection-request conn-req)
                           cr/save
                           (conn/attach wallet)
                           ar/created)))

  (route/not-found (afr/file-resource "not-found.html")))

(defn create-app []
  (-> (handler/site main-routes)
      log/inbound-request
      req/capture-request
      jde/json-documents
      acn/accepts
      acn/vary-by-accept
      afr/resources
      wrap-file-info
      as/static-dir-index
      log/outbound-response))

