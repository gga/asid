define ['page.home',
        'repository.wallet',
        'controller.wallet'], (homePage, walletRepo, walletController) ->
  start: () ->
    homePage.onNewIdentity ->
      walletRepo.create
        ifSucceeded: (uri) ->
          console.log(uri)
          walletController.launch(uri)

        elseFailed: ->
