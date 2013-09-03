define ['repository.wallet',
        'page.myWallet',
        'store',
        'navigator'], (walletRepo, myWallet, store, navigator) ->

  launch: (walletUri) ->
    store.set('currentWalletUri', walletUri)
    navigator.changePage('/wallet/')

  start: () ->
    walletRepo.get store.get('currentWalletUri'),
      ifSucceeded: (data) ->
        myWallet.render(data)
      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
