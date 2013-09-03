define ['jquery'], ($) ->

  create: (idSeed, opts) ->
    $.ajax '/identity',
      type: 'POST'
      data: idSeed
      contentType: 'text/plain'
      success: (_, status, xhr) ->
        if xhr.status == 201
          opts.ifSucceeded(xhr.getResponseHeader('Location'))
        else
          opts.elseFailed()
      error: -> opts.elseFailed()

  get: (walletUri, opts) ->
    $.ajax walletUri,
      type: 'GET'
      headers:
        Accept: 'application/vnd.org.asidentity.wallet+json'
      success: (wallet) -> opts.ifSucceeded(wallet)
      error: -> opts.elseFailed()
