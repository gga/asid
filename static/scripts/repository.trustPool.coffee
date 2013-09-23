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