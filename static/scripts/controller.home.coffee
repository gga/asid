define ['page.home'], (homePage) ->
  start: () ->
    homePage.onNewIdentity ->
      alert('user clicked new identity')