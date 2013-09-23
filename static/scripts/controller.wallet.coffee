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

  displayCurrentWallet = () ->
    myWalletPage.render(presentWallet(currentWallet))

  myWalletPage.onAddBagItem (key, value) ->
    walletRepo.addBagItem currentWallet.links.bag, key, value,
      ifSucceeded: (updatedWallet) ->
        currentWallet = updatedWallet
        displayCurrentWallet()
        myWalletPage.render(reset: true)

  myWalletPage.onAddChallenge(() -> myWalletPage.render(addChallengeLine: true))

  myWalletPage.onAddTrustPool (poolName, challenge) ->
    trustPoolRepo.create currentWallet, poolName, challenge,
      ifSucceeded: (poolUri, pool) ->
        myWalletPage.render(trustPools: [pool])
        myWalletPage.render(reset: true)
      elseFailed: () ->
        console.log("Error! Couldn't create trust pool.")

  launch: (walletUri) ->
    store.set('currentWalletUri', walletUri)
    navigator.changePage(walletUri)

  start: () ->
    walletRepo.get store.get('currentWalletUri'),
      ifSucceeded: (data) ->
        currentWallet = data
        displayCurrentWallet()

      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
