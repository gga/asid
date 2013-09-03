define () ->

  set: (key, value) ->
    window.localStorage.setItem(key, value)
  get: (key) ->
    window.localStorage.getItem(key)
