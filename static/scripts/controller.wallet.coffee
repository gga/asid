define ['repository.wallet',
        'store',
        'navigator'], (walletRepo, store, navigator) ->

  launch: (walletUri) ->
    store.set('currentWalletUri', walletUri)
    navigator.changePage('/wallet/')

  start: () ->
    walletRepo.get store.get('currentWalletUri'),
      ifSucceeded: (data) ->
        console.log(data)
      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")
