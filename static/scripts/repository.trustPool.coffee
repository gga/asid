define ['jquery'], ($) ->

  create: (wallet, name, challenge, opts) ->
    $.ajax wallet.links.trustpool,
      type: 'POST'
      data: JSON.stringify
        name: name
        challenge: challenge
      contentType: 'application/vnd.org.asidentity.trust-pool+json'
      success: (pool, status, xhr) ->
        if xhr.status == 201
          opts.ifSucceeded(xhr.getResponseHeader('Location'), pool)
        else
          opts.elseFailed()
      error: -> opts.elseFailed()

  get: (tpUri, opts) ->
    $.ajax tpUri,
      type: 'GET'
      headers:
        Accept: 'application/vnd.org.asidentity.trust-pool+json'
      success: (pool) ->
        opts.ifSucceeded(pool) if opts.ifSucceeded?
      error: () ->
        opts.elseFailed() if opts.elseFailed?

  connect: (tpUri, otherIdentity, otherUri, opts) ->
    $.ajax tpUri,
      type: 'POST'
      data: JSON.stringify
        identity: otherIdentity
        uri: otherUri
      contentType: 'application/vnd.org.asidentity.calling-card+json'
      success: (card, status, xhr) ->
        if xhr.status == 201
          opts.ifSucceeded(xhr.getResponseHeader('Location'), card)
        else
          opts.elseFailed()
      error: -> opts.elseFailed()