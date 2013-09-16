define ['jquery'], ($) ->

  render: (walletData) ->
    $('.wallet').html(ich.walletTmpl(walletData))
    $('.bag .entries').html(ich.bagTmpl(walletData))
