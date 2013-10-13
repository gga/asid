define ['page.home',
        'repository.wallet',
        'controller.wallet'], (homePage, walletRepo, walletController) ->
  start: () ->
    homePage.onNewIdentity (idSeed) ->
      if idSeed.length > 0
        walletRepo.create idSeed,
          ifSucceeded: (uri) ->
            walletController.launch(uri)

          elseFailed: ->
      else
        homePage.render(error: "I'm going to need something from you to seed a unique identity.")
