define ['underscore',
        'repository.wallet',
        'repository.trustPool',
        'page.myWallet',
        'store',
        'navigator'], (_, walletRepo, trustPoolRepo, myWalletPage, store, navigator) ->
  currentWallet = null

  presentWallet = (wallet) ->
    wallet:
      identity: wallet.identity
      publicKey: wallet.key.public
      bag: _.map(_.keys(wallet.bag), (k) -> key: k, value: wallet.bag[k])

  displayCurrentWalletData = () ->
    myWalletPage.render(presentWallet(currentWallet))

  displayTrustPools = () ->
    _.each currentWallet.links.trustpools, (tpUri) ->
      trustPoolRepo.get tpUri,
        ifSucceeded: (pool) ->
          myWalletPage.render(addTrustPool: pool)

  displayCurrentWallet = () ->
    displayCurrentWalletData()
    displayTrustPools()

  myWalletPage.onAddBagItem (key, value) ->
    walletRepo.addBagItem currentWallet.links.bag, key, value,
      ifSucceeded: (updatedWallet) ->
        currentWallet = updatedWallet
        displayCurrentWalletData()
        myWalletPage.render(reset: true)

  myWalletPage.onAddChallenge(() -> myWalletPage.render(addChallengeLine: true))

  myWalletPage.onAddTrustPool (poolName, challenge) ->
    trustPoolRepo.create currentWallet, poolName, challenge,
      ifSucceeded: (poolUri, pool) ->
        myWalletPage.render(addTrustPool: pool)
        myWalletPage.render(reset: true)
      elseFailed: () ->
        console.log("Error! Couldn't create trust pool.")

  myWalletPage.onSign (poolUri) ->

  launch: (walletUri) ->
    store.set('currentWalletUri', walletUri)
    navigator.changePage(walletUri)

  start: () ->
    myWalletPage.initialize()
    walletRepo.get store.get('currentWalletUri'),
      ifSucceeded: (data) ->
        currentWallet = data
        displayCurrentWallet()

      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
