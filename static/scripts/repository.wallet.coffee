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
    $.get(walletUri)
      .done((wallet) -> opts.ifSucceeded(wallet))
      .fail(() -> opts.elseFailed())
