define ['controller.home', 'controller.wallet'], (home, wallet) ->
  controllerFor: (path) ->
    return home if path == '/'
    return wallet if path.match(/([0-9a-f]{4,4}-)+[0-9a-f]{1,4}/)
