define ['jquery'], ($) ->

  render: (walletData) ->
    console.log(walletData)
    $('.wallet').html(ich.walletTmpl(walletData))
