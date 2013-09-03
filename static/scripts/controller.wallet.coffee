define ['repository.wallet'], (walletRepo) ->

  start: (walletUri) ->
    walletRepo.get walletUri,
      ifSucceeded: (data) ->
        console.log(data)
      elseFailed: () ->
        console.log("Error! Couldn't get wallet.")