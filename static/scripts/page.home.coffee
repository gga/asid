define ['jquery'], ($) ->

  onNewIdentity: (handler) ->
    $('a#new-identity').on('click', () -> handler())