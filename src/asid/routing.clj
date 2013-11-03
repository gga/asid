(ns asid.routing
  (:use compojure.core
        ring.util.response
        [asid.error.thread :only [fail-> dofailure]]
        [asid.error.definition :only [validate!]])

  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.data.json :as json]
            [clojure.string :as cs])

  (:require [asid.content-negotiation :as acn]
            [asid.file-resource :as afr]
            [asid.nodes :as an]
            [asid.graph :as ag]
            [asid.identity :as aid]
            [asid.trust-pool :as tp]
            [asid.wallet :as w]
            [asid.calling-card :as cc]
            [asid.introduction :as i]
            [asid.connection-request :as cr]
            [asid.wallet.links :as awl]
            [asid.wallet.repository :as wr]
            [asid.response :as ar]
            [asid.trust-pool-repository :as tpr]
            [asid.calling-card-repository :as ccr]
            [asid.connection-request-repository :as crr]))

(defn uri-map [repo]
  (handler/site (routes
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
                                    name (:name data)
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

                  (GET "/:walletid/card/:cardid" [walletid cardid]
                       (fail-> (wr/get-wallet walletid repo)
                               (ccr/card-from-wallet cardid)
                               (ar/resource)))

                  (POST ["/:id/letterplate", :id aid/grammar] [id :as {conn-req :json-doc}]
                        (dofailure [wallet (wr/get-wallet id repo)] 
                                   (fail-> (cr/new-connection-request conn-req)
                                           crr/save
                                           (cr/attach wallet)
                                           ar/created)))

                  (GET "/:walletid/request/:connreqid" [walletid connreqid]
                       (fail-> (wr/get-wallet walletid repo)
                               (crr/conn-req-from-wallet connreqid)
                               (ar/resource)))

                  (route/not-found (afr/file-resource "not-found.html")))))
