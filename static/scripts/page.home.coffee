define ['jquery'], ($) ->

  reset = () ->
    $('.error').text('')

  onNewIdentity: (handler) ->
    $('a#new-identity').on 'click', () ->
      reset()
      handler($('input#id-seed').val())

  render: (viewMsg) ->
    if viewMsg.error
      $('.error').text(viewMsg.error)
