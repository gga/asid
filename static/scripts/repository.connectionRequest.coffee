define ['jquery'], ($) ->

  get: (crUri, opts) ->
    $.ajax crUri,
      type: 'GET'
      headers:
        Accept: 'application/vnd.org.asidentity.connection-request+json'
      success: (connReq) ->
        opts.ifSucceeded(connReq) if opts.ifSucceeded?
      error: () ->
        opts.elseFailed() if opts.elseFailed?