define ['jquery'], ($) ->

  onAddBagItem: (handler) ->
    $('form#addBagItem').on 'submit', (e) ->
      handler($('input#newKey').val(), $('input#newValue').val())
      e.preventDefault()

  render: (walletData) ->
    $('.wallet').html(ich.walletTmpl(walletData))
    $('.bag .entries').html(ich.bagTmpl(walletData))
