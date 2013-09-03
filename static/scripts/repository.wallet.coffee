define ['jquery'], ($) ->

  create: (opts) ->
    $.post('/identity').done (_, status, xhr) ->
      if xhr.status == 201
        opts.ifSucceeded(xhr.getResponseHeader('Location'))
      else
        opts.elseFailed()

  get: (walletUri, opts) ->
    $.get(walletUri)
      .done((wallet) -> opts.ifSucceeded(wallet))
      .fail(() -> opts.elseFailed())