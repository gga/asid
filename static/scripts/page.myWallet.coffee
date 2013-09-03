define ['jquery'], ($) ->

  render: (walletData) ->
    $('.wallet').html(ich.walletTmpl(walletData))
