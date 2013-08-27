require.config
  baseUrl: 'scripts',
  paths:
    jquery: 'jquery-2.0.3.min'

require ['jquery', 'asid'], ($, asid) ->
  path = window.location.pathname
  asid[path].start() if asid[path]
