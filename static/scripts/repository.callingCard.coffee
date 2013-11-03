define ['jquery'], ($) ->

  get: (ccUri, opts) ->
    $.ajax ccUri,
      type: 'GET'
      headers:
        Accept: 'application/vnd.org.asidentity.calling-card+json'
      success: (card) ->
        opts.ifSucceeded(card) if opts.ifSucceeded?
      error: () ->
        opts.elseFailed() if opts.elseFailed?