define ['underscore',
        'repository.wallet',
        'repository.trustPool',
        'repository.callingCard',
        'repository.connectionRequest',
        'page.myWallet',
        'navigator'], (_, walletRepo, trustPoolRepo, callingCardRepo, connReqRepo, myWalletPage, navigator) ->
  currentWallet = null

  presentWallet = (wallet) ->
    wallet:
      identity: wallet.identity
      publicKey: wallet.key.public
      bag: _.map(_.keys(wallet.bag), (k) -> key: k, value: wallet.bag[k])

  presentCard = (card, trustPool) ->
    otherParty: card.otherParty
    otherPartyUrl: card.links.otherParty
    trustPoolName: trustPool.name

  displayCurrentWalletData = () ->
    myWalletPage.render(presentWallet(currentWallet))

  displayTrustPools = () ->
    _.each currentWallet.links.trustpools, (tpUri) ->
      trustPoolRepo.get tpUri,
        ifSucceeded: (pool) ->
          myWalletPage.render(addTrustPool: pool)

  displayCallingCard = (ccUri) ->
    callingCardRepo.get ccUri,
      ifSucceeded: (card) ->
        trustPoolRepo.get card.links.trustpool,
          ifSucceeded: (trustPool) ->
            myWalletPage.render(addCard: presentCard(card, trustPool))

  displayCallingCards = () ->
    _.each(currentWallet.links.cards, displayCallingCard)

  displayConnectionRequest = (connReqUri) ->
    connReqRepo.get connReqUri,
      ifSucceeded: (connReq) ->
        myWalletPage.render(addConnReq: connReq)

  displayConnectionRequests = () ->
    _.each(currentWallet.links.connectionRequests, displayConnectionRequest)

  displayCurrentWallet = () ->
    displayCurrentWalletData()
    displayTrustPools()
    displayCallingCards()
    displayConnectionRequests()

  switchToTab = (tab) ->
    myWalletPage.render
      hideTabs: _.reject(['pools', 'cards', 'requests'], (v) -> v == tab)
      showTab: tab

  myWalletPage.onAddBagItem (key, value) ->
    walletRepo.addBagItem currentWallet.links.bag, key, value,
      ifSucceeded: (updatedWallet) ->
        currentWallet = updatedWallet
        displayCurrentWalletData()
        myWalletPage.render(reset: true)

  myWalletPage.onShowPools () -> switchToTab('pools')
  myWalletPage.onShowCards () -> switchToTab('cards')
  myWalletPage.onShowRequests () -> switchToTab('requests')

  myWalletPage.onAddChallenge(() -> myWalletPage.render(addChallengeLine: true))

  myWalletPage.onAddTrustPool (poolName, challenge) ->
    trustPoolRepo.create currentWallet, poolName, challenge,
      ifSucceeded: (poolUri, pool) ->
        myWalletPage.render(addTrustPool: pool)
        myWalletPage.render(reset: true)
      elseFailed: () ->
        console.log("Error! Couldn't create trust pool.")

  myWalletPage.onSign (poolUri) ->
    myWalletPage.render(displayPoolConnDetails: poolUri)

  myWalletPage.onConnectCancel (poolUri) ->
    myWalletPage.render(removePoolConnDetails: poolUri)

  myWalletPage.onConnectConfirm (poolUri, identity, identityUri) ->
    trustPoolRepo.connect poolUri, identity, identityUri,
      ifSucceeded: (cardUri, card) ->
        displayCallingCard(cardUri)
      elseFailed: ->
        console.log("Error! Couldn't leave calling card with other party.")

  launch: (walletUri) ->
    navigator.changePage(walletUri)

  start: () ->
    myWalletPage.initialize()
    walletRepo.get navigator.currentPage(),
      ifSucceeded: (data) ->
        currentWallet = data
        displayCurrentWallet()

      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
