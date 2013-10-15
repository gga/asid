define () ->

  changePage: (path) ->
    window.location.href = window.location.origin + path

  currentPage: () ->
    window.location.href