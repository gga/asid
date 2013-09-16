define ['underscore',
        'repository.wallet',
        'page.myWallet',
        'store',
        'navigator'], (_, walletRepo, myWalletPage, store, navigator) ->

  presentWallet = (wallet) ->
    identity: wallet.identity
    publicKey: wallet.key.public
    bag: _.map(_.keys(wallet.bag), (k) -> key: k, value: wallet.bag[k])

  myWalletPage.onAddBagItem (key, value) ->
    console.log(key)
    console.log(value)

  launch: (walletUri) ->
    store.set('currentWalletUri', walletUri)
    navigator.changePage(walletUri)

  start: () ->
    walletRepo.get store.get('currentWalletUri'),
      ifSucceeded: (data) ->
        myWalletPage.render(presentWallet(data))
      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
