define ['underscore',
        'repository.wallet',
        'page.myWallet',
        'store',
        'navigator'], (_, walletRepo, myWalletPage, store, navigator) ->
  currentWallet = null

  presentWallet = (wallet) ->
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
