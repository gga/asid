require.config
  baseUrl: '/scripts',
  paths:
    jquery: 'jquery-2.0.3.min'
  shim:
    underscore:
      exports: '_'

require ['asid'], (asid) ->
  path = window.location.pathname
  controller = asid.controllerFor(path)
  controller.start() if controller
